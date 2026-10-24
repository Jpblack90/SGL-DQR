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
import org.springframework.http.HttpHeaders;
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
import io.swagger.v3.oas.annotations.media.ExampleObject;
import pe.marvisur.sgldqr.api.dto.DetalleEncomiendaDto;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaRequest;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaResponse;
import pe.marvisur.sgldqr.api.dto.RespuestaCobroDto;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.servicio.ComprobanteServicio;
import pe.marvisur.sgldqr.api.servicio.EncomiendaApiServicio;
import pe.marvisur.sgldqr.api.servicio.EntregaApiServicio;
import pe.marvisur.sgldqr.api.servicio.ResultadoCobro;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;

import java.net.URI;
import java.util.Objects;

/**
 * Controlador REST para el ciclo de vida de encomiendas: registro con Doble QR,
 * cobro en ventanilla, consulta detallada, listado filtrado por sucursal y regeneración de imagen QR1.
 */
@RestController
@RequestMapping("/api/v1/encomiendas")
@Tag(name = "Encomiendas y Doble QR", description = "Endpoints de registro, trazabilidad y reimpresión de etiquetas QR1")
@SecurityRequirement(name = "BearerAuth")
public class EncomiendaController {

    private final EncomiendaApiServicio servicio;
    private final EntregaApiServicio entregaServicio;
    private final ComprobanteServicio comprobanteServicio;

    public EncomiendaController(EncomiendaApiServicio servicio,
                                EntregaApiServicio entregaServicio,
                                ComprobanteServicio comprobanteServicio) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio no puede ser nulo");
        this.entregaServicio = Objects.requireNonNull(entregaServicio, "El entregaServicio no puede ser nulo");
        this.comprobanteServicio = Objects.requireNonNull(comprobanteServicio, "El comprobanteServicio no puede ser nulo");
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

    @PostMapping("/{id}/cobro")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ALMACENERO', 'ADMINISTRADOR')")
    @Operation(
            summary = "Registrar cobro de encomienda Contra Entrega",
            description = "Pasa el estado_pago de PENDIENTE a PAGADO mediante el núcleo de dominio, "
                    + "registra auditoría COBRO_CONTRA_ENTREGA y evento en historial_tracking con observación 'Cobro registrado'. "
                    + "Si la encomienda ya se encuentra pagada, devuelve 409 (Conflict). No entrega el paquete físicamente. "
                    + "Permitido para Recepcionista (TN1), Almacenero (TN2) y Administrador (TN3)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cobro registrado exitosamente y encomienda desbloqueada",
                    content = @Content(
                            schema = @Schema(implementation = RespuestaCobroDto.class),
                            examples = @ExampleObject(
                                    name = "Cobro Exitoso",
                                    value = "{\"idEncomienda\":\"7b92f4e8-469b-43d7-a5c9-123456789abc\",\"codigoComprobante\":\"MV-20261021-3A7MLMKY\",\"montoTotal\":25.50,\"estadoPago\":\"PAGADO\",\"estadoLogistico\":\"EN_ALMACEN_DESTINO\",\"mensaje\":\"Cobro registrado exitosamente. La encomienda ha sido desbloqueada para su validación cruzada y entrega.\",\"fechaCobro\":\"2026-10-21T19:30:00\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Encomienda no encontrada",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Encomienda No Encontrada",
                                    value = "{\"type\":\"https://marvisur.pe/errores/no-encontrado\",\"title\":\"Recurso No Encontrado\",\"status\":404,\"detail\":\"No se encontró la encomienda con ID: enc-999\",\"codigo\":\"RECURSO_NO_ENCONTRADO\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La encomienda ya se encuentra pagada",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Ya Pagada",
                                    value = "{\"type\":\"https://marvisur.pe/errores/encomienda-ya-pagada\",\"title\":\"Encomienda Ya Pagada\",\"status\":409,\"detail\":\"La encomienda con código MV-20261021-3A7MLMKY ya se encuentra pagada.\",\"codigo\":\"YA_PAGADA\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: rol no autorizado (ej. TN4 Chofer)")
    })
    public ResponseEntity<?> registrarCobro(
            @PathVariable("id") String idEncomienda,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest httpRequest) {

        String ipOrigen = obtenerIpCliente(httpRequest);
        ResultadoCobro resultado = entregaServicio.registrarCobro(idEncomienda, usuario, ipOrigen);

        return switch (resultado.getTipo()) {
            case EXITOSO -> ResponseEntity.ok(resultado.getRespuestaDto());

            case YA_PAGADA -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, resultado.getMensaje());
                problem.setTitle("Encomienda Ya Pagada");
                problem.setType(URI.create("https://marvisur.pe/errores/encomienda-ya-pagada"));
                problem.setProperty("codigo", "YA_PAGADA");
                yield ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
            }

            case NO_ENCONTRADO -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, resultado.getMensaje());
                problem.setTitle("Recurso No Encontrado");
                problem.setType(URI.create("https://marvisur.pe/errores/no-encontrado"));
                problem.setProperty("codigo", "RECURSO_NO_ENCONTRADO");
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
            }
        };
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    @GetMapping(value = "/{id}/comprobante.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECEPCIONISTA', 'ALMACENERO')")
    @Operation(summary = "Descargar comprobante en PDF",
            description = "Descarga el comprobante informativo de control interno en PDF (RF-11). Acceso para TN1, TN2 (solo sucursal) y TN3 (todas).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF generado exitosamente",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Identificador de encomienda inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos para ver comprobante de otra sucursal o rol Chofer"),
            @ApiResponse(responseCode = "404", description = "Encomienda no encontrada")
    })
    public ResponseEntity<byte[]> descargarComprobantePdf(
            @PathVariable("id") String id,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ComprobanteServicio.ResultadoPdfComprobante resultado = comprobanteServicio.obtenerOgenerarPdfInterno(id, usuario);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resultado.nombreArchivo() + "\"")
                .body(resultado.contenidoPdf());
    }
}
