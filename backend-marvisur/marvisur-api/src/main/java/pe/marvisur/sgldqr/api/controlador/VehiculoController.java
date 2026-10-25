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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.CrearVehiculoRequest;
import pe.marvisur.sgldqr.api.dto.VehiculoDto;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.servicio.FlotaViajeServicio;

import java.util.List;
import java.util.Objects;

/**
 * Controlador REST para la gestión de flota de vehículos de transporte interprovincial (RF-05).
 * Permite a los administradores (TN3) registrar y consultar unidades vehiculares.
 */
@RestController
@RequestMapping("/api/v1/vehiculos")
@Tag(name = "Flota de Vehículos", description = "Gestión del parque automotor de transporte interprovincial (TN3)")
@SecurityRequirement(name = "BearerAuth")
public class VehiculoController {

    private final FlotaViajeServicio flotaViajeServicio;

    public VehiculoController(FlotaViajeServicio flotaViajeServicio) {
        this.flotaViajeServicio = Objects.requireNonNull(flotaViajeServicio, "El flotaViajeServicio no puede ser nulo");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Listar vehículos de la flota",
            description = "Consulta la lista completa de vehículos activos registrados en el sistema, ordenados por placa. Exclusivo para Administrador (TN3)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de vehículos obtenida exitosamente",
                    content = @Content(
                            schema = @Schema(implementation = VehiculoDto.class),
                            examples = @ExampleObject(
                                    name = "Lista de Vehículos",
                                    value = "[{\"idVehiculo\":\"veh-01\",\"placa\":\"V1A-852\",\"modelo\":\"Volvo FH16\",\"capacidadCargaKg\":25000.00}]"
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
                    description = "Acceso denegado (requiere rol TN3 Administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<VehiculoDto>> listarVehiculos() {
        List<VehiculoDto> vehiculos = flotaViajeServicio.listarVehiculos();
        return ResponseEntity.ok(vehiculos);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Registrar nuevo vehículo",
            description = "Registra una nueva unidad vehicular en la flota con placa única y capacidad de carga mayor a cero. Exclusivo para Administrador (TN3)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Vehículo registrado exitosamente",
                    content = @Content(
                            schema = @Schema(implementation = VehiculoDto.class),
                            examples = @ExampleObject(
                                    name = "Vehículo Creado",
                                    value = "{\"idVehiculo\":\"550e8400-e29b-41d4-a716-446655440000\",\"placa\":\"V1A-852\",\"modelo\":\"Volvo FH16\",\"capacidadCargaKg\":25000.00}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validación fallida (placa inválida o capacidad menor o igual a cero)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto: la placa ya se encuentra registrada en el sistema",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol TN3 Administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<VehiculoDto> crearVehiculo(
            @Valid @RequestBody CrearVehiculoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest httpRequest) {

        String ipOrigen = extraerIp(httpRequest);
        VehiculoDto creado = flotaViajeServicio.crearVehiculo(request, usuario.getIdEmpleado(), ipOrigen);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    private String extraerIp(HttpServletRequest request) {
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
