package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.RutaDto;
import pe.marvisur.sgldqr.api.servicio.FlotaViajeServicio;

import java.util.List;
import java.util.Objects;

/**
 * Controlador REST de solo lectura para rutas interprovinciales (RF-05).
 * Permite a personal operativo de almacén y transporte (TN2, TN3, TN4) consultar las rutas habilitadas.
 */
@RestController
@RequestMapping("/api/v1/rutas")
@Tag(name = "Rutas Interprovinciales", description = "Consulta de rutas terrestres habilitadas entre agencias (TN2, TN3, TN4)")
@SecurityRequirement(name = "BearerAuth")
public class RutaController {

    private final FlotaViajeServicio flotaViajeServicio;

    public RutaController(FlotaViajeServicio flotaViajeServicio) {
        this.flotaViajeServicio = Objects.requireNonNull(flotaViajeServicio, "El flotaViajeServicio no puede ser nulo");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ALMACENERO', 'ADMINISTRADOR', 'CHOFER')")
    @Operation(
            summary = "Listar rutas terrestres interprovinciales",
            description = "Consulta la lista de rutas registradas entre agencias con sus nombres oficiales y kilometraje de distancia. Endpoint de solo lectura."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de rutas obtenida exitosamente",
                    content = @Content(
                            schema = @Schema(implementation = RutaDto.class),
                            examples = @ExampleObject(
                                    name = "Rutas Interprovinciales",
                                    value = "[{\"id\":\"ruta-01\",\"origen\":{\"id\":\"suc-lima-01\",\"nombreAgencia\":\"Agencia Lima Central\"},\"destino\":{\"id\":\"suc-arequipa-01\",\"nombreAgencia\":\"Agencia Arequipa Terminal\"},\"distanciaKm\":1010.50}]"
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
                    description = "Acceso denegado (requiere TN2 Almacenero, TN3 Administrador o TN4 Chofer)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<RutaDto>> listarRutas() {
        List<RutaDto> rutas = flotaViajeServicio.listarRutas();
        return ResponseEntity.ok(rutas);
    }
}
