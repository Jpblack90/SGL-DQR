package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entidad JPA para la tabla 'sucursales'.
 */
@Entity
@Table(name = "sucursales")
public class SucursalJpaEntity {

    @Id
    @Column(name = "id_sucursal", length = 36, nullable = false)
    private String idSucursal;

    @Column(name = "nombre_agencia", length = 100, nullable = false)
    private String nombreAgencia;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "ciudad", length = 80, nullable = false)
    private String ciudad;

    public SucursalJpaEntity() {
    }

    public SucursalJpaEntity(String idSucursal, String nombreAgencia, String direccion, String ciudad) {
        this.idSucursal = idSucursal;
        this.nombreAgencia = nombreAgencia;
        this.direccion = direccion;
        this.ciudad = ciudad;
    }

    public String getIdSucursal() {
        return idSucursal;
    }

    public void setIdSucursal(String idSucursal) {
        this.idSucursal = idSucursal;
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
        SucursalJpaEntity that = (SucursalJpaEntity) o;
        return Objects.equals(idSucursal, that.idSucursal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSucursal);
    }
}
