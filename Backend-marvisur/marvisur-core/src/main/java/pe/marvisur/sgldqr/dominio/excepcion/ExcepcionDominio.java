package pe.marvisur.sgldqr.dominio.excepcion;

/**
 * Excepción base para todas las infracciones de reglas de negocio del dominio SGL-DQR.
 */
public class ExcepcionDominio extends RuntimeException {

    public ExcepcionDominio(String mensaje) {
        super(mensaje);
    }

    public ExcepcionDominio(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
