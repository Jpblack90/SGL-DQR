package pe.marvisur.sgldqr.dominio.modelo;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una ruta interprovincial entre sucursales de Marvisur
 * según la tabla rutas del Diccionario de Datos.
 */
public class Ruta {

    private final String idRuta;
    private final String idSucursalOrigen;
    private final String idSucursalDestino;
    private final BigDecimal distanciaKm;

    public Ruta(String idRuta, String idSucursalOrigen, String idSucursalDestino, BigDecimal distanciaKm) {
        this.idRuta = Objects.requireNonNull(idRuta, "El idRuta no puede ser nulo");
        this.idSucursalOrigen = Objects.requireNonNull(idSucursalOrigen, "El idSucursalOrigen no puede ser nulo");
        this.idSucursalDestino = Objects.requireNonNull(idSucursalDestino, "El idSucursalDestino no puede ser nulo");
        this.distanciaKm = Objects.requireNonNull(distanciaKm, "La distanciaKm no puede ser nula");
    }

    public static Ruta crear(String idSucursalOrigen, String idSucursalDestino, BigDecimal distanciaKm) {
        return new Ruta(UUID.randomUUID().toString(), idSucursalOrigen, idSucursalDestino, distanciaKm);
    }

    public String getIdRuta() {
        return idRuta;
    }

    public String getIdSucursalOrigen() {
        return idSucursalOrigen;
    }

    public String getIdSucursalDestino() {
        return idSucursalDestino;
    }

    public BigDecimal getDistanciaKm() {
        return distanciaKm;
    }
}
