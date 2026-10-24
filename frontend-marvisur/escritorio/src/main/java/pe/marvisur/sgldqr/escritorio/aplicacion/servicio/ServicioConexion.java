package pe.marvisur.sgldqr.escritorio.aplicacion.servicio;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EstadoConexionUI;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio centralizado de monitoreo y detección de conectividad con conmutación en &le; 2 segundos (RNF-02, O1).
 * Es la fuente única de verdad para el estado de conexión de la UI (EN_LINEA, SIN_CONEXION, SINCRONIZANDO).
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
    private final List<ListenerEstadoConexionUI> listenersUI = new CopyOnWriteArrayList<>();
    private volatile EstadoConexionUI estadoUI = EstadoConexionUI.EN_LINEA;
    private final AtomicInteger totalPendientes = new AtomicInteger(0);
    private volatile String mensajeNotificacion = "";

    public interface ListenerEstadoConexion {
        void onCambioEstado(boolean enLinea);
    }

    @FunctionalInterface
    public interface ListenerEstadoConexionUI {
        void onCambioEstadoUI(EstadoConexionUI estado, int pendientes, String notificacion);
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

    public void agregarListenerUI(ListenerEstadoConexionUI listener) {
        if (listener != null && !listenersUI.contains(listener)) {
            listenersUI.add(listener);
        }
    }

    public void removerListenerUI(ListenerEstadoConexionUI listener) {
        listenersUI.remove(listener);
    }

    public boolean estaEnLinea() {
        return enLinea.get();
    }

    public EstadoConexionUI getEstadoUI() {
        return estadoUI;
    }

    public int getPendientes() {
        return totalPendientes.get();
    }

    public String getMensajeNotificacion() {
        return mensajeNotificacion;
    }

    public long getTiempoUltimaDeteccionMs() {
        return tiempoUltimaDeteccionMs.get();
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void actualizarPendientes(int pendientes) {
        totalPendientes.set(Math.max(0, pendientes));
        notificarListenersUI();
    }

    public void reportarInicioSincronizacion(int pendientes) {
        this.totalPendientes.set(Math.max(0, pendientes));
        this.estadoUI = EstadoConexionUI.SINCRONIZANDO;
        notificarListenersUI();
    }

    public void reportarFinSincronizacion(int sincronizados, int pendientesRestantes) {
        this.totalPendientes.set(Math.max(0, pendientesRestantes));
        this.estadoUI = enLinea.get() ? EstadoConexionUI.EN_LINEA : EstadoConexionUI.SIN_CONEXION;
        if (sincronizados > 0) {
            this.mensajeNotificacion = "Sincronizado";
        }
        notificarListenersUI();
    }

    public void limpiarNotificacion() {
        this.mensajeNotificacion = "";
        notificarListenersUI();
    }

    /**
     * Reporta de forma inmediata una caída de red o timeout provocada por cualquier operación HTTP o sondeo.
     */
    public void reportarFalloConexion(String motivo, long duracionMs) {
        tiempoUltimaDeteccionMs.set(duracionMs);
        boolean estabaEnLinea = enLinea.getAndSet(false);
        if (estadoUI != EstadoConexionUI.SINCRONIZANDO) {
            estadoUI = EstadoConexionUI.SIN_CONEXION;
        }
        if (estabaEnLinea) {
            LOGGER.log(Level.WARNING, "Transición a SIN CONEXIÓN detectada en " + duracionMs + " ms. Motivo: " + motivo);
            notificarListeners(false);
        }
        notificarListenersUI();
    }

    public void reportarFalloConexion(String motivo) {
        reportarFalloConexion(motivo, 0);
    }

    /**
     * Reporta el restablecimiento exitoso de la comunicación con el servidor central.
     */
    public void reportarExitoConexion() {
        boolean estabaOffline = !enLinea.getAndSet(true);
        if (estadoUI != EstadoConexionUI.SINCRONIZANDO) {
            estadoUI = EstadoConexionUI.EN_LINEA;
        }
        if (estabaOffline) {
            LOGGER.info("Comunicación restablecida con el servidor central: EN LÍNEA.");
            notificarListeners(true);
        }
        notificarListenersUI();
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

    private void notificarListenersUI() {
        EstadoConexionUI est = this.estadoUI;
        int pend = this.totalPendientes.get();
        String notif = this.mensajeNotificacion;
        for (ListenerEstadoConexionUI listener : listenersUI) {
            try {
                listener.onCambioEstadoUI(est, pend, notif);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al notificar listener de estado UI: " + e.getMessage());
            }
        }
    }
}
