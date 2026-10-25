package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
import pe.marvisur.sgldqr.api.dto.RolComboDto;
import pe.marvisur.sgldqr.api.servicio.EmpleadoServicio;

import java.util.List;
import java.util.Objects;

/**
 * Controlador REST para el catálogo de roles del sistema (RF-01, RF-03).
 * Utilizado para poblar selectores de asignación en pantallas administrativas.
 */
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles del Sistema", description = "Catálogo de roles operativos para asignación y combos (RF-01, RF-03)")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR') or hasAuthority('ROLE_ADMINISTRADOR')")
public class RolController {

    private final EmpleadoServicio empleadoServicio;

    public RolController(EmpleadoServicio empleadoServicio) {
        this.empleadoServicio = Objects.requireNonNull(empleadoServicio, "El empleadoServicio no puede ser nulo");
    }

    @GetMapping
    @Operation(
            summary = "Listar catálogo de roles",
            description = "Retorna el listado de roles registrados en el sistema (id, nombre) para llenar selectores y combos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo de roles obtenido exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol Administrador)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<RolComboDto>> listarRoles() {
        List<RolComboDto> roles = empleadoServicio.listarRoles();
        return ResponseEntity.ok(roles);
    }
}
