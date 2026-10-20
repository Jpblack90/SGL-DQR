package pe.marvisur.sgldqr.api.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionDominio;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionEntregaBloqueada;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionTransicionInvalida;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Pruebas unitarias de ManejadorGlobalExcepciones (RFC 7807 ProblemDetail)")
class ManejadorGlobalExcepcionesTest {

    private final ManejadorGlobalExcepciones handler = new ManejadorGlobalExcepciones();

    @Test
    @DisplayName("ExcepcionEntregaBloqueada retorna HTTP 409 con codigo ENTREGA_BLOQUEADA_PAGO_PENDIENTE")
    void testEntregaBloqueadaRetorna409() {
        ExcepcionEntregaBloqueada ex = new ExcepcionEntregaBloqueada("enc-001");
        ResponseEntity<ProblemDetail> respuesta = handler.manejarEntregaBloqueada(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = respuesta.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
        assertThat(body.getProperties()).containsEntry("codigo", "ENTREGA_BLOQUEADA_PAGO_PENDIENTE");
        assertThat(body.getDetail()).contains("enc-001");
    }

    @Test
    @DisplayName("ExcepcionTransicionInvalida retorna HTTP 409 con codigo TRANSICION_ESTADO_INVALIDA")
    void testTransicionInvalidaRetorna409() {
        ExcepcionTransicionInvalida ex = new ExcepcionTransicionInvalida(EstadoLogistico.REGISTRADO, EstadoLogistico.ENTREGADO);
        ResponseEntity<ProblemDetail> respuesta = handler.manejarTransicionInvalida(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = respuesta.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(409);
        assertThat(body.getProperties()).containsEntry("codigo", "TRANSICION_ESTADO_INVALIDA");
    }

    @Test
    @DisplayName("ExcepcionDominio generica retorna HTTP 422 con codigo ERROR_DOMINIO")
    void testExcepcionDominioRetorna422() {
        ExcepcionDominio ex = new ExcepcionDominio("Regla de negocio infringida");
        ResponseEntity<ProblemDetail> respuesta = handler.manejarExcepcionDominio(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        ProblemDetail body = respuesta.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(422);
        assertThat(body.getProperties()).containsEntry("codigo", "ERROR_DOMINIO");
        assertThat(body.getDetail()).isEqualTo("Regla de negocio infringida");
    }

    @Test
    @DisplayName("NoSuchElementException retorna HTTP 404 con codigo RECURSO_NO_ENCONTRADO")
    void testNoEncontradoRetorna404() {
        NoSuchElementException ex = new NoSuchElementException("Encomienda no encontrada con id: enc-999");
        ResponseEntity<ProblemDetail> respuesta = handler.manejarNoEncontrado(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail body = respuesta.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getProperties()).containsEntry("codigo", "RECURSO_NO_ENCONTRADO");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException retorna HTTP 400 con codigo VALIDACION_FALLIDA y mapa de campos")
    void testValidacionRetorna400ConCampos() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("comando", "pesoKg", "El peso debe ser mayor a cero");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ProblemDetail> respuesta = handler.manejarValidacion(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = respuesta.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getProperties()).containsEntry("codigo", "VALIDACION_FALLIDA");

        @SuppressWarnings("unchecked")
        Map<String, String> campos = (Map<String, String>) body.getProperties().get("campos");
        assertThat(campos).containsEntry("pesoKg", "El peso debe ser mayor a cero");
    }

    @Test
    @DisplayName("Exception no controlada retorna HTTP 500 con codigo ERROR_INTERNO sin filtrar detalles")
    void testErrorGenericoRetorna500SinDetalles() {
        RuntimeException ex = new RuntimeException("Error fatal en base de datos: conexion perdida");
        ResponseEntity<ProblemDetail> respuesta = handler.manejarErrorGenerico(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ProblemDetail body = respuesta.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(500);
        assertThat(body.getProperties()).containsEntry("codigo", "ERROR_INTERNO");
        assertThat(body.getDetail()).doesNotContain("conexion perdida");
    }
}
