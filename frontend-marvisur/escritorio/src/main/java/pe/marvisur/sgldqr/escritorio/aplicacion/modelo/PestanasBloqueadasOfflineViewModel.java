package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Modelo de vista que evalúa la disponibilidad de pestañas y módulos según el estado de conectividad (O1, RNF-02).
 *
 * <p><strong>Reglas de Alcance Honesto del Modo Offline:</strong>
 * <ul>
 *   <li>Solo se soporta REGISTRAR encomiendas sin conexión (Ventanilla con Doble QR).</li>
 *   <li>Sin conexión NO están disponibles: login nuevo (la sesión iniciada con conexión sigue vigente),
 *       Código Express, Entrega, Lotes ni Contingencia: sus pestañas muestran «Requiere conexión».</li>
 * </ul>
 * </p>
 */
public class PestanasBloqueadasOfflineViewModel {

    public static final String MENSAJE_REQUIERE_CONEXION = "Requiere conexión: esta operación requiere conexión activa con el servidor central.";
    public static final String MENSAJE_LOGIN_REQUIERE_CONEXION = "Requiere conexión: el inicio de sesión inicial requiere conexión con el servidor central. La sesión previamente iniciada se mantiene en memoria sin conexión.";
    public static final String MENSAJE_EXPRESS_REQUIERE_CONEXION = "Requiere conexión: la precarga de Código Express requiere conexión activa con el servidor central.";

    public enum ModuloPestana {
        VENTANILLA_REGISTRO,
        CODIGO_EXPRESS,
        ENTREGA,
        LOTES,
        CONTINGENCIA,
        LOGIN_NUEVO
    }

    private final boolean enLinea;

    public PestanasBloqueadasOfflineViewModel(boolean enLinea) {
        this.enLinea = enLinea;
    }

    public boolean estaHabilitado(ModuloPestana modulo) {
        if (modulo == ModuloPestana.VENTANILLA_REGISTRO) {
            return true; // Siempre habilitado (modo offline-first soportado para emitir encomiendas)
        }
        return enLinea;
    }

    public String obtenerMensaje(ModuloPestana modulo) {
        if (estaHabilitado(modulo)) {
            return "Operación disponible.";
        }
        return switch (modulo) {
            case LOGIN_NUEVO -> MENSAJE_LOGIN_REQUIERE_CONEXION;
            case CODIGO_EXPRESS -> MENSAJE_EXPRESS_REQUIERE_CONEXION;
            default -> MENSAJE_REQUIERE_CONEXION;
        };
    }

    public boolean esVentanillaHabilitada() {
        return estaHabilitado(ModuloPestana.VENTANILLA_REGISTRO);
    }

    public boolean esCodigoExpressHabilitado() {
        return estaHabilitado(ModuloPestana.CODIGO_EXPRESS);
    }

    public boolean esEntregaHabilitada() {
        return estaHabilitado(ModuloPestana.ENTREGA);
    }

    public boolean esLotesHabilitado() {
        return estaHabilitado(ModuloPestana.LOTES);
    }

    public boolean esContingenciaHabilitada() {
        return estaHabilitado(ModuloPestana.CONTINGENCIA);
    }

    public boolean esLoginNuevoHabilitado() {
        return estaHabilitado(ModuloPestana.LOGIN_NUEVO);
    }

    public boolean isEnLinea() {
        return enLinea;
    }
}
