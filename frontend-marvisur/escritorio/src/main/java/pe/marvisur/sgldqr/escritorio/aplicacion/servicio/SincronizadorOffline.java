package pe.marvisur.sgldqr.escritorio.aplicacion.servicio;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ElementoColaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ItemResultadoSyncDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.MotivoErrorSync;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RespuestaSyncLoteDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ResultadoSync;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ApiExcepcion;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.ColaLocalPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.SyncPort;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;
import pe.marvisur.sgldqr.escritorio.infraestructura.sqlite.SQLiteColaAdapter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio en segundo plano para la sincronización automática de la cola local SQLite con el servidor central (B4, O2).
 *
 * <p><strong>Comportamiento:</strong>
 * <ul>
 *   <li>Ejecución periódica cada 10 segundos o disparada ante reconexión del servidor.</li>
 *   <li>Envío en lotes de hasta 50 registros por petición a {@code POST /api/v1/sync/encomiendas}.</li>
 *   <li>Garantiza una única sincronización activa concurrente mediante {@link AtomicBoolean}.</li>
 *   <li>Tratamiento idempotente (reconocimiento de registros ya sincronizados como éxito local).</li>
 *   <li>Reintento con backoff exponencial hasta 5 intentos (errores de validación se rechazan sin reintento).</li>
 *   <li>Preservación íntegra de la cola local ante expiración de sesión (HTTP 401).</li>
 * </ul>
 * </p>
 */
public class SincronizadorOffline {

    private static final Logger LOGGER = Logger.getLogger(SincronizadorOffline.class.getName());
    public static final int TAMANO_LOTE_MAXIMO = 50;
    public static final int INTERVALO_SEGUNDOS = 10;

    private static SincronizadorOffline instancia;

    private final ColaLocalPort colaLocalPort;
    private final SyncPort syncPort;
    private final ServicioConexion servicioConexion;
    private final List<ListenerSincronizacion> listeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService planificador;
    private final AtomicBoolean sincronizandoEnCurso = new AtomicBoolean(false);

    public interface ListenerSincronizacion {
        void onSincronizacionCompletada(int totalSincronizados, int pendientesRestantes);
        void onColaActualizada(int pendientesRestantes);
    }

    public static synchronized SincronizadorOffline getInstancia() {
        if (instancia == null) {
            instancia = new SincronizadorOffline();
        }
        return instancia;
    }

    public static synchronized void reiniciarInstancia(SincronizadorOffline nuevaInstancia) {
        if (instancia != null) {
            instancia.detener();
        }
        instancia = nuevaInstancia;
    }

    public SincronizadorOffline() {
        this(new SQLiteColaAdapter(), new MarvisurHttpClient(), ServicioConexion.getInstancia());
    }

    public SincronizadorOffline(ColaLocalPort colaLocalPort, SyncPort syncPort, ServicioConexion servicioConexion) {
        this.colaLocalPort = Objects.requireNonNull(colaLocalPort, "colaLocalPort no puede ser nulo");
        this.syncPort = Objects.requireNonNull(syncPort, "syncPort no puede ser nulo");
        this.servicioConexion = Objects.requireNonNull(servicioConexion, "servicioConexion no puede ser nulo");
    }

    public void agregarListener(ListenerSincronizacion listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removerListener(ListenerSincronizacion listener) {
        listeners.remove(listener);
    }

    public synchronized void iniciar() {
        if (planificador != null && !planificador.isShutdown()) {
            return;
        }

        planificador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hilo-sincronizador-offline");
            t.setDaemon(true);
            return t;
        });

        // Tarea periódica cada 10 segundos
        planificador.scheduleWithFixedDelay(this::ejecutarSincronizacionSegura, 3, INTERVALO_SEGUNDOS, TimeUnit.SECONDS);

        // Reaccionar inmediatamente cuando la conectividad se restablece si hay registros pendientes
        servicioConexion.agregarListener(online -> {
            if (online && planificador != null && !planificador.isShutdown()) {
                if (colaLocalPort.contarPendientes() > 0) {
                    LOGGER.info("Conectividad restablecida con pendientes en cola. Disparando sincronización...");
                    planificador.submit(this::ejecutarSincronizacionSegura);
                }
            }
        });

        LOGGER.info("Sincronizador offline iniciado (intervalo: " + INTERVALO_SEGUNDOS + " s, lote máx: " + TAMANO_LOTE_MAXIMO + ").");
    }

    public synchronized void detener() {
        if (planificador != null && !planificador.isShutdown()) {
            planificador.shutdownNow();
            planificador = null;
            LOGGER.info("Sincronizador offline detenido.");
        }
    }

    public void sincronizarAhora() {
        if (planificador != null && !planificador.isShutdown()) {
            planificador.submit(this::ejecutarSincronizacionSegura);
        } else {
            ejecutarSincronizacionSegura();
        }
    }

    private void ejecutarSincronizacionSegura() {
        try {
            sincronizarLote(false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error durante la ejecución del sincronizador offline: " + e.getMessage());
        }
    }

    /**
     * Ejecuta una ronda de sincronización enviando hasta 50 registros pendientes.
     * Garantiza que solo exista una sincronización activa a la vez.
     *
     * @param forzarReintento Si es true, ignora el cálculo de backoff exponencial temporal.
     * @return total de registros sincronizados con éxito en esta ejecución (nuevos + ya sincronizados).
     */
    public int sincronizarLote(boolean forzarReintento) {
        if (!sincronizandoEnCurso.compareAndSet(false, true)) {
            LOGGER.fine("Sincronización omitida: ya existe un proceso de sincronización en curso.");
            return 0;
        }

        try {
            if (!servicioConexion.estaEnLinea()) {
                return 0;
            }

            if (!SesionContexto.estaAutenticado()) {
                return 0;
            }

            int totalPendientes = colaLocalPort.contarPendientes();
            if (totalPendientes <= 0) {
                servicioConexion.actualizarPendientes(0);
                notificarColaActualizada(0);
                return 0;
            }

            List<ElementoColaDto> pendientes = colaLocalPort.listarPendientes(TAMANO_LOTE_MAXIMO);
            if (pendientes.isEmpty()) {
                servicioConexion.actualizarPendientes(0);
                notificarColaActualizada(0);
                return 0;
            }

            LocalDateTime ahora = LocalDateTime.now();
            List<ElementoColaDto> loteAEnviar = new ArrayList<>();
            for (ElementoColaDto item : pendientes) {
                if (forzarReintento || !debeEsperarBackoff(item, ahora)) {
                    loteAEnviar.add(item);
                }
            }

            if (loteAEnviar.isEmpty()) {
                servicioConexion.actualizarPendientes(totalPendientes);
                return 0;
            }

            LOGGER.info("Iniciando sincronización offline con " + totalPendientes + " registros pendientes...");
            servicioConexion.reportarInicioSincronizacion(totalPendientes);

            LOGGER.info("Enviando lote de " + loteAEnviar.size() + " registros a sincronizar...");
            long inicioLote = System.currentTimeMillis();

            // Construir el JSON del lote en formato compatible: { "items": [ {...}, ... ] }
            StringBuilder jsonBuilder = new StringBuilder("{\"items\":[");
            for (int i = 0; i < loteAEnviar.size(); i++) {
                if (i > 0) jsonBuilder.append(",");
                jsonBuilder.append(loteAEnviar.get(i).payload());
            }
            jsonBuilder.append("]}");

            RespuestaSyncLoteDto respuesta;
            try {
                respuesta = syncPort.sincronizarLote(jsonBuilder.toString());
            } catch (ApiExcepcion ae) {
                long duracionMs = System.currentTimeMillis() - inicioLote;
                if (ae.getStatusCode() == 401) {
                    LOGGER.warning("Sesión expirada durante la sincronización: la cola local se preserva íntegra.");
                    SesionContexto.notificarSesionExpirada("Sesión expirada durante sincronización offline");
                    servicioConexion.reportarFinSincronizacion(0, colaLocalPort.contarPendientes());
                    return 0;
                }
                LOGGER.log(Level.WARNING, "Error HTTP (" + ae.getStatusCode() + ") al sincronizar lote en " + duracionMs + " ms: " + ae.getMessage());
                servicioConexion.reportarFinSincronizacion(0, colaLocalPort.contarPendientes());
                return 0;
            } catch (Exception e) {
                long duracionMs = System.currentTimeMillis() - inicioLote;
                LOGGER.log(Level.WARNING, "Fallo de conexión al sincronizar lote en " + duracionMs + " ms: " + e.getMessage());
                servicioConexion.reportarFalloConexion("Fallo de red en endpoint de sincronización", duracionMs);
                servicioConexion.reportarFinSincronizacion(0, colaLocalPort.contarPendientes());
                return 0;
            }

            if (respuesta == null || respuesta.getDetalles() == null) {
                servicioConexion.reportarFinSincronizacion(0, colaLocalPort.contarPendientes());
                return 0;
            }

            int exitosos = 0;
            for (ItemResultadoSyncDto item : respuesta.getDetalles()) {
                String idEnc = item.getIdEncomienda();
                if (idEnc == null || idEnc.isBlank()) continue;

                if (item.getResultado() == ResultadoSync.OK || item.getResultado() == ResultadoSync.YA_SINCRONIZADA) {
                    colaLocalPort.marcarSincronizada(idEnc);
                    exitosos++;
                } else if (item.getResultado() == ResultadoSync.ERROR) {
                    boolean reintentable = item.getMotivoError() != MotivoErrorSync.VALIDACION;
                    String errorMsg = item.getMensaje() != null ? item.getMensaje() : String.valueOf(item.getMotivoError());
                    colaLocalPort.marcarError(idEnc, errorMsg, reintentable);
                }
            }

            long duracionLote = System.currentTimeMillis() - inicioLote;
            int pendientesRestantes = colaLocalPort.contarPendientes();
            LOGGER.info("Lote procesado en " + duracionLote + " ms: " + exitosos + " sincronizados exitosamente, " + pendientesRestantes + " pendientes restantes.");

            servicioConexion.reportarFinSincronizacion(exitosos, pendientesRestantes);
            notificarSincronizacion(exitosos, pendientesRestantes);
            return exitosos;
        } finally {
            sincronizandoEnCurso.set(false);
        }
    }

    /**
     * Calcula los segundos de espera de backoff exponencial: 2^(min(intentos, 5)).
     * Para 1 intento = 2s, 2 = 4s, 3 = 8s, 4 = 16s, 5 = 32s.
     */
    public static long calcularSegundosBackoff(int intentos) {
        if (intentos <= 0) return 0;
        int exponente = Math.min(intentos, 5);
        return (long) Math.pow(2, exponente);
    }

    /**
     * Evalúa si un ítem debe posponer su reintento según el tiempo transcurrido desde su última actualización.
     */
    public static boolean debeEsperarBackoff(ElementoColaDto elemento, LocalDateTime ahora) {
        if (elemento.intentos() <= 0) {
            return false;
        }
        LocalDateTime referencia = elemento.actualizadoEn() != null ? elemento.actualizadoEn() : elemento.creadoEn();
        if (referencia == null) {
            return false;
        }
        long segundosEspera = calcularSegundosBackoff(elemento.intentos());
        return referencia.plusSeconds(segundosEspera).isAfter(ahora);
    }

    private void notificarSincronizacion(int totalSincronizados, int pendientesRestantes) {
        for (ListenerSincronizacion listener : listeners) {
            try {
                listener.onSincronizacionCompletada(totalSincronizados, pendientesRestantes);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al notificar listener de sincronización: " + e.getMessage());
            }
        }
    }

    private void notificarColaActualizada(int pendientesRestantes) {
        for (ListenerSincronizacion listener : listeners) {
            try {
                listener.onColaActualizada(pendientesRestantes);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al notificar listener de cola: " + e.getMessage());
            }
        }
    }
}
