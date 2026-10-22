package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entidad JPA para la tabla 'empleados'.
 */
@Entity
@Table(name = "empleados")
public class EmpleadoJpaEntity {

    @Id
    @Column(name = "id_empleado", length = 36, nullable = false)
    private String idEmpleado;

    @Column(name = "id_rol", length = 36, nullable = false)
    private String idRol;

    @Column(name = "id_sucursal", length = 36, nullable = false)
    private String idSucursal;

    @Column(name = "dni", length = 8, nullable = false, unique = true)
    private String dni;

    @Column(name = "nombres", length = 80, nullable = false)
    private String nombres;

    @Column(name = "apellidos", length = 80, nullable = false)
    private String apellidos;

    @Column(name = "correo", length = 120, nullable = false, unique = true)
    private String correo;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    public EmpleadoJpaEntity() {
    }

    public EmpleadoJpaEntity(String idEmpleado, String idRol, String idSucursal, String dni,
                             String nombres, String apellidos, String correo, String passwordHash, Boolean estado) {
        this.idEmpleado = idEmpleado;
        this.idRol = idRol;
        this.idSucursal = idSucursal;
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.estado = estado;
    }

    public String getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(String idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public String getIdRol() {
        return idRol;
    }

    public void setIdRol(String idRol) {
        this.idRol = idRol;
    }

    public String getIdSucursal() {
        return idSucursal;
    }

    public void setIdSucursal(String idSucursal) {
        this.idSucursal = idSucursal;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmpleadoJpaEntity that = (EmpleadoJpaEntity) o;
        return Objects.equals(idEmpleado, that.idEmpleado);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEmpleado);
    }
}
