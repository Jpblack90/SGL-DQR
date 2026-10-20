package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import pe.marvisur.sgldqr.api.dto.DetalleEncomiendaDto;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaRequest;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaResponse;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.servicio.EncomiendaApiServicio;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;

import java.net.URI;
import java.util.Objects;

/**
 * Controlador REST para el ciclo de vida de encomiendas: registro con Doble QR,
 * consulta detallada, listado filtrado por sucursal y regeneración de imagen QR1.
 */
@RestController
@RequestMapping("/api/v1/encomiendas")
@Tag(name = "Encomiendas y Doble QR", description = "Endpoints de registro, trazabilidad y reimpresión de etiquetas QR1")
@SecurityRequirement(name = "BearerAuth")
public class EncomiendaController {

    private final EncomiendaApiServicio servicio;

    public EncomiendaController(EncomiendaApiServicio servicio) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio no puede ser nulo");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA')")
    @Operation(summary = "Registrar nueva encomienda",
            description = "Registra una encomienda en ventanilla, emite el par Doble QR y genera el enlace temporal a la Billetera Digital.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Encomienda registrada exitosamente",
                    content = @Content(schema = @Schema(implementation = RegistrarEncomiendaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validación de entrada fallida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Permiso denegado (requiere TN1 o TN3)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RegistrarEncomiendaResponse> registrarEncomienda(
            @Valid @RequestBody RegistrarEncomiendaRequest peticion,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest request,
            UriComponentsBuilder uriBuilder) {

        String ipOrigen = obtenerIpCliente(request);
        RegistrarEncomiendaResponse respuesta = servicio.registrarEncomienda(peticion, usuario, ipOrigen);

        URI location = uriBuilder.path("/api/v1/encomiendas/{id}")
                .buildAndExpand(respuesta.idEncomienda())
                .toUri();

        return ResponseEntity.created(location).body(respuesta);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ALMACENERO', 'RECEPCIONISTA')")
    @Operation(summary = "Consultar encomienda por ID",
            description = "Obtiene los detalles de una encomienda. Los empleados no administradores solo pueden consultar envíos vinculados a su sucursal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalles de la encomienda",
                    content = @Content(schema = @Schema(implementation = DetalleEncomiendaDto.class))),
            @ApiResponse(responseCode = "404", description = "Encomienda no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado a encomiendas de otra sucursal",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DetalleEncomiendaDto> consultarPorId(
            @PathVariable("id") String idEncomienda,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        DetalleEncomiendaDto dto = servicio.consultarPorId(idEncomienda, usuario);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ALMACENERO', 'RECEPCIONISTA')")
    @Operation(summary = "Listar encomiendas con filtros y paginación",
            description = "Consulta paginada con filtros por estado de pago, estado logístico y sucursales. Los empleados ven únicamente las de su sucursal; los administradores ven todas.")
    public ResponseEntity<Page<DetalleEncomiendaDto>> listarEncomiendas(
            @RequestParam(name = "estadoPago", required = false) EstadoPago estadoPago,
            @RequestParam(name = "estadoLogistico", required = false) EstadoLogistico estadoLogistico,
            @RequestParam(name = "idSucursalOrigen", required = false) String idSucursalOrigen,
            @RequestParam(name = "idSucursalDestino", required = false) String idSucursalDestino,
            @PageableDefault(size = 20, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        Page<DetalleEncomiendaDto> pagina = servicio.listarEncomiendas(
                estadoPago, estadoLogistico, idSucursalOrigen, idSucursalDestino, pageable, usuario
        );
        return ResponseEntity.ok(pagina);
    }

    @GetMapping(value = "/{id}/qr1.png", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ALMACENERO', 'RECEPCIONISTA')")
    @Operation(summary = "Reimprimir código QR1 físico",
            description = "Regenera el PNG del QR1 Operativo para reimpresión física en ventanilla o almacén (RF-06).")
    public ResponseEntity<byte[]> obtenerImagenQr1(
            @PathVariable("id") String idEncomienda,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        byte[] png = servicio.generarPngQr1(idEncomienda, usuario);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
