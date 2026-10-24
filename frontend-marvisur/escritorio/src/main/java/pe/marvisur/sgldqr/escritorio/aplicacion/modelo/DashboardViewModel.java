package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Modelo de vista para la lógica de presentación del Dashboard (RF-02) sin dependencias de JavaFX.
 */
public class DashboardViewModel {

    public static final String MENSAJE_SIN_CONEXION = "Requiere conexión";
    public static final String MENSAJE_ACCESO_DENEGADO = "No tienes permiso para ver el dashboard";
    public static final String TEXTO_TABLA_ESTANCADOS_VACIA = "Sin envíos estancados";
    public static final String COLOR_AMBAR_LATENCIA_CRITICA = "#F59E0B";
    public static final String NOTA_PIE_PAGINA = "Recaudado cuenta por fecha de registro del envío.";
    public static final int LIMITE_LATENCIA_RNF03_MS = 1000;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DashboardResumenDto datos;
    private final boolean desactualizado;
    private final String mensajeError;

    public DashboardViewModel(DashboardResumenDto datos) {
        this(datos, false, null);
    }

    public DashboardViewModel(DashboardResumenDto datos, boolean desactualizado, String mensajeError) {
        this.datos = datos;
        this.desactualizado = desactualizado;
        this.mensajeError = mensajeError;
    }

    public DashboardResumenDto getDatos() {
        return datos;
    }

    public boolean isDesactualizado() {
        return desactualizado;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    // --- Métodos de formateo y reglas de presentación ---

    public static String formatearMonto(BigDecimal monto) {
        if (monto == null) {
            return "S/ 0.00";
        }
        return String.format(Locale.US, "S/ %.2f", monto);
    }

    public static String formatearHoras(long horas) {
        return horas + " h";
    }

    public static String formatearLatenciaPromedio(Double latenciaMs) {
        if (latenciaMs == null || latenciaMs <= 0.0) {
            return "0.0 ms";
        }
        return String.format(Locale.US, "%.1f ms", latenciaMs);
    }

    public static String formatearLatenciaMaxima(Integer latenciaMs) {
        if (latenciaMs == null || latenciaMs <= 0) {
            return "0 ms";
        }
        return latenciaMs + " ms";
    }

    public static boolean esLatenciaCritica(Integer latenciaMaximaMs) {
        return latenciaMaximaMs != null && latenciaMaximaMs > LIMITE_LATENCIA_RNF03_MS;
    }

    public static String etiquetaEstadoLogistico(String codigoEstado) {
        if (codigoEstado == null) {
            return "Desconocido";
        }
        return switch (codigoEstado.trim().toUpperCase()) {
            case "REGISTRADA", "REGISTRADO" -> "Registrado";
            case "EN_ALMACEN_ORIGEN" -> "Almacén origen";
            case "EN_RUTA", "EN_TRANSITO" -> "En ruta";
            case "EN_ALMACEN_DESTINO" -> "Almacén destino";
            case "ENTREGADA", "ENTREGADO" -> "Entregado";
            case "BLOQUEADA", "BLOQUEADO" -> "Bloqueada";
            case "CANCELADA", "CANCELADO" -> "Cancelada";
            default -> codigoEstado;
        };
    }

    public static String formatearHoraLocal(OffsetDateTime fechaHora) {
        if (fechaHora == null) {
            return "--:--:--";
        }
        return fechaHora.atZoneSameInstant(ZoneId.systemDefault()).format(FORMATO_HORA);
    }

    public static String formatearTextoActualizacion(OffsetDateTime generadoEn, boolean desactualizado, String error) {
        String hora = formatearHoraLocal(generadoEn);
        if (desactualizado) {
            String sufijo = (error != null && !error.isBlank()) ? " · " + error : "";
            return "Desactualizado (último: " + hora + ")" + sufijo;
        }
        return "Actualizado: " + hora;
    }

    public static String textoUmbralEstancado(int horas) {
        return "Umbral: " + horas + " horas";
    }

    public static String limpiarMensajeError(Throwable error) {
        if (error == null) {
            return "Error inesperado en el dashboard";
        }
        if (error instanceof ApiExcepcion ae) {
            if (ae.getStatusCode() == 403) {
                return MENSAJE_ACCESO_DENEGADO;
            }
            if (ae.getStatusCode() == 401) {
                return "Sesión expirada o no autorizada";
            }
            String msg = ae.getMessage();
            if (msg != null && !msg.isBlank() && !msg.equalsIgnoreCase("null")) {
                return msg.replace("null", "").trim();
            }
        }
        String msg = error.getMessage();
        if (msg == null || msg.isBlank() || msg.equalsIgnoreCase("null")) {
            return "Error de comunicación con el servidor central";
        }
        return msg.replace("null", "").trim();
    }
}
