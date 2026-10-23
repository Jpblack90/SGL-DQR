package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.RespuestaValidacionEntregaDto;
import pe.marvisur.sgldqr.api.dto.ValidarEntregaRequest;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.servicio.EntregaApiServicio;
import pe.marvisur.sgldqr.api.servicio.ResultadoProcesoValidacion;

import java.net.URI;
import java.util.Objects;

/**
 * Controlador REST para validación cruzada y entrega física con Doble QR en almacén (RF-07).
 */
@RestController
@RequestMapping("/api/v1/entregas")
@Tag(name = "Entregas y Validación Cruzada", description = "Operaciones de entrega física con motor criptográfico Doble QR (TN2, TN3)")
@SecurityRequirement(name = "bearerAuth")
public class EntregaController {

    private final EntregaApiServicio entregaApiServicio;

    public EntregaController(EntregaApiServicio entregaApiServicio) {
        this.entregaApiServicio = Objects.requireNonNull(entregaApiServicio, "El entregaApiServicio no puede ser nulo");
    }

    @PostMapping("/validar")
    @PreAuthorize("hasAnyRole('ROLE_ALMACENERO', 'ROLE_ADMINISTRADOR')")
    @Operation(
            summary = "Validar cruzado Doble QR y autorizar entrega física",
            description = "Valida el QR1 operativo del bulto frente al token QR2 digital del cliente. "
                    + "Si la validación es exitosa, invalida irreversiblemente el token QR2, cambia el estado logístico "
                    + "a ENTREGADO y registra auditoría y tracking. Si el pago está PENDIENTE o el token no es válido, "
                    + "devuelve código 409 (Conflict). Requiere roles Almacenero (TN2) o Administrador (TN3)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Validación cruzada exitosa y entrega física autorizada",
                    content = @Content(
                            schema = @Schema(implementation = RespuestaValidacionEntregaDto.class),
                            examples = @ExampleObject(
                                    name = "Validación Exitosa",
                                    value = "{\"idEncomienda\":\"7b92f4e8-469b-43d7-a5c9-123456789abc\",\"codigoComprobante\":\"MV-20261021-3A7MLMKY\",\"resultado\":\"EXITOSO\",\"estadoLogistico\":\"ENTREGADO\",\"mensaje\":\"Entrega física autorizada y token QR2 invalidado de forma irreversible.\",\"latenciaMs\":15,\"fechaEntrega\":\"2026-10-21T19:30:00\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "QR1 o encomienda no encontrada en el sistema",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "QR1 No Encontrado",
                                    value = "{\"type\":\"https://marvisur.pe/errores/no-encontrado\",\"title\":\"Recurso No Encontrado\",\"status\":404,\"detail\":\"No se encontró encomienda asociada al código QR1: MARVISUR:QR1:inexistente\",\"codigo\":\"RECURSO_NO_ENCONTRADO\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Validación rechazada por pago pendiente, token inválido o estado incorrecto",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Pago Pendiente (Contra Entrega)",
                                            value = "{\"type\":\"https://marvisur.pe/errores/entrega-bloqueada-pago-pendiente\",\"title\":\"Entrega Bloqueada por Pago Pendiente\",\"status\":409,\"detail\":\"La entrega está bloqueada porque la encomienda se encuentra en modalidad Contra Entrega con pago pendiente.\",\"codigo\":\"ENTREGA_BLOQUEADA_PAGO_PENDIENTE\",\"idEncomienda\":\"7b92f4e8-469b-43d7-a5c9-123456789abc\",\"codigoComprobante\":\"MV-20261021-3A7MLMKY\",\"montoTotal\":35.00,\"latenciaMs\":8}"
                                    ),
                                    @ExampleObject(
                                            name = "Token QR2 Inválido o Usado",
                                            value = "{\"type\":\"https://marvisur.pe/errores/token-qr2-invalido\",\"title\":\"Token QR2 Inválido\",\"status\":409,\"detail\":\"El código QR2 digital presentado es inválido, ya fue utilizado o no corresponde a esta encomienda.\",\"codigo\":\"TOKEN_QR2_INVALIDO\",\"latenciaMs\":12}"
                                    ),
                                    @ExampleObject(
                                            name = "Transición Inválida (No en almacén destino)",
                                            value = "{\"type\":\"https://marvisur.pe/errores/transicion-invalida\",\"title\":\"Transición Inválida\",\"status\":409,\"detail\":\"La encomienda no se encuentra en estado 'EN_ALMACEN_DESTINO'. Estado actual: REGISTRADO\",\"codigo\":\"TRANSICION_INVALIDA\",\"estadoActual\":\"REGISTRADO\"}"
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol no autorizado (TN1 Recepcionista o TN4 Chofer)")
    })
    public ResponseEntity<?> validarEntrega(
            @Valid @RequestBody ValidarEntregaRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest httpRequest
    ) {
        String ipOrigen = httpRequest.getRemoteAddr();
        ResultadoProcesoValidacion resultado = entregaApiServicio.validarEntrega(request, usuario, ipOrigen);

        return switch (resultado.getTipo()) {
            case EXITOSO -> ResponseEntity.ok(resultado.getRespuestaDto());

            case FALLIDO_PAGO_PENDIENTE -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, resultado.getMensaje());
                problem.setTitle("Entrega Bloqueada por Pago Pendiente");
                problem.setType(URI.create("https://marvisur.pe/errores/entrega-bloqueada-pago-pendiente"));
                problem.setProperty("codigo", "ENTREGA_BLOQUEADA_PAGO_PENDIENTE");
                problem.setProperty("idEncomienda", resultado.getIdEncomienda());
                problem.setProperty("codigoComprobante", resultado.getCodigoComprobante());
                problem.setProperty("montoTotal", resultado.getMontoTotalPendiente());
                problem.setProperty("latenciaMs", resultado.getLatenciaMs());
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
            }

            case FALLIDO_TOKEN_INVALIDO -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, resultado.getMensaje());
                problem.setTitle("Token QR2 Inválido");
                problem.setType(URI.create("https://marvisur.pe/errores/token-qr2-invalido"));
                problem.setProperty("codigo", "TOKEN_QR2_INVALIDO");
                problem.setProperty("latenciaMs", resultado.getLatenciaMs());
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
            }

            case TRANSICION_INVALIDA -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, resultado.getMensaje());
                problem.setTitle("Transición Inválida");
                problem.setType(URI.create("https://marvisur.pe/errores/transicion-invalida"));
                problem.setProperty("codigo", "TRANSICION_INVALIDA");
                problem.setProperty("estadoActual", resultado.getEstadoActual());
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
            }

            case NO_ENCONTRADO -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, resultado.getMensaje());
                problem.setTitle("Recurso No Encontrado");
                problem.setType(URI.create("https://marvisur.pe/errores/no-encontrado"));
                problem.setProperty("codigo", "RECURSO_NO_ENCONTRADO");
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
            }
        };
    }
}
