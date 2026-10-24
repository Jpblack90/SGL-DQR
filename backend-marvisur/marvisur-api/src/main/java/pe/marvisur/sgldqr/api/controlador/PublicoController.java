package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.BilleteraRequest;
import pe.marvisur.sgldqr.api.dto.CotizacionRequest;
import pe.marvisur.sgldqr.api.dto.CotizacionResponse;
import pe.marvisur.sgldqr.api.dto.CrearPreRegistroPublicoRequest;
import pe.marvisur.sgldqr.api.dto.RastreoPublicoDto;
import pe.marvisur.sgldqr.api.dto.RespuestaBilleteraDto;
import pe.marvisur.sgldqr.api.dto.RespuestaPreRegistroPublicoDto;
import pe.marvisur.sgldqr.api.dto.SucursalPublicaDto;
import pe.marvisur.sgldqr.api.servicio.ComprobanteServicio;
import pe.marvisur.sgldqr.api.servicio.PublicoServicio;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Controlador de endpoints públicos para el portal web de clientes (RF-08, RF-09, RF-10).
 *
 * <p><strong>Fricción Cero y Seguridad:</strong>
 * Estos endpoints no requieren cabecera {@code Authorization}. Cuentan con protección por
 * limitación de tasa por IP en memoria y devuelven información anonimizada o restringida
 * para resguardar la privacidad del cliente.</p>
 */
@RestController
@RequestMapping("/api/v1/publico")
@Tag(name = "Portal Web Público", description = "Endpoints de autoservicio sin autenticación para clientes finales")
public class PublicoController {

    private final PublicoServicio publicoServicio;
    private final ComprobanteServicio comprobanteServicio;

    public PublicoController(PublicoServicio publicoServicio, ComprobanteServicio comprobanteServicio) {
        this.publicoServicio = Objects.requireNonNull(publicoServicio, "El publicoServicio no puede ser nulo");
        this.comprobanteServicio = Objects.requireNonNull(comprobanteServicio, "El comprobanteServicio no puede ser nulo");
    }

    @GetMapping("/ping")
    @Operation(summary = "Comprobación de conectividad pública", description = "Verifica que los endpoints /api/v1/publico/** no exigen token JWT.")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of(
                "estado", "OK",
                "mensaje", "Endpoint público accesible sin token"
        ));
    }

    @GetMapping("/sucursales")
    @Operation(summary = "Listar agencias públicas", description = "Retorna el listado de agencias activas para alimentar los selectores de origen y destino del portal web.")
    @ApiResponse(responseCode = "200", description = "Listado de sucursales obtenido exitosamente")
    public ResponseEntity<List<SucursalPublicaDto>> listarSucursales() {
        return ResponseEntity.ok(publicoServicio.listarSucursales());
    }

    @GetMapping("/rastreo")
    @Operation(summary = "Rastreo público de encomiendas", description = "Consulta la línea de tiempo y estado de una encomienda autorizada mediante el DNI/RUC del remitente o destinatario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos de rastreo obtenidos"),
            @ApiResponse(responseCode = "400", description = "Parámetros obligatorios ausentes"),
            @ApiResponse(responseCode = "404", description = "Encomienda no encontrada o documento no coincide")
    })
    public ResponseEntity<RastreoPublicoDto> consultarRastreo(
            @Parameter(description = "Código de boleta o comprobante", required = true)
            @RequestParam(name = "comprobante") String comprobante,
            @Parameter(description = "Número de documento (DNI o RUC) del remitente o destinatario", required = true)
            @RequestParam(name = "documento") String documento) {
        return ResponseEntity.ok(publicoServicio.consultarRastreo(comprobante, documento));
    }

    @PostMapping("/cotizacion")
    @Operation(summary = "Cotización estimada de tarifa", description = "Calcula el costo aproximado de envío en soles basado en el peso físico oficial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cotización calculada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de cotización inválidos")
    })
    public ResponseEntity<CotizacionResponse> cotizar(
            @Valid @RequestBody CotizacionRequest request) {
        return ResponseEntity.ok(publicoServicio.cotizar(request));
    }

    @PostMapping("/pre-registros")
    @Operation(summary = "Crear pre-registro express", description = "Genera un código temporal legible ('Código Express') para acelerar el depósito de encomienda en ventanilla.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pre-registro generado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de remitente, destinatario o peso inválidos")
    })
    public ResponseEntity<RespuestaPreRegistroPublicoDto> crearPreRegistro(
            @Valid @RequestBody CrearPreRegistroPublicoRequest request) {
        RespuestaPreRegistroPublicoDto respuesta = publicoServicio.crearPreRegistro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/billetera")
    @Operation(summary = "Consultar Billetera Digital", description = "Recibe el token de retiro en el cuerpo para consultar el estatus y generar la llave digital QR2 al vuelo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos de la billetera y llave QR2 generada"),
            @ApiResponse(responseCode = "400", description = "Token ausente o inválido"),
            @ApiResponse(responseCode = "404", description = "Llave digital inexistente o no encontrada")
    })
    public ResponseEntity<RespuestaBilleteraDto> consultarBilletera(
            @Valid @RequestBody BilleteraRequest request) {
        return ResponseEntity.ok(publicoServicio.consultarBilletera(request));
    }

    @GetMapping(value = "/comprobante.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Descarga pública de comprobante de envío en PDF",
            description = "Descarga el comprobante de control interno en PDF (RF-11). Requiere comprobante y número de documento válido de remitente o destinatario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprobante PDF obtenido exitosamente",
                    content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Parámetros obligatorios ausentes"),
            @ApiResponse(responseCode = "404", description = "Comprobante no encontrado o documento no coincide")
    })
    public ResponseEntity<byte[]> descargarComprobantePdf(
            @Parameter(description = "Código de boleta o comprobante", required = true)
            @RequestParam(name = "comprobante") String comprobante,
            @Parameter(description = "Número de documento (DNI o RUC) del remitente o destinatario", required = true)
            @RequestParam(name = "documento") String documento) {
        ComprobanteServicio.ResultadoPdfComprobante resultado = comprobanteServicio.obtenerOgenerarPdfPublico(comprobante, documento);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resultado.nombreArchivo() + "\"")
                .body(resultado.contenidoPdf());
    }
}
