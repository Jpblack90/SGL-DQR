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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.EncomiendaBusquedaDto;
import pe.marvisur.sgldqr.api.dto.IncidenciaDetalleDto;
import pe.marvisur.sgldqr.api.dto.LoteEscaneoRequest;
import pe.marvisur.sgldqr.api.dto.RegistrarIncidenciaRequest;
import pe.marvisur.sgldqr.api.dto.RespuestaIncidenciaDto;
import pe.marvisur.sgldqr.api.dto.RespuestaLoteEscaneoDto;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.servicio.AlmacenServicio;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Controlador REST para operaciones de almacén: escaneo masivo por lotes,
 * búsqueda de emergencia por documento y gestión de incidencias de etiquetas.
 */
@RestController
@RequestMapping("/api/v1/almacen")
@Tag(name = "Módulo de Almacén", description = "Operaciones de recepción, despacho masivo, búsqueda de emergencia e incidencias (RF-06)")
@SecurityRequirement(name = "BearerAuth")
public class AlmacenController {

    private final AlmacenServicio almacenServicio;

    public AlmacenController(AlmacenServicio almacenServicio) {
        this.almacenServicio = Objects.requireNonNull(almacenServicio, "El almacenServicio no puede ser nulo");
    }

    // ------------------------------------------------------------------------
    // 1. Procesamiento masivo por lote de escaneo
    // ------------------------------------------------------------------------
    @PostMapping("/lotes/escaneo")
    @PreAuthorize("hasAnyRole('ALMACENERO', 'ADMINISTRADOR')")
    @Operation(
            summary = "Procesamiento masivo por lote de escaneo",
            description = "Ejecuta una acción masiva sobre un lote de códigos QR1 (hasta el límite configurado). "
                    + "Cada ítem se procesa en una transacción autónoma e independiente (un fallo individual no aborta el lote). "
                    + "Acciones: RECEPCIONAR_ORIGEN (REGISTRADO -> EN_ALMACEN_ORIGEN), "
                    + "DESPACHAR_A_VIAJE (EN_ALMACEN_ORIGEN -> EN_RUTA con asignación a viaje), "
                    + "RECEPCIONAR_DESTINO (EN_RUTA -> EN_ALMACEN_DESTINO). "
                    + "Permitido exclusivamente para Almacenero (TN2) y Administrador (TN3)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lote procesado exitosamente (con resultados individuales por ítem)",
                    content = @Content(
                            schema = @Schema(implementation = RespuestaLoteEscaneoDto.class),
                            examples = @ExampleObject(
                                    name = "Lote Procesado con Éxito Parcial",
                                    value = "{\"totalProcesados\":3,\"totalExitosos\":1,\"totalYaRegistrados\":1,\"totalErrores\":1,\"tiempoProcesamientoMs\":85,\"detalles\":[{\"qr1\":\"MV-QR1-001\",\"idEncomienda\":\"enc-01\",\"resultado\":\"OK\",\"motivo\":null},{\"qr1\":\"MV-QR1-002\",\"idEncomienda\":\"enc-02\",\"resultado\":\"YA_REGISTRADA\",\"motivo\":null},{\"qr1\":\"MV-QR1-999\",\"idEncomienda\":null,\"resultado\":\"ERROR\",\"motivo\":\"QR1_INEXISTENTE\"}]}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Petición inválida (lote vacío, tamaño excede el máximo permitido o idViaje faltante para despacho)",
                    content = @Content(
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(
                                    name = "Lote Excedido",
                                    value = "{\"type\":\"https://marvisur.pe/errores/solicitud-invalida\",\"title\":\"Solicitud Inválida\",\"status\":400,\"detail\":\"El tamaño del lote (201) excede el límite configurado de 200 ítems\",\"codigo\":\"SOLICITUD_INVALIDA\"}"
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
                    description = "Acceso denegado (requiere TN2 Almacenero o TN3 Administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RespuestaLoteEscaneoDto> procesarLoteEscaneo(
            @Valid @RequestBody LoteEscaneoRequest peticion,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest request) {

        String ipOrigen = obtenerIpCliente(request);
        RespuestaLoteEscaneoDto respuesta = almacenServicio.procesarLote(peticion, usuario, ipOrigen);
        return ResponseEntity.ok(respuesta);
    }

    // ------------------------------------------------------------------------
    // 2. Búsqueda de emergencia por DNI o RUC
    // ------------------------------------------------------------------------
    @GetMapping("/encomiendas/buscar")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ALMACENERO', 'ADMINISTRADOR')")
    @Operation(
            summary = "Búsqueda de emergencia por DNI o RUC",
            description = "Permite localizar encomiendas no entregadas asociadas al número de documento del remitente o destinatario. "
                    + "Aplica alcance de seguridad por sucursal: los colaboradores ven encomiendas cuyo origen o destino coincida con su sucursal; "
                    + "el Administrador posee visibilidad global. Permitido para TN1, TN2 y TN3."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de encomiendas encontradas",
                    content = @Content(
                            schema = @Schema(implementation = EncomiendaBusquedaDto.class),
                            examples = @ExampleObject(
                                    name = "Encomiendas Encontradas",
                                    value = "[{\"idEncomienda\":\"enc-01\",\"codigoComprobante\":\"MV-20261021-3A7MLMKY\",\"numeroDocumentoRemitente\":\"45879632\",\"nombreRemitente\":\"Juan Pérez\",\"numeroDocumentoDestinatario\":\"20601234567\",\"nombreDestinatario\":\"Comercial Sur SAC\",\"idSucursalOrigen\":\"suc-lima-01\",\"idSucursalDestino\":\"suc-arequipa-01\",\"pesoKg\":10.50,\"montoTotal\":31.25,\"estadoPago\":\"PENDIENTE\",\"estadoLogistico\":\"EN_ALMACEN_DESTINO\",\"fechaRegistro\":\"2026-10-21T18:00:00\"}]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Documento de búsqueda vacío o inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere TN1, TN2 o TN3)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<EncomiendaBusquedaDto>> buscarEncomiendas(
            @Parameter(description = "Número de DNI (8 dígitos) o RUC (11 dígitos) a buscar", example = "45879632", required = true)
            @RequestParam String documento,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        List<EncomiendaBusquedaDto> resultados = almacenServicio.buscarEncomiendasPorDocumento(documento, usuario);
        return ResponseEntity.ok(resultados);
    }

    // ------------------------------------------------------------------------
    // 3. Registro de Incidencia de Emergencia y Reimpresión de QR1
    // ------------------------------------------------------------------------
    @PostMapping("/incidencias")
    @PreAuthorize("hasAnyRole('RECEPCIONISTA', 'ALMACENERO', 'ADMINISTRADOR')")
    @Operation(
            summary = "Registrar incidencia de emergencia y regenerar QR1",
            description = "Reporta una incidencia sobre una etiqueta (ETIQUETA_DANADA o ETIQUETA_PERDIDA), registra la auditoría "
                    + "y regenera la imagen PNG del código QR1 operativo en Base64 para reimpresión física en ventanilla/almacén. "
                    + "El código QR2 digital permanece intacto. Rechaza la operación con 409 si la encomienda ya se encuentra ENTREGADA. "
                    + "Permitido para TN1, TN2 y TN3."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Incidencia registrada exitosamente y QR1 regenerado",
                    content = @Content(
                            schema = @Schema(implementation = RespuestaIncidenciaDto.class),
                            examples = @ExampleObject(
                                    name = "Incidencia Registrada",
                                    value = "{\"idIncidencia\":\"inc-01\",\"idEncomienda\":\"enc-01\",\"idEmpleadoReporta\":\"emp-02\",\"tipoIncidencia\":\"ETIQUETA_DANADA\",\"motivoReimpresion\":\"Etiqueta manchada con tinta\",\"fechaIncidencia\":\"2026-10-21T19:40:00\",\"qr1Base64\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de incidencia inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Encomienda o pareja Doble QR no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto: la encomienda ya fue entregada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere TN1, TN2 o TN3)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RespuestaIncidenciaDto> registrarIncidencia(
            @Valid @RequestBody RegistrarIncidenciaRequest peticion,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            HttpServletRequest request) {

        String ipOrigen = obtenerIpCliente(request);
        RespuestaIncidenciaDto respuesta = almacenServicio.registrarIncidencia(peticion, usuario, ipOrigen);
        URI location = URI.create("/api/v1/almacen/incidencias?idEncomienda=" + respuesta.getIdEncomienda());
        return ResponseEntity.created(location).body(respuesta);
    }

    // ------------------------------------------------------------------------
    // 4. Listar Incidencias Reportadas
    // ------------------------------------------------------------------------
    @GetMapping("/incidencias")
    @PreAuthorize("hasAnyRole('ALMACENERO', 'ADMINISTRADOR')")
    @Operation(
            summary = "Listar incidencias de emergencia",
            description = "Consulta las incidencias reportadas en almacén, con filtro opcional por encomienda. "
                    + "Permitido exclusivamente para Almacenero (TN2) y Administrador (TN3)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de incidencias obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = IncidenciaDetalleDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere TN2 o TN3)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<IncidenciaDetalleDto>> listarIncidencias(
            @Parameter(description = "ID de la encomienda para filtrar incidencias", example = "enc-01")
            @RequestParam(required = false) String idEncomienda) {

        List<IncidenciaDetalleDto> lista = almacenServicio.listarIncidencias(idEncomienda);
        return ResponseEntity.ok(lista);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
