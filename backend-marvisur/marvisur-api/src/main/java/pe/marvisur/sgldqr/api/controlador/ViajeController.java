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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.CambiarEstadoViajeRequest;
import pe.marvisur.sgldqr.api.dto.CrearViajeRequest;
import pe.marvisur.sgldqr.api.dto.ManifiestoEncomiendaDto;
import pe.marvisur.sgldqr.api.dto.ViajeDto;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.servicio.FlotaViajeServicio;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;

import java.util.List;
import java.util.Objects;

/**
 * Controlador REST para la gestión y consulta de viajes interprovinciales programados (RF-05).
 * Permite la programación de rutas (TN3), control de estados en ruta (TN3, TN4) y consulta de manifiesto (TN2, TN3, TN4).
 */
@RestController
@RequestMapping("/api/v1/viajes")
@Tag(name = "Viajes y Flota", description = "Programación, despacho y seguimiento de viajes interprovinciales")
@SecurityRequirement(name = "BearerAuth")
public class ViajeController {

    private final FlotaViajeServicio flotaViajeServicio;

    public ViajeController(FlotaViajeServicio flotaViajeServicio) {
        this.flotaViajeServicio = Objects.requireNonNull(flotaViajeServicio, "El flotaViajeServicio no puede ser nulo");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ALMACENERO', 'ADMINISTRADOR', 'CHOFER')")
    @Operation(
            summary = "Listar viajes programados",
            description = "Consulta la programación de viajes interprovinciales con filtros opcionales por estado, sucursal de origen y chofer. "
                    + "Regla RBAC: un chofer (TN4) solo visualiza sus propios viajes asignados sin importar los filtros enviados."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de viajes obtenida exitosamente",
                    content = @Content(
                            schema = @Schema(implementation = ViajeDto.class),
                            examples = @ExampleObject(
                                    name = "Viajes Programados",
                                    value = "[{\"idViaje\":\"viaje-01\",\"idRuta\":\"ruta-01\",\"idSucursalOrigen\":\"suc-lima-01\",\"idSucursalDestino\":\"suc-arequipa-01\",\"distanciaKm\":1010.50,\"idVehiculo\":\"veh-01\",\"placaVehiculo\":\"V1A-852\",\"idChofer\":\"emp-04\",\"nombreChofer\":\"Juan Chofer Chofer\",\"fechaSalida\":\"2026-10-21T22:00:00\",\"fechaLlegadaEstimada\":\"2026-10-22T14:00:00\",\"estadoViaje\":\"PROGRAMADO\",\"ruta\":{\"id\":\"ruta-01\",\"origen\":{\"id\":\"suc-lima-01\",\"nombreAgencia\":\"Agencia Lima Central\"},\"destino\":{\"id\":\"suc-arequipa-01\",\"nombreAgencia\":\"Agencia Arequipa Terminal\"},\"distanciaKm\":1010.50},\"placa\":\"V1A-852\",\"modelo\":\"Volvo FH16\"}]"
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
            @RequestParam(name = "idSucursalOrigen", required = false) String idSucursalOrigen,
            @Parameter(description = "Filtrar por ID del chofer asignado", example = "emp-04")
            @RequestParam(name = "idChofer", required = false) String idChofer,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        List<ViajeDto> lista = flotaViajeServicio.listarViajes(estado, idSucursalOrigen, idChofer, usuario);
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(
            summary = "Programar nuevo viaje interprovincial",
            description = "Crea una nueva programación de viaje interprovincial asignando ruta, vehículo y chofer calificado. Exclusivo para Administrador (TN3)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Viaje programado exitosamente con estado inicial PROGRAMADO",
                    content = @Content(schema = @Schema(implementation = ViajeDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Petición inválida (fechas inconsistentes o fecha de salida en el pasado)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto: chofer inválido/inactivo/no-chofer, chofer ocupado o vehículo ocupado en viaje EN_RUTA",
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
    public ResponseEntity<ViajeDto> crearViaje(
            @Valid @RequestBody CrearViajeRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest httpRequest) {

        String ipOrigen = extraerIp(httpRequest);
        ViajeDto creado = flotaViajeServicio.crearViaje(request, usuario, ipOrigen);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'CHOFER')")
    @Operation(
            summary = "Actualizar estado operativo del viaje",
            description = "Permite avanzar el estado de un viaje de PROGRAMADO a EN_RUTA y de EN_RUTA a FINALIZADO. "
                    + "Autorizado para TN3 Administrador o TN4 Chofer si el viaje le pertenece."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado de viaje actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = ViajeDto.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Transición inválida entre estados (ej. PROGRAMADO a FINALIZADO)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (el chofer no tiene asignado este viaje o rol no autorizado)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Viaje no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ViajeDto> cambiarEstadoViaje(
            @PathVariable("id") String idViaje,
            @Valid @RequestBody CambiarEstadoViajeRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest httpRequest) {

        String ipOrigen = extraerIp(httpRequest);
        ViajeDto actualizado = flotaViajeServicio.cambiarEstadoViaje(idViaje, request, usuario, ipOrigen);
        return ResponseEntity.ok(actualizado);
    }

    @GetMapping("/{id}/encomiendas")
    @PreAuthorize("hasAnyRole('ALMACENERO', 'ADMINISTRADOR', 'CHOFER')")
    @Operation(
            summary = "Consultar manifiesto de carga del viaje",
            description = "Retorna el listado de encomiendas asignadas al viaje interprovincial con estricto CERO datos personales. "
                    + "Autorizado para TN2 Almacenero, TN3 Administrador o TN4 Chofer asignado al viaje."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Manifiesto de encomiendas obtenido exitosamente",
                    content = @Content(
                            schema = @Schema(implementation = ManifiestoEncomiendaDto.class),
                            examples = @ExampleObject(
                                    name = "Manifiesto de Carga",
                                    value = "[{\"codigoComprobante\":\"MV-20261021-A1B2C3D4\",\"estadoLogistico\":\"EN_RUTA\",\"nombreAgenciaDestino\":\"Agencia Arequipa Terminal\",\"pesoKg\":12.50}]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (el chofer solo puede consultar manifiestos de sus propios viajes)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Viaje no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<ManifiestoEncomiendaDto>> obtenerManifiesto(
            @PathVariable("id") String idViaje,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        List<ManifiestoEncomiendaDto> manifiesto = flotaViajeServicio.obtenerManifiesto(idViaje, usuario);
        return ResponseEntity.ok(manifiesto);
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
