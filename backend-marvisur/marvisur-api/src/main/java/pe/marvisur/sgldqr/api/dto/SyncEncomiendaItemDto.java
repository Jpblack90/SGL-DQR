package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.marvisur.sgldqr.aplicacion.ModalidadPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos de una encomienda registrada localmente sin conexión para sincronización con el servidor central.
 */
@Schema(description = "Datos de una encomienda registrada offline para sincronización central")
public class SyncEncomiendaItemDto {

    @Schema(description = "Identificador único de la encomienda generado offline (UUID)", example = "7b92f4e8-469b-43d7-a5c9-123456789abc")
    @NotBlank(message = "El idEncomienda es obligatorio")
    private String idEncomienda;

    @Schema(description = "Código de comprobante asignado en ventanilla offline", example = "MV-20261022-3A7MLMKY")
    @NotBlank(message = "El codigoComprobante es obligatorio")
    private String codigoComprobante;

    @Schema(description = "Datos del cliente remitente")
    @NotNull(message = "El remitente es obligatorio")
    @Valid
    private ClienteInputDto remitente;

    @Schema(description = "Datos del cliente destinatario")
    @NotNull(message = "El destinatario es obligatorio")
    @Valid
    private ClienteInputDto destinatario;

    @Schema(description = "Identificador de la sucursal de origen", example = "SUC-LIM-01")
    @NotBlank(message = "El idSucursalOrigen es obligatorio")
    private String idSucursalOrigen;

    @Schema(description = "Identificador de la sucursal de destino", example = "SUC-HYO-01")
    @NotBlank(message = "El idSucursalDestino es obligatorio")
    private String idSucursalDestino;

    @Schema(description = "Peso del paquete en kilogramos (debe ser mayor a 0)", example = "5.50")
    @NotNull(message = "El pesoKg es obligatorio")
    @DecimalMin(value = "0.01", message = "El pesoKg debe ser mayor a cero")
    private BigDecimal pesoKg;

    @Schema(description = "Volumen estimado del paquete en metros cúbicos (opcional)", example = "0.0250")
    private BigDecimal volumenM3;

    @Schema(description = "Descripción o contenido declarado de la encomienda", example = "Repuestos mecánicos y herramientas")
    private String descripcionContenido;

    @Schema(description = "Modalidad de pago convenida", example = "CONTADO")
    @NotNull(message = "La modalidadPago es obligatoria")
    private ModalidadPago modalidadPago;

    @Schema(description = "Monto total cobrado o pactado localmente en soles", example = "25.00")
    private BigDecimal montoTotal;

    @Schema(description = "Fecha y hora en que se registró la encomienda en el cliente local", example = "2026-10-22T10:15:30")
    @NotNull(message = "La fechaRegistro es obligatoria")
    private LocalDateTime fechaRegistro;

    @Schema(description = "Datos del par criptográfico Doble QR generado offline")
    @NotNull(message = "El objeto qr es obligatorio")
    @Valid
    private QrSyncDto qr;

    public SyncEncomiendaItemDto() {
    }

    public SyncEncomiendaItemDto(String idEncomienda, String codigoComprobante, ClienteInputDto remitente,
                                 ClienteInputDto destinatario, String idSucursalOrigen, String idSucursalDestino,
                                 BigDecimal pesoKg, BigDecimal volumenM3, String descripcionContenido,
                                 ModalidadPago modalidadPago, BigDecimal montoTotal, LocalDateTime fechaRegistro,
                                 QrSyncDto qr) {
        this.idEncomienda = idEncomienda;
        this.codigoComprobante = codigoComprobante;
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.idSucursalOrigen = idSucursalOrigen;
        this.idSucursalDestino = idSucursalDestino;
        this.pesoKg = pesoKg;
        this.volumenM3 = volumenM3;
        this.descripcionContenido = descripcionContenido;
        this.modalidadPago = modalidadPago;
        this.montoTotal = montoTotal;
        this.fechaRegistro = fechaRegistro;
        this.qr = qr;
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

    public ClienteInputDto getRemitente() {
        return remitente;
    }

    public void setRemitente(ClienteInputDto remitente) {
        this.remitente = remitente;
    }

    public ClienteInputDto getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(ClienteInputDto destinatario) {
        this.destinatario = destinatario;
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

    public BigDecimal getVolumenM3() {
        return volumenM3;
    }

    public void setVolumenM3(BigDecimal volumenM3) {
        this.volumenM3 = volumenM3;
    }

    public String getDescripcionContenido() {
        return descripcionContenido;
    }

    public void setDescripcionContenido(String descripcionContenido) {
        this.descripcionContenido = descripcionContenido;
    }

    public ModalidadPago getModalidadPago() {
        return modalidadPago;
    }

    public void setModalidadPago(ModalidadPago modalidadPago) {
        this.modalidadPago = modalidadPago;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public QrSyncDto getQr() {
        return qr;
    }

    public void setQr(QrSyncDto qr) {
        this.qr = qr;
    }

    @Override
    public String toString() {
        return "SyncEncomiendaItemDto{" +
                "idEncomienda='" + idEncomienda + '\'' +
                ", codigoComprobante='" + codigoComprobante + '\'' +
                ", idSucursalOrigen='" + idSucursalOrigen + '\'' +
                ", idSucursalDestino='" + idSucursalDestino + '\'' +
                ", pesoKg=" + pesoKg +
                ", modalidadPago=" + modalidadPago +
                ", montoTotal=" + montoTotal +
                '}';
    }
}
