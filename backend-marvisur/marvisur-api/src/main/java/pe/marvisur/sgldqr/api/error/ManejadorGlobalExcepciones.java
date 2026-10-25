package pe.marvisur.sgldqr.api.error;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionDominio;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionEntregaBloqueada;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionTransicionInvalida;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Manejador global de excepciones que transforma los errores en respuestas estandarizadas RFC 7807 (ProblemDetail)
 * con el campo extendido "codigo" para interoperabilidad y auditoría.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    @ExceptionHandler(ExcepcionOperacionInvalida.class)
    public ResponseEntity<ProblemDetail> manejarOperacionInvalida(ExcepcionOperacionInvalida ex) {
        log.warn("Operación inválida [{}]: {}", ex.getCodigo(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getTitulo());
        problem.setType(URI.create("https://marvisur.pe/errores/" + ex.getCodigo().toLowerCase().replace('_', '-')));
        problem.setProperty("codigo", ex.getCodigo());
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(ExcepcionEntregaBloqueada.class)
    public ResponseEntity<ProblemDetail> manejarEntregaBloqueada(ExcepcionEntregaBloqueada ex) {
        log.warn("Entrega bloqueada: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Entrega Bloqueada por Pago Pendiente");
        problem.setType(URI.create("https://marvisur.pe/errores/entrega-bloqueada"));
        problem.setProperty("codigo", "ENTREGA_BLOQUEADA_PAGO_PENDIENTE");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ExcepcionTransicionInvalida.class)
    public ResponseEntity<ProblemDetail> manejarTransicionInvalida(ExcepcionTransicionInvalida ex) {
        log.warn("Transición inválida: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Transición de Estado Inválida");
        problem.setType(URI.create("https://marvisur.pe/errores/transicion-invalida"));
        problem.setProperty("codigo", "TRANSICION_ESTADO_INVALIDA");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ExcepcionDominio.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionDominio(ExcepcionDominio ex) {
        log.warn("Infracción de regla de dominio: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Error de Regla de Negocio");
        problem.setType(URI.create("https://marvisur.pe/errores/regla-dominio"));
        problem.setProperty("codigo", "ERROR_DOMINIO");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    @ExceptionHandler({NoSuchElementException.class, EntityNotFoundException.class})
    public ResponseEntity<ProblemDetail> manejarNoEncontrado(Exception ex) {
        log.info("Recurso no encontrado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso No Encontrado");
        problem.setType(URI.create("https://marvisur.pe/errores/no-encontrado"));
        problem.setProperty("codigo", "RECURSO_NO_ENCONTRADO");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> manejarParametroFaltante(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        log.warn("Parámetro requerido ausente: {}", ex.getParameterName());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El parámetro requerido '" + ex.getParameterName() + "' no fue proporcionado en la solicitud."
        );
        problem.setTitle("Parámetro Obligatorio Ausente");
        problem.setType(URI.create("https://marvisur.pe/errores/parametro-ausente"));
        problem.setProperty("codigo", "PARAMETRO_AUSENTE");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarValidacion(MethodArgumentNotValidException ex) {
        log.warn("Fallo de validación en petición: {}", ex.getMessage());
        Map<String, String> campos = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "La petición contiene campos con errores de validación.");
        problem.setTitle("Validación Fallida");
        problem.setType(URI.create("https://marvisur.pe/errores/validacion-fallida"));
        problem.setProperty("codigo", "VALIDACION_FALLIDA");
        problem.setProperty("campos", campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> manejarViolacionRestricciones(ConstraintViolationException ex) {
        log.warn("Violación de restricciones: {}", ex.getMessage());
        Map<String, String> campos = new HashMap<>();
        ex.getConstraintViolations().forEach(cv ->
                campos.put(cv.getPropertyPath().toString(), cv.getMessage())
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Restricciones de validación no cumplidas.");
        problem.setTitle("Validación Fallida");
        problem.setType(URI.create("https://marvisur.pe/errores/validacion-fallida"));
        problem.setProperty("codigo", "VALIDACION_FALLIDA");
        problem.setProperty("campos", campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> manejarCredencialesInvalidas(BadCredentialsException ex) {
        log.warn("Fallo de autenticación: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Credenciales Inválidas");
        problem.setType(URI.create("https://marvisur.pe/errores/credenciales-invalidas"));
        problem.setProperty("codigo", "CREDENCIALES_INVALIDAS");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> manejarAccesoDenegado(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Acceso denegado. No posee los permisos requeridos para acceder a este recurso."
        );
        problem.setTitle("Acceso Denegado");
        problem.setType(URI.create("https://marvisur.pe/errores/acceso-denegado"));
        problem.setProperty("codigo", "ACCESO_DENEGADO");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> manejarResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        log.warn("Excepción ResponseStatusException {}: {}", ex.getStatusCode(), ex.getReason());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
        problem.setTitle("Error de Operación");
        problem.setType(URI.create("https://marvisur.pe/errores/solicitud"));
        problem.setProperty("codigo", ex.getStatusCode().toString());
        return ResponseEntity.status(ex.getStatusCode()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarErrorGenerico(Exception ex) {
        log.error("Error no controlado en el servidor", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error interno en el servidor. Contacte al administrador si el problema persiste."
        );
        problem.setTitle("Error Interno del Servidor");
        problem.setType(URI.create("https://marvisur.pe/errores/error-interno"));
        problem.setProperty("codigo", "ERROR_INTERNO");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
