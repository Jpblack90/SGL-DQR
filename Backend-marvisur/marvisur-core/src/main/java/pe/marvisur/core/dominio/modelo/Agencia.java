package pe.marvisur.core.dominio.modelo;

/**
 * Agencia de Expreso Marvisur (origen o destino de encomiendas).
 * <p>
 * Actúa como punto de entrega y recepción de paquetes dentro de la red logística.
 * El campo {@code id} es un UUID generado en el cliente (arquitectura offline-first).
 * </p>
 */
public class Agencia {

    // -------------------------------------------------------------------------
    // Atributos
    // -------------------------------------------------------------------------

    /** Identificador único de la agencia (UUID). */
    private final String id;

    /** Código interno de la agencia (ej. "AYA-01", "ICA-02"). */
    private String codigo;

    /** Nombre comercial de la agencia. */
    private String nombre;

    /** Dirección física de la agencia. */
    private String direccion;

    /** Ciudad donde se ubica la agencia. */
    private String ciudad;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Crea una nueva agencia con todos sus datos.
     *
     * @param id        identificador UUID
     * @param codigo    código interno de la agencia
     * @param nombre    nombre de la agencia
     * @param direccion dirección física
     * @param ciudad    ciudad de ubicación
     */
    public Agencia(String id, String codigo, String nombre, String direccion, String ciudad) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
        this.ciudad = ciudad;
    }

    // -------------------------------------------------------------------------
    // Getters y Setters
    // -------------------------------------------------------------------------

    /** @return identificador UUID de la agencia */
    public String getId() {
        return id;
    }

    /** @return código interno de la agencia */
    public String getCodigo() {
        return codigo;
    }

    /** @param codigo nuevo código interno */
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    /** @return nombre de la agencia */
    public String getNombre() {
        return nombre;
    }

    /** @param nombre nuevo nombre de la agencia */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** @return dirección física de la agencia */
    public String getDireccion() {
        return direccion;
    }

    /** @param direccion nueva dirección */
    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    /** @return ciudad donde se ubica la agencia */
    public String getCiudad() {
        return ciudad;
    }

    /** @param ciudad nueva ciudad */
    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    @Override
    public String toString() {
        return "Agencia{id='" + id + "', codigo='" + codigo
                + "', nombre='" + nombre + "', ciudad='" + ciudad + "'}";
    }
}
