package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO para la consulta y presentación de vehículos de la flota de transporte.
 */
@Schema(description = "Datos de un vehículo registrado en la flota interprovincial")
public class VehiculoDto {

    @Schema(description = "Identificador único del vehículo (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idVehiculo;

    @Schema(description = "Placa de rodaje del vehículo (5 a 10 caracteres en mayúsculas, sin espacios)", example = "V1A-852")
    private String placa;

    @Schema(description = "Modelo o descripción del vehículo", example = "Volvo FH16")
    private String modelo;

    @Schema(description = "Capacidad de carga útil en kilogramos", example = "25000.00")
    private BigDecimal capacidadCargaKg;

    public VehiculoDto() {
    }

    public VehiculoDto(String idVehiculo, String placa, String modelo, BigDecimal capacidadCargaKg) {
        this.idVehiculo = idVehiculo;
        this.placa = placa;
        this.modelo = modelo;
        this.capacidadCargaKg = capacidadCargaKg;
    }

    public String getIdVehiculo() {
        return idVehiculo;
    }

    public void setIdVehiculo(String idVehiculo) {
        this.idVehiculo = idVehiculo;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public BigDecimal getCapacidadCargaKg() {
        return capacidadCargaKg;
    }

    public void setCapacidadCargaKg(BigDecimal capacidadCargaKg) {
        this.capacidadCargaKg = capacidadCargaKg;
    }
}
