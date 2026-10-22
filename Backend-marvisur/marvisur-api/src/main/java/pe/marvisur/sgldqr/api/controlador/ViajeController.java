package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.ViajeDto;
import pe.marvisur.sgldqr.api.servicio.AlmacenServicio;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;

import java.util.List;
import java.util.Objects;

/**
 * Controlador REST para la consulta de viajes interprovinciales programados.
 * Permite a los colaboradores de almacén y transporte (TN2, TN3, TN4) consultar
 * los viajes activos para asociar lotes de despacho.
 */
@RestController
@RequestMapping("/api/v1/viajes")
@Tag(name = "Viajes y Flota", description = "Consulta de programación de viajes interprovinciales para despacho de lotes")
@SecurityRequirement(name = "BearerAuth")
public class ViajeController {

    private final AlmacenServicio almacenServicio;

    public ViajeController(AlmacenServicio almacenServicio) {
        this.almacenServicio = Objects.requireNonNull(almacenServicio, "El almacenServicio no puede ser nulo");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ALMACENERO', 'ADMINISTRADOR', 'CHOFER')")
    @Operation(
            summary = "Listar viajes programados",
            description = "Consulta la programación de viajes interprovinciales con filtros opcionales por estado del viaje "
                    + "y sucursal de origen de la ruta. Endpoint de solo lectura para soporte al despacho en almacén."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de viajes obtenida exitosamente",
                    content = @Content(
                            schema = @Schema(implementation = ViajeDto.class),
                            examples = @ExampleObject(
                                    name = "Viajes Programados",
                                    value = "[{\"idViaje\":\"viaje-01\",\"idRuta\":\"ruta-01\",\"idSucursalOrigen\":\"suc-lima-01\",\"idSucursalDestino\":\"suc-arequipa-01\",\"distanciaKm\":1010.50,\"idVehiculo\":\"veh-01\",\"placaVehiculo\":\"V1A-852\",\"idChofer\":\"emp-04\",\"nombreChofer\":\"Juan Chofer Chofer\",\"fechaSalida\":\"2026-10-21T22:00:00\",\"fechaLlegadaEstimada\":\"2026-10-22T14:00:00\",\"estadoViaje\":\"PROGRAMADO\"}]"
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
    public ResponseEntity<List<ViajeDto>> listarViajes(
            @Parameter(description = "Filtrar por estado del viaje (PROGRAMADO, EN_RUTA, FINALIZADO)", example = "PROGRAMADO")
            @RequestParam(name = "estado", required = false) EstadoViaje estado,
            @Parameter(description = "Filtrar por ID de la sucursal de origen de la ruta", example = "suc-lima-01")
            @RequestParam(name = "idSucursalOrigen", required = false) String idSucursalOrigen) {

        List<ViajeDto> lista = almacenServicio.listarViajes(estado, idSucursalOrigen);
        return ResponseEntity.ok(lista);
    }
}
