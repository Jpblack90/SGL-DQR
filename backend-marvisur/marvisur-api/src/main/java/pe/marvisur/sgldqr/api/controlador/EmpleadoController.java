package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import pe.marvisur.sgldqr.api.dto.ActualizarEmpleadoRequest;
import pe.marvisur.sgldqr.api.dto.CambiarEstadoEmpleadoRequest;
import pe.marvisur.sgldqr.api.dto.CrearEmpleadoRequest;
import pe.marvisur.sgldqr.api.dto.EmpleadoDto;
import pe.marvisur.sgldqr.api.dto.RestablecerPasswordRequest;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.servicio.EmpleadoServicio;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * Controlador REST para la gestión de personal operativo y accesos (RF-03).
 * Acceso restringido exclusivamente para el rol Administrador (TN3).
 */
@RestController
@RequestMapping("/api/v1/empleados")
@Tag(name = "Personal y Empleados", description = "Gestión de personal operativo, roles y accesos con baja lógica (RF-03)")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR') or hasAuthority('ROLE_ADMINISTRADOR')")
public class EmpleadoController {

    private final EmpleadoServicio empleadoServicio;

    public EmpleadoController(EmpleadoServicio empleadoServicio) {
        this.empleadoServicio = Objects.requireNonNull(empleadoServicio, "El empleadoServicio no puede ser nulo");
    }

    @GetMapping
    @Operation(
            summary = "Listar empleados con filtros y paginación",
            description = "Consulta colaboradores operativos con filtros por estado laboral (ACTIVOS, INACTIVOS, TODOS), rol, sucursal " +
                    "y búsqueda por texto en DNI, nombres, apellidos o correo. Tamaño de página máximo de 50 registros."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de empleados obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol Administrador)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Page<EmpleadoDto>> listarEmpleados(
            @Parameter(description = "Filtro de estado laboral (ACTIVOS, INACTIVOS, TODOS). Por defecto ACTIVOS.", example = "ACTIVOS")
            @RequestParam(name = "estado", required = false, defaultValue = "ACTIVOS") String estado,
            @Parameter(description = "ID del rol para filtrar", example = "ROL-TN1-RECEPCION")
            @RequestParam(name = "idRol", required = false) String idRol,
            @Parameter(description = "ID de la sucursal para filtrar", example = "SUC-LIM-01")
            @RequestParam(name = "idSucursal", required = false) String idSucursal,
            @Parameter(description = "Término de búsqueda textual (DNI, nombres, apellidos o correo)", example = "quispe")
            @RequestParam(name = "q", required = false) String q,
            @PageableDefault(size = 20, sort = "apellidos", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<EmpleadoDto> pagina = empleadoServicio.listarEmpleados(estado, idRol, idSucursal, q, pageable);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar empleado por ID",
            description = "Obtiene los datos completos de un colaborador sin incluir campos de contraseñas ni hashes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado encontrado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<EmpleadoDto> obtenerPorId(
            @Parameter(description = "Identificador único del empleado", example = "EMP-ADMIN-01")
            @PathVariable("id") String id) {
        EmpleadoDto dto = empleadoServicio.obtenerPorId(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Operation(
            summary = "Registrar nuevo empleado",
            description = "Registra un colaborador en el sistema con contraseña cifrada en BCrypt y auditoría transaccional."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empleado creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto por DNI o correo duplicado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<EmpleadoDto> crearEmpleado(
            @Valid @RequestBody CrearEmpleadoRequest peticion,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest request,
            UriComponentsBuilder uriBuilder) {

        String ipOrigen = obtenerIpCliente(request);
        EmpleadoDto creado = empleadoServicio.crearEmpleado(peticion, usuario, ipOrigen);

        URI location = uriBuilder.path("/api/v1/empleados/{id}")
                .buildAndExpand(creado.idEmpleado())
                .toUri();

        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar datos de empleado",
            description = "Modifica nombres, apellidos, correo, rol y sucursal. El DNI no es modificable. Protege contra el cambio del propio rol del administrador y el último administrador activo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Empleado, rol o sucursal no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Acción prohibida (correo duplicado, auto-cambio de rol o último administrador)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<EmpleadoDto> actualizarEmpleado(
            @Parameter(description = "Identificador del empleado a actualizar", example = "EMP-001")
            @PathVariable("id") String id,
            @Valid @RequestBody ActualizarEmpleadoRequest peticion,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest request) {

        String ipOrigen = obtenerIpCliente(request);
        EmpleadoDto actualizado = empleadoServicio.actualizarEmpleado(id, peticion, usuario, ipOrigen);
        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/estado")
    @Operation(
            summary = "Cambiar estado de vigencia laboral (baja lógica o reactivación)",
            description = "Establece el estado activo del empleado. Prohíbe la autobaja del administrador y la baja del último administrador activo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado modificado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Acción no permitida (autobaja o último administrador)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<EmpleadoDto> cambiarEstado(
            @Parameter(description = "Identificador del empleado", example = "EMP-001")
            @PathVariable("id") String id,
            @Valid @RequestBody CambiarEstadoEmpleadoRequest peticion,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest request) {

        String ipOrigen = obtenerIpCliente(request);
        EmpleadoDto actualizado = empleadoServicio.cambiarEstado(id, peticion, usuario, ipOrigen);
        return ResponseEntity.ok(actualizado);
    }

    @PostMapping("/{id}/password")
    @Operation(
            summary = "Restablecer contraseña de empleado",
            description = "Permite al administrador fijar una nueva contraseña segura para el colaborador. La contraseña no se retorna en la respuesta."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente"),
            @ApiResponse(responseCode = "400", description = "Contraseña no cumple la política de complejidad", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Map<String, String>> restablecerPassword(
            @Parameter(description = "Identificador del empleado", example = "EMP-001")
            @PathVariable("id") String id,
            @Valid @RequestBody RestablecerPasswordRequest peticion,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest request) {

        String ipOrigen = obtenerIpCliente(request);
        empleadoServicio.restablecerPassword(id, peticion, usuario, ipOrigen);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña restablecida exitosamente"));
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
