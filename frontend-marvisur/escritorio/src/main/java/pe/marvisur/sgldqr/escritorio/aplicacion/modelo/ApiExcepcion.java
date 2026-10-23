package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.util.Collections;
import java.util.Map;

/**
 * Excepción lanzada al ocurrir errores de comunicación o respuestas de error de la API REST.
 */
public class ApiExcepcion extends RuntimeException {

    private final int statusCode;
    private final String codigoError;
    private final Map<String, String> erroresValidacion;

    public ApiExcepcion(String mensaje) {
        super(mensaje);
        this.statusCode = 0;
        this.codigoError = "ERROR_CONEXION";
        this.erroresValidacion = Collections.emptyMap();
    }

    public ApiExcepcion(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.statusCode = 0;
        this.codigoError = "ERROR_CONEXION";
        this.erroresValidacion = Collections.emptyMap();
    }

    public ApiExcepcion(int statusCode, String mensaje) {
        super(mensaje);
        this.statusCode = statusCode;
        this.codigoError = "HTTP_" + statusCode;
        this.erroresValidacion = Collections.emptyMap();
    }

    public ApiExcepcion(int statusCode, String codigoError, String mensaje, Map<String, String> erroresValidacion) {
        super(mensaje);
        this.statusCode = statusCode;
        this.codigoError = codigoError != null ? codigoError : "HTTP_" + statusCode;
        this.erroresValidacion = erroresValidacion != null ? erroresValidacion : Collections.emptyMap();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCodigoError() {
        return codigoError;
    }

    public Map<String, String> getErroresValidacion() {
        return erroresValidacion;
    }

    public boolean esNoAutorizado() {
        return statusCode == 401;
    }

    public boolean esNoEncontrado() {
        return statusCode == 404;
    }

    public boolean esConflicto() {
        return statusCode == 409;
    }
}
