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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.RespuestaSyncLoteDto;
import pe.marvisur.sgldqr.api.dto.SyncLoteRequest;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.servicio.SyncServicio;

import java.util.Objects;

/**
 * Controlador REST para la sincronización de encomiendas registradas sin conexión (offline-first).
 */
@RestController
@RequestMapping("/api/v1/sync")
@Tag(name = "Sincronización Offline", description = "Endpoints para sincronización central de encomiendas registradas localmente en ventanilla (RNF-02, B4)")
@SecurityRequirement(name = "BearerAuth")
public class SyncController {

    private final SyncServicio syncServicio;

    public SyncController(SyncServicio syncServicio) {
        this.syncServicio = Objects.requireNonNull(syncServicio, "El syncServicio no puede ser nulo");
    }

    @PostMapping("/encomiendas")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(
            summary = "Sincronización de encomiendas registradas offline",
            description = "Recibe un lote de encomiendas registradas localmente sin conexión por el cliente de escritorio. "
                    + "Procesa cada ítem en una transacción autónoma independiente (REQUIRES_NEW). "
                    + "Garantiza idempotencia estricta, recálculo de tarifas oficiales con TarifarioPort, "
                    + "validación cruzada de sucursal de origen y registro de hashes criptográficos del Doble QR. "
                    + "Permitido exclusivamente para Recepcionista (TN1) y Administrador (TN3). Para TN2/TN4 responde 403 Forbidden."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lote procesado exitosamente (con resultados individuales por ítem)",
                    content = @Content(
                            schema = @Schema(implementation = RespuestaSyncLoteDto.class),
                            examples = @ExampleObject(
                                    name = "Lote Sincronizado",
                                    value = "{\"totalProcesados\":2,\"totalExitosos\":2,\"totalYaSincronizados\":0,\"totalErrores\":0,\"tiempoProcesamientoMs\":62,\"detalles\":[{\"idEncomienda\":\"7b92f4e8-469b-43d7-a5c9-123456789abc\",\"codigoComprobante\":\"MV-20261022-3A7MLMKY\",\"resultado\":\"OK\",\"advertencia\":null,\"motivoError\":null,\"mensaje\":\"Sincronización exitosa\"}]}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Petición inválida (lote vacío o excede app.sync.lote-maximo)",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Lote Vacío o Excedido",
                                    value = "{\"type\":\"https://marvisur.pe/errores/solicitud-invalida\",\"title\":\"Solicitud Inválida\",\"status\":400,\"detail\":\"El lote de sincronización no puede estar vacío\",\"codigo\":\"SOLICITUD_INVALIDA\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol RECEPCIONISTA o ADMINISTRADOR)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RespuestaSyncLoteDto> sincronizarEncomiendas(
            @RequestBody SyncLoteRequest peticion,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest request) {

        String ipOrigen = obtenerIpCliente(request);
        RespuestaSyncLoteDto respuesta = syncServicio.sincronizarLote(peticion, usuario, ipOrigen);
        return ResponseEntity.ok(respuesta);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
