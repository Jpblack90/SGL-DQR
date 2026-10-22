package pe.marvisur.sgldqr.dominio.modelo;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa a un colaborador interno de Expreso Marvisur.
 */
public class Empleado {

    private final String idEmpleado;
    private String idRol;
    private String idSucursal;
    private String dni;
    private String nombres;
    private String apellidos;
    private String correo;
    private String passwordHash;
    private boolean estado;

    public Empleado(String idEmpleado, String idRol, String idSucursal, String dni,
                    String nombres, String apellidos, String correo, String passwordHash, boolean estado) {
        this.idEmpleado = Objects.requireNonNull(idEmpleado, "El idEmpleado no puede ser nulo");
        this.idRol = Objects.requireNonNull(idRol, "El idRol no puede ser nulo");
        this.idSucursal = Objects.requireNonNull(idSucursal, "El idSucursal no puede ser nulo");
        this.dni = Objects.requireNonNull(dni, "El dni no puede ser nulo");
        this.nombres = Objects.requireNonNull(nombres, "Los nombres no pueden ser nulos");
        this.apellidos = Objects.requireNonNull(apellidos, "Los apellidos no pueden ser nulos");
        this.correo = Objects.requireNonNull(correo, "El correo no puede ser nulo");
        this.passwordHash = Objects.requireNonNull(passwordHash, "El passwordHash no puede ser nulo");
        this.estado = estado;
    }

    public static Empleado crearNuevo(String idRol, String idSucursal, String dni,
                                      String nombres, String apellidos, String correo, String passwordHash) {
        return new Empleado(UUID.randomUUID().toString(), idRol, idSucursal, dni, nombres, apellidos, correo, passwordHash, true);
    }

    public String getIdEmpleado() {
        return idEmpleado;
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

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    public String getNombreCompleto() {
        return this.nombres + " " + this.apellidos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Empleado empleado = (Empleado) o;
        return Objects.equals(idEmpleado, empleado.idEmpleado);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEmpleado);
    }
}
