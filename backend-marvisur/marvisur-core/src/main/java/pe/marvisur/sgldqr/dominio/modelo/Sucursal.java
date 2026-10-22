package pe.marvisur.sgldqr.dominio.modelo;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una agencia o sucursal de Expreso Marvisur.
 */
public class Sucursal {

    private final String idSucursal;
    private String nombreAgencia;
    private String direccion;
    private String ciudad;

    public Sucursal(String idSucursal, String nombreAgencia, String direccion, String ciudad) {
        this.idSucursal = Objects.requireNonNull(idSucursal, "El idSucursal no puede ser nulo");
        this.nombreAgencia = Objects.requireNonNull(nombreAgencia, "El nombreAgencia no puede ser nulo");
        this.direccion = direccion;
        this.ciudad = Objects.requireNonNull(ciudad, "La ciudad no puede ser nula");
    }

    public static Sucursal crearNueva(String nombreAgencia, String direccion, String ciudad) {
        return new Sucursal(UUID.randomUUID().toString(), nombreAgencia, direccion, ciudad);
    }

    public String getIdSucursal() {
        return idSucursal;
    }

    public String getNombreAgencia() {
        return nombreAgencia;
    }

    public void setNombreAgencia(String nombreAgencia) {
        this.nombreAgencia = nombreAgencia;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sucursal sucursal = (Sucursal) o;
        return Objects.equals(idSucursal, sucursal.idSucursal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSucursal);
    }
}
