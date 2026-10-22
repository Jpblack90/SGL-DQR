package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.DetallePreRegistroVentanillaDto;
import pe.marvisur.sgldqr.api.servicio.PublicoServicio;

import java.util.Objects;

/**
 * Controlador interno para la consulta de pre-registros web ('Código Express') desde la ventanilla (TN1, TN3).
 */
@RestController
@RequestMapping("/api/v1/preregistros")
@Tag(name = "Pre-Registros Ventanilla", description = "Servicios internos para precargar datos de pre-registros en el formulario de registro")
@SecurityRequirement(name = "BearerAuth")
public class PreRegistroController {

    private final PublicoServicio publicoServicio;

    public PreRegistroController(PublicoServicio publicoServicio) {
        this.publicoServicio = Objects.requireNonNull(publicoServicio, "El publicoServicio no puede ser nulo");
    }

    @GetMapping("/{codigoExpress}")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ADMINISTRADOR')")
    @Operation(summary = "Consultar pre-registro por código express", description = "Recupera los datos pre-cargados por el cliente para el registro definitivo en ventanilla.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del pre-registro recuperados"),
            @ApiResponse(responseCode = "404", description = "Pre-registro no encontrado"),
            @ApiResponse(responseCode = "409", description = "Pre-registro expirado o ya procesado")
    })
    public ResponseEntity<DetallePreRegistroVentanillaDto> consultarPorCodigo(
            @Parameter(description = "Código express legible (e.g. PRE-481920-PE)", required = true)
            @PathVariable(name = "codigoExpress") String codigoExpress) {
        return ResponseEntity.ok(publicoServicio.consultarPreRegistroVentanilla(codigoExpress));
    }
}
