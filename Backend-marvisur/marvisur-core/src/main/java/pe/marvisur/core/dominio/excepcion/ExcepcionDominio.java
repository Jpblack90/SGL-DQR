package pe.marvisur.core.dominio.excepcion;

/**
 * Excepción base para todas las excepciones de dominio del sistema SGL-DQR.
 * <p>
 * Extiende {@link RuntimeException} para no forzar manejo explícito en cada
 * capa de la aplicación; los adaptadores deciden cómo presentarla al usuario.
 * </p>
 */
public class ExcepcionDominio extends RuntimeException {

    /**
     * Crea una excepción de dominio con un mensaje descriptivo.
     *
     * @param mensaje descripción del error de dominio
     */
    public ExcepcionDominio(String mensaje) {
        super(mensaje);
    }

    /**
     * Crea una excepción de dominio con mensaje y causa original.
     *
     * @param mensaje descripción del error de dominio
     * @param causa   excepción original que provocó este error
     */
    public ExcepcionDominio(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
