package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para la consulta y selección de viajes interprovinciales programados.
 */
@Schema(description = "Datos de un viaje programado para asignación y despacho de lotes")
public class ViajeDto {

    @Schema(description = "Identificador único del viaje", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idViaje;

    @Schema(description = "Identificador de la ruta asociada", example = "ruta-lima-arequipa")
    private String idRuta;

    @Schema(description = "Identificador de la sucursal de origen de la ruta", example = "suc-lima-01")
    private String idSucursalOrigen;

    @Schema(description = "Identificador de la sucursal de destino de la ruta", example = "suc-arequipa-01")
    private String idSucursalDestino;

    @Schema(description = "Distancia en kilómetros de la ruta", example = "1010.50")
    private BigDecimal distanciaKm;

    @Schema(description = "Identificador del vehículo asignado", example = "veh-01")
    private String idVehiculo;

    @Schema(description = "Placa del vehículo", example = "V1A-852")
    private String placaVehiculo;

    @Schema(description = "Identificador del chofer asignado", example = "emp-chofer-01")
    private String idChofer;

    @Schema(description = "Nombre completo del chofer", example = "Carlos Quispe Ramos")
    private String nombreChofer;

    @Schema(description = "Fecha y hora estimada de salida")
    private LocalDateTime fechaSalida;

    @Schema(description = "Fecha y hora estimada de llegada")
    private LocalDateTime fechaLlegadaEstimada;

    @Schema(description = "Estado actual del viaje", example = "PROGRAMADO")
    private EstadoViaje estadoViaje;

    @Schema(description = "Datos detallados de la ruta asociada")
    private RutaDto ruta;

    @Schema(description = "Placa del vehículo", example = "V1A-852")
    private String placa;

    @Schema(description = "Modelo del vehículo", example = "Volvo FH16")
    private String modelo;

    public ViajeDto() {
    }

    public ViajeDto(String idViaje, String idRuta, String idSucursalOrigen, String idSucursalDestino,
                    BigDecimal distanciaKm, String idVehiculo, String placaVehiculo, String idChofer,
                    String nombreChofer, LocalDateTime fechaSalida, LocalDateTime fechaLlegadaEstimada,
                    EstadoViaje estadoViaje) {
        this.idViaje = idViaje;
        this.idRuta = idRuta;
        this.idSucursalOrigen = idSucursalOrigen;
        this.idSucursalDestino = idSucursalDestino;
        this.distanciaKm = distanciaKm;
        this.idVehiculo = idVehiculo;
        this.placaVehiculo = placaVehiculo;
        this.placa = placaVehiculo;
        this.idChofer = idChofer;
        this.nombreChofer = nombreChofer;
        this.fechaSalida = fechaSalida;
        this.fechaLlegadaEstimada = fechaLlegadaEstimada;
        this.estadoViaje = estadoViaje;
    }

    public ViajeDto(String idViaje, String idRuta, String idSucursalOrigen, String idSucursalDestino,
                    BigDecimal distanciaKm, String idVehiculo, String placaVehiculo, String idChofer,
                    String nombreChofer, LocalDateTime fechaSalida, LocalDateTime fechaLlegadaEstimada,
                    EstadoViaje estadoViaje, RutaDto ruta, String placa, String modelo) {
        this(idViaje, idRuta, idSucursalOrigen, idSucursalDestino, distanciaKm, idVehiculo, placaVehiculo,
                idChofer, nombreChofer, fechaSalida, fechaLlegadaEstimada, estadoViaje);
        this.ruta = ruta;
        this.placa = placa != null ? placa : placaVehiculo;
        this.modelo = modelo;
    }

    public String getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(String idViaje) {
        this.idViaje = idViaje;
    }

    public String getIdRuta() {
        return idRuta;
    }

    public void setIdRuta(String idRuta) {
        this.idRuta = idRuta;
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

    public BigDecimal getDistanciaKm() {
        return distanciaKm;
    }

    public void setDistanciaKm(BigDecimal distanciaKm) {
        this.distanciaKm = distanciaKm;
    }

    public String getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(String idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public String getPlacaVehiculo() {
        return placaVehiculo;
    }

    public void setPlacaVehiculo(String placaVehiculo) {
        this.placaVehiculo = placaVehiculo;
    }

    public String getIdChofer() {
        return idChofer;
    }

    public void setIdChofer(String idChofer) {
        this.idChofer = idChofer;
    }

    public String getNombreChofer() {
        return nombreChofer;
    }

    public void setNombreChofer(String nombreChofer) {
        this.nombreChofer = nombreChofer;
    }

    public LocalDateTime getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDateTime fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public LocalDateTime getFechaLlegadaEstimada() {
        return fechaLlegadaEstimada;
    }

    public void setFechaLlegadaEstimada(LocalDateTime fechaLlegadaEstimada) {
        this.fechaLlegadaEstimada = fechaLlegadaEstimada;
    }

    public EstadoViaje getEstadoViaje() {
        return estadoViaje;
    }

    public void setEstadoViaje(EstadoViaje estadoViaje) {
        this.estadoViaje = estadoViaje;
    }

    public RutaDto getRuta() {
        return ruta;
    }

    public void setRuta(RutaDto ruta) {
        this.ruta = ruta;
    }

    public String getPlaca() {
        return placa != null ? placa : placaVehiculo;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
        if (this.placaVehiculo == null) {
            this.placaVehiculo = placa;
        }
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }
}
