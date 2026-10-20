package pe.marvisur.sgldqr.api.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Punto de entrada para solicitudes no autenticadas o con token JWT inválido/expirado/ausente.
 * Retorna una respuesta estándar RFC 7807 (ProblemDetail) con código HTTP 401.
 */
@Component
public class PuntoEntradaAutenticacionJwt implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public PuntoEntradaAutenticacionJwt(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Acceso no autorizado. El token de autenticación está ausente, ha expirado o es inválido."
        );
        problem.setTitle("No Autorizado");
        problem.setType(URI.create("https://marvisur.pe/errores/no-autorizado"));
        problem.setProperty("codigo", "NO_AUTORIZADO");
        problem.setInstance(URI.create(request.getRequestURI()));

        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
