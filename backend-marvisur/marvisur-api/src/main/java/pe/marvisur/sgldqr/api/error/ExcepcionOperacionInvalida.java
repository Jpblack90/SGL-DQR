package pe.marvisur.sgldqr.api.error;

import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * Excepción lanzada ante operaciones de negocio inválidas o conflictos de estado (RF-01, RF-03).
 * Proporciona código de error para ProblemDetail RFC 7807.
 */
public class ExcepcionOperacionInvalida extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final String titulo;

    public ExcepcionOperacionInvalida(HttpStatus status, String codigo, String titulo, String mensaje) {
        super(mensaje);
        this.status = Objects.requireNonNull(status, "status no puede ser nulo");
        this.codigo = Objects.requireNonNull(codigo, "codigo no puede ser nulo");
        this.titulo = Objects.requireNonNull(titulo, "titulo no puede ser nulo");
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }
}
