package pe.marvisur.sgldqr.api.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de protección contra abuso y denegación de servicio (RF-08) para los endpoints públicos
 * (/api/v1/publico/**).
 *
 * <p>Aplica limitación de tasa basada en ventana deslizante en memoria por dirección IP.
 * Si se supera la cuota configurada ({@code app.publico.limite-por-minuto}), responde HTTP 429
 * Too Many Requests con cabecera {@code Retry-After} y cuerpo estándar RFC 7807 (ProblemDetail).
 * Confía en {@code X-Forwarded-For} únicamente si {@code app.publico.confiar-proxy=true}.</p>
 */
@Component
public class FiltroRateLimitingPublico extends OncePerRequestFilter {

    private static final String PREFIJO_PUBLICO = "/api/v1/publico";
    private static final long VENTANA_MS = 60_000L; // 1 minuto

    private final int limitePorMinuto;
    private final boolean confiarProxy;
    private final ObjectMapper objectMapper;

    private final Map<String, Deque<Long>> peticionesPorIp = new ConcurrentHashMap<>();

    public FiltroRateLimitingPublico(@Value("${app.publico.limite-por-minuto:30}") int limitePorMinuto,
                                     @Value("${app.publico.confiar-proxy:false}") boolean confiarProxy,
                                     ObjectMapper objectMapper) {
        this.limitePorMinuto = limitePorMinuto;
        this.confiarProxy = confiarProxy;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith(PREFIJO_PUBLICO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = resolverIp(request);
        long ahora = System.currentTimeMillis();

        Deque<Long> marcas = peticionesPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());

        synchronized (marcas) {
            // Limpiar timestamps fuera de la ventana deslizante
            while (!marcas.isEmpty() && (ahora - marcas.peekFirst()) > VENTANA_MS) {
                marcas.pollFirst();
            }

            if (marcas.size() >= limitePorMinuto) {
                long masAntiguo = marcas.isEmpty() ? ahora : marcas.peekFirst();
                long tiempoEsperaSegundos = Math.max(1, (masAntiguo + VENTANA_MS - ahora) / 1000);

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(tiempoEsperaSegundos));
                response.setContentType("application/problem+json;charset=UTF-8");

                ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Se ha superado el límite de " + limitePorMinuto + " peticiones por minuto para su dirección IP."
                );
                problemDetail.setTitle("Límite de peticiones excedido");
                problemDetail.setType(URI.create("about:blank"));
                problemDetail.setInstance(URI.create(request.getRequestURI()));
                problemDetail.setProperty("timestamp", Instant.now().toString());
                problemDetail.setProperty("retryAfterSegundos", tiempoEsperaSegundos);

                objectMapper.writeValue(response.getOutputStream(), problemDetail);
                return;
            }

            marcas.addLast(ahora);
        }

        filterChain.doFilter(request, response);
    }

    private String resolverIp(HttpServletRequest request) {
        if (confiarProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Limpia contadores en memoria (útil para pruebas unitarias y de integración).
     */
    public void reiniciar() {
        peticionesPorIp.clear();
    }
}
