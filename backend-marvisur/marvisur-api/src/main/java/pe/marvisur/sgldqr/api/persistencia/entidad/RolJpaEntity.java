package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entidad JPA para la tabla 'roles'.
 */
@Entity
@Table(name = "roles")
public class RolJpaEntity {

    @Id
    @Column(name = "id_rol", length = 36, nullable = false)
    private String idRol;

    @Column(name = "nombre_rol", length = 30, nullable = false, unique = true)
    private String nombreRol;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    public RolJpaEntity() {
    }

    public RolJpaEntity(String idRol, String nombreRol, String descripcion) {
        this.idRol = idRol;
        this.nombreRol = nombreRol;
        this.descripcion = descripcion;
    }

    public String getIdRol() {
        return idRol;
    }

    public void setIdRol(String idRol) {
        this.idRol = idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolJpaEntity that = (RolJpaEntity) o;
        return Objects.equals(idRol, that.idRol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRol);
    }
}
