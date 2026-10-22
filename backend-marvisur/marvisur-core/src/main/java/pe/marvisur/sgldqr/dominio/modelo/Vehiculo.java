package pe.marvisur.sgldqr.dominio.modelo;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa un vehículo de la flota de Marvisur
 * según la tabla vehiculos del Diccionario de Datos.
 */
public class Vehiculo {

    private final String idVehiculo;
    private final String placa;
    private final String modelo;
    private final BigDecimal capacidadCargaKg;

    public Vehiculo(String idVehiculo, String placa, String modelo, BigDecimal capacidadCargaKg) {
        this.idVehiculo = Objects.requireNonNull(idVehiculo, "El idVehiculo no puede ser nulo");
        this.placa = Objects.requireNonNull(placa, "La placa no puede ser nula");
        this.modelo = Objects.requireNonNull(modelo, "El modelo no puede ser nulo");
        this.capacidadCargaKg = Objects.requireNonNull(capacidadCargaKg, "La capacidadCargaKg no puede ser nula");
    }

    public static Vehiculo crear(String placa, String modelo, BigDecimal capacidadCargaKg) {
        return new Vehiculo(UUID.randomUUID().toString(), placa, modelo, capacidadCargaKg);
    }

    public String getIdVehiculo() {
        return idVehiculo;
    }

    public String getPlaca() {
        return placa;
    }

    public String getModelo() {
        return modelo;
    }

    public BigDecimal getCapacidadCargaKg() {
        return capacidadCargaKg;
    }
}
