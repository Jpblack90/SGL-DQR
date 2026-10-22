package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para el resultado de búsqueda de emergencia de encomiendas por DNI/RUC en almacén.
 */
@Schema(description = "Datos de encomienda devueltos en la búsqueda de emergencia por DNI/RUC")
public class EncomiendaBusquedaDto {

    @Schema(description = "Identificador único de la encomienda", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idEncomienda;

    @Schema(description = "Código de comprobante único", example = "MV-20261021-A1B2C3D4")
    private String codigoComprobante;

    @Schema(description = "Número de documento del remitente", example = "45879632")
    private String numeroDocumentoRemitente;

    @Schema(description = "Nombres o razón social del remitente", example = "Juan Pérez Gómez")
    private String nombreRemitente;

    @Schema(description = "Número de documento del destinatario", example = "20601234567")
    private String numeroDocumentoDestinatario;

    @Schema(description = "Nombres o razón social del destinatario", example = "Comercializadora Sur SAC")
    private String nombreDestinatario;

    @Schema(description = "Sucursal de origen", example = "suc-lima-01")
    private String idSucursalOrigen;

    @Schema(description = "Sucursal de destino", example = "suc-arequipa-01")
    private String idSucursalDestino;

    @Schema(description = "Peso en kilogramos", example = "12.50")
    private BigDecimal pesoKg;

    @Schema(description = "Monto total a cobrar/pagado en soles", example = "45.00")
    private BigDecimal montoTotal;

    @Schema(description = "Estado de pago de la encomienda: PAGADO o PENDIENTE", example = "PENDIENTE")
    private String estadoPago;

    @Schema(description = "Estado logístico actual: REGISTRADO, EN_ALMACEN_ORIGEN, EN_RUTA o EN_ALMACEN_DESTINO", example = "EN_ALMACEN_DESTINO")
    private String estadoLogistico;

    @Schema(description = "Fecha de registro de la encomienda")
    private LocalDateTime fechaRegistro;

    public EncomiendaBusquedaDto() {
    }

    public EncomiendaBusquedaDto(String idEncomienda, String codigoComprobante, String numeroDocumentoRemitente,
                                 String nombreRemitente, String numeroDocumentoDestinatario, String nombreDestinatario,
                                 String idSucursalOrigen, String idSucursalDestino, BigDecimal pesoKg,
                                 BigDecimal montoTotal, String estadoPago, String estadoLogistico, LocalDateTime fechaRegistro) {
        this.idEncomienda = idEncomienda;
        this.codigoComprobante = codigoComprobante;
        this.numeroDocumentoRemitente = numeroDocumentoRemitente;
        this.nombreRemitente = nombreRemitente;
        this.numeroDocumentoDestinatario = numeroDocumentoDestinatario;
        this.nombreDestinatario = nombreDestinatario;
        this.idSucursalOrigen = idSucursalOrigen;
        this.idSucursalDestino = idSucursalDestino;
        this.pesoKg = pesoKg;
        this.montoTotal = montoTotal;
        this.estadoPago = estadoPago;
        this.estadoLogistico = estadoLogistico;
        this.fechaRegistro = fechaRegistro;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(String idEncomienda) {
        this.idEncomienda = idEncomienda;
    }

    public String getCodigoComprobante() {
        return codigoComprobante;
    }

    public void setCodigoComprobante(String codigoComprobante) {
        this.codigoComprobante = codigoComprobante;
    }

    public String getNumeroDocumentoRemitente() {
        return numeroDocumentoRemitente;
    }

    public void setNumeroDocumentoRemitente(String numeroDocumentoRemitente) {
        this.numeroDocumentoRemitente = numeroDocumentoRemitente;
    }

    public String getNombreRemitente() {
        return nombreRemitente;
    }

    public void setNombreRemitente(String nombreRemitente) {
        this.nombreRemitente = nombreRemitente;
    }

    public String getNumeroDocumentoDestinatario() {
        return numeroDocumentoDestinatario;
    }

    public void setNumeroDocumentoDestinatario(String numeroDocumentoDestinatario) {
        this.numeroDocumentoDestinatario = numeroDocumentoDestinatario;
    }

    public String getNombreDestinatario() {
        return nombreDestinatario;
    }

    public void setNombreDestinatario(String nombreDestinatario) {
        this.nombreDestinatario = nombreDestinatario;
    }

    public String getIdSucursalOrigen() {
        return idSucursalOrigen;
    }

    public void setIdSucursalOrigen(String idSucursalOrigen) {
        this.idSucursalOrigen = idSucursalOrigen;
    }

    public String getIdSucursalDestino() {
        return idSucursalDestino;
    }

    public void setIdSucursalDestino(String idSucursalDestino) {
        this.idSucursalDestino = idSucursalDestino;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public String getEstadoLogistico() {
        return estadoLogistico;
    }

    public void setEstadoLogistico(String estadoLogistico) {
        this.estadoLogistico = estadoLogistico;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
