package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.ItemAuditoriaDto;
import pe.marvisur.sgldqr.api.servicio.AuditoriaServicio;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Controlador REST para la consulta de la bitácora de auditoría (RF-03).
 * Acceso restringido exclusivamente para el rol Administrador (TN3).
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@Tag(name = "Auditoría del Sistema", description = "Consulta y trazabilidad de eventos operativos y de seguridad (RF-03)")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR') or hasAuthority('ROLE_ADMINISTRADOR')")
public class AuditoriaController {

    private final AuditoriaServicio auditoriaServicio;

    public AuditoriaController(AuditoriaServicio auditoriaServicio) {
        this.auditoriaServicio = Objects.requireNonNull(auditoriaServicio, "El auditoriaServicio no puede ser nulo");
    }

    @GetMapping
    @Operation(
            summary = "Consultar bitácora de auditoría",
            description = "Retorna registros de auditoría ordenados por fecha y hora descendente, con filtros opcionales por empleado, acción y rango de fechas. Tamaño máximo de página de 100 registros."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de registros de auditoría obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol Administrador)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Page<ItemAuditoriaDto>> listarAuditoria(
            @Parameter(description = "Identificador del empleado para filtrar registros", example = "EMP-ADMIN-01")
            @RequestParam(name = "idEmpleado", required = false) String idEmpleado,
            @Parameter(description = "Acción ejecutada (ej. EMPLEADO_CREADO, LOGIN_FALLIDO)", example = "EMPLEADO_CREADO")
            @RequestParam(name = "accion", required = false) String accion,
            @Parameter(description = "Fecha inicial de rango (ISO 8601 con zona horaria)", example = "2026-10-24T00:00:00-05:00")
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @Parameter(description = "Fecha final de rango (ISO 8601 con zona horaria)", example = "2026-10-24T23:59:59-05:00")
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ItemAuditoriaDto> pagina = auditoriaServicio.listarAuditoria(idEmpleado, accion, desde, hasta, pageable);
        return ResponseEntity.ok(pagina);
    }
}
