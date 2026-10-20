package pe.marvisur.sgldqr.dominio.modelo;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa a un cliente (remitente o destinatario) de Expreso Marvisur.
 */
public class Cliente {

    private final String idCliente;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String nombresRazonSocial;
    private String telefono;
    private String correo;

    public Cliente(String idCliente, TipoDocumento tipoDocumento, String numeroDocumento,
                   String nombresRazonSocial, String telefono, String correo) {
        this.idCliente = Objects.requireNonNull(idCliente, "El idCliente no puede ser nulo");
        this.tipoDocumento = Objects.requireNonNull(tipoDocumento, "El tipoDocumento no puede ser nulo");
        this.numeroDocumento = Objects.requireNonNull(numeroDocumento, "El numeroDocumento no puede ser nulo");
        this.nombresRazonSocial = Objects.requireNonNull(nombresRazonSocial, "El nombresRazonSocial no puede ser nulo");
        this.telefono = telefono;
        this.correo = correo;
    }

    public static Cliente crearNuevo(TipoDocumento tipoDocumento, String numeroDocumento,
                                     String nombresRazonSocial, String telefono, String correo) {
        return new Cliente(UUID.randomUUID().toString(), tipoDocumento, numeroDocumento, nombresRazonSocial, telefono, correo);
    }

    public String getIdCliente() {
        return idCliente;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getNombresRazonSocial() {
        return nombresRazonSocial;
    }

    public void setNombresRazonSocial(String nombresRazonSocial) {
        this.nombresRazonSocial = nombresRazonSocial;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return Objects.equals(idCliente, cliente.idCliente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente);
    }
}
