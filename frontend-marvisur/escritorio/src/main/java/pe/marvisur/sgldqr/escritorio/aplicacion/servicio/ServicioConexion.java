package pe.marvisur.sgldqr.escritorio.aplicacion.servicio;

import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.EstadoConexionPort;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.ConfiguracionApi;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio centralizado de monitoreo y detección de conectividad con conmutación en &le; 2 segundos (RNF-02, O1).
 *
 * <p><strong>Reglas de Conmutación:</strong>
 * <ul>
 *   <li>Timeouts estrictos de 2 segundos en conexiones HTTP y lecturas.</li>
 *   <li>Transición inmediata a <em>SIN CONEXIÓN</em> ante el primer fallo de red, timeout o respuesta no saludable.</li>
 *   <li>Transición a <em>EN LÍNEA</em> cuando el endpoint {@code /actuator/health} responde 200 OK con estado UP.</li>
 *   <li>Medición y registro de la latencia de detección (sin datos personales ni sensibles).</li>
 * </ul>
 * </p>
 */
public class ServicioConexion implements EstadoConexionPort {

    private static final Logger LOGGER = Logger.getLogger(ServicioConexion.class.getName());
    public static final Duration TIMEOUT_HTTP_DEFECTO = Duration.ofSeconds(2);

    private static ServicioConexion instancia;

    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final AtomicBoolean enLinea = new AtomicBoolean(true);
    private final AtomicLong tiempoUltimaDeteccionMs = new AtomicLong(0);
    private final List<ListenerEstadoConexion> listeners = new CopyOnWriteArrayList<>();

    public interface ListenerEstadoConexion {
        void onCambioEstado(boolean enLinea);
    }

    public static synchronized ServicioConexion getInstancia() {
        if (instancia == null) {
            instancia = new ServicioConexion();
        }
        return instancia;
    }

    public static synchronized void reiniciarInstancia(ServicioConexion nuevaInstancia) {
        instancia = nuevaInstancia;
    }

    public ServicioConexion() {
        this(new ConfiguracionApi().getBaseUrl(), obtenerTimeoutConfigurado(), null);
    }

    public ServicioConexion(String baseUrl, Duration timeout, HttpClient httpClient) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout != null ? timeout : TIMEOUT_HTTP_DEFECTO;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(this.timeout)
                .build();
    }

    private static Duration obtenerTimeoutConfigurado() {
        try (InputStream is = ServicioConexion.class.getResourceAsStream("/escritorio.properties")) {
            if (is != null) {
                Properties prop = new Properties();
                prop.load(is);
                String segStr = prop.getProperty("http.timeout-segundos");
                if (segStr != null && !segStr.isBlank()) {
                    return Duration.ofSeconds(Long.parseLong(segStr.trim()));
                }
            }
        } catch (Exception ignored) {
        }
        return TIMEOUT_HTTP_DEFECTO;
    }

    public void agregarListener(ListenerEstadoConexion listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removerListener(ListenerEstadoConexion listener) {
        listeners.remove(listener);
    }

    public boolean estaEnLinea() {
        return enLinea.get();
    }

    public long getTiempoUltimaDeteccionMs() {
        return tiempoUltimaDeteccionMs.get();
    }

    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Reporta de forma inmediata una caída de red o timeout provocada por cualquier operación HTTP.
     */
    public void reportarFalloConexion(String motivo, long duracionMs) {
        tiempoUltimaDeteccionMs.set(duracionMs);
        boolean estabaEnLinea = enLinea.getAndSet(false);
        if (estabaEnLinea) {
            LOGGER.log(Level.WARNING, "Transición a SIN CONEXIÓN detectada en " + duracionMs + " ms. Motivo: " + motivo);
            notificarListeners(false);
        }
    }

    public void reportarFalloConexion(String motivo) {
        reportarFalloConexion(motivo, 0);
    }

    /**
     * Reporta el restablecimiento exitoso de la comunicación con el servidor central.
     */
    public void reportarExitoConexion() {
        boolean estabaOffline = !enLinea.getAndSet(true);
        if (estabaOffline) {
            LOGGER.info("Comunicación restablecida con el servidor central: EN LÍNEA.");
            notificarListeners(true);
        }
    }

    @Override
    public boolean verificarConectividad() {
        long inicio = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/actuator/health"))
                    .header("Accept", "application/json")
                    .timeout(timeout)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencia = System.currentTimeMillis() - inicio;
            tiempoUltimaDeteccionMs.set(latencia);

            if (response.statusCode() == 200 && response.body() != null && response.body().contains("UP")) {
                reportarExitoConexion();
                return true;
            } else {
                reportarFalloConexion("Respuesta de salud HTTP " + response.statusCode(), latencia);
                return false;
            }
        } catch (Exception e) {
            long latencia = System.currentTimeMillis() - inicio;
            reportarFalloConexion("Fallo en verificación /actuator/health: " + e.getClass().getSimpleName(), latencia);
            return false;
        }
    }

    private void notificarListeners(boolean online) {
        for (ListenerEstadoConexion listener : listeners) {
            try {
                listener.onCambioEstado(online);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al notificar listener de conectividad: " + e.getMessage());
            }
        }
    }
}
