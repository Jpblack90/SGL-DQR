package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    /**
     * Claves exactas retornadas por la API para la distribución por estado logístico en orden estándar.
     */
    public static final List<String> CLAVES_ESTADOS_GRAFICO = List.of(
            "REGISTRADO",
            "EN_ALMACEN_ORIGEN",
            "EN_RUTA",
            "EN_ALMACEN_DESTINO",
            "ENTREGADO"
    );

    /**
     * DTO de presentación para cada barra del gráfico de estados logísticos.
     */
    public record ItemGraficoEstado(String clave, String etiqueta, long cantidad) {}

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
        if (latenciaMs == null || latenciaMs < 1.0) {
            return "< 1 ms";
        }
        return String.format(Locale.US, "%.1f ms", latenciaMs);
    }

    public static String formatearLatenciaMaxima(Integer latenciaMs) {
        if (latenciaMs == null || latenciaMs < 1) {
            return "< 1 ms";
        }
        return String.format(Locale.US, "%.1f ms", (double) latenciaMs);
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

    /**
     * Extrae los datos para las 5 barras del gráfico de estados logísticos en orden garantizado,
     * utilizando las claves exactas de la API y asignando 0 ante claves faltantes o nulas.
     */
    /**
     * Calcula el límite superior del eje Y para el gráfico de barras.
     * Garantiza números enteros enteros sin autoRanging fraccionario: max(1, valor máximo).
     */
    public static long calcularLimiteSuperiorEje(List<ItemGraficoEstado> items) {
        if (items == null || items.isEmpty()) {
            return 1L;
        }
        long max = 0L;
        for (ItemGraficoEstado item : items) {
            if (item != null && item.cantidad() > max) {
                max = item.cantidad();
            }
        }
        return Math.max(1L, max);
    }

    public static long calcularLimiteSuperiorEje(Map<String, Long> mapaEstados) {
        return calcularLimiteSuperiorEje(extraerDatosGraficoEstados(mapaEstados));
    }

    public static List<ItemGraficoEstado> extraerDatosGraficoEstados(Map<String, Long> mapaEstados) {
        List<ItemGraficoEstado> items = new ArrayList<>(CLAVES_ESTADOS_GRAFICO.size());
        for (String clave : CLAVES_ESTADOS_GRAFICO) {
            long cantidad = 0L;
            if (mapaEstados != null) {
                if (mapaEstados.containsKey(clave) && mapaEstados.get(clave) != null) {
                    cantidad = mapaEstados.get(clave);
                } else if ("REGISTRADO".equals(clave) && mapaEstados.containsKey("REGISTRADA") && mapaEstados.get("REGISTRADA") != null) {
                    cantidad = mapaEstados.get("REGISTRADA");
                } else if ("ENTREGADO".equals(clave) && mapaEstados.containsKey("ENTREGADA") && mapaEstados.get("ENTREGADA") != null) {
                    cantidad = mapaEstados.get("ENTREGADA");
                }
            }
            items.add(new ItemGraficoEstado(clave, etiquetaEstadoLogistico(clave), cantidad));
        }
        return Collections.unmodifiableList(items);
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
