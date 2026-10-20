package pe.marvisur.core.dominio.modelo;

/**
 * Usuario del sistema SGL-DQR con un rol específico.
 * <p>
 * La autenticación real (validación de contraseña) ocurre en el adaptador
 * de persistencia; este objeto de dominio solo representa la identidad y rol
 * del usuario dentro de las reglas de negocio.
 * </p>
 */
public class Usuario {

    // -------------------------------------------------------------------------
    // Atributos
    // -------------------------------------------------------------------------

    /** Identificador único del usuario (UUID). */
    private final String id;

    /** Nombre completo del usuario. */
    private String nombreCompleto;

    /** Rol asignado que determina los permisos en el sistema. */
    private Rol rol;

    /** UUID de la agencia a la que pertenece el usuario. */
    private String agenciaId;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Crea un nuevo usuario del sistema.
     *
     * @param id            identificador UUID
     * @param nombreCompleto nombre completo del usuario
     * @param rol           rol del usuario en el sistema
     * @param agenciaId     UUID de la agencia asignada
     */
    public Usuario(String id, String nombreCompleto, Rol rol, String agenciaId) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.agenciaId = agenciaId;
    }

    // -------------------------------------------------------------------------
    // Métodos de dominio
    // -------------------------------------------------------------------------

    /**
     * Indica si el usuario puede iniciar sesión.
     * <p>
     * La validación real de credenciales ocurre en el adaptador de seguridad.
     * Este método representa la intención del protocolo de inicio de sesión
     * dentro del dominio.
     * </p>
     *
     * @return {@code true} siempre (la lógica real vive en el adaptador)
     */
    public boolean iniciarSesion() {
        return true;
    }

    // -------------------------------------------------------------------------
    // Getters y Setters
    // -------------------------------------------------------------------------

    /** @return identificador UUID del usuario */
    public String getId() {
        return id;
    }

    /** @return nombre completo del usuario */
    public String getNombreCompleto() {
        return nombreCompleto;
    }

    /** @param nombreCompleto nuevo nombre completo */
    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    /** @return rol del usuario */
    public Rol getRol() {
        return rol;
    }

    /** @param rol nuevo rol asignado */
    public void setRol(Rol rol) {
        this.rol = rol;
    }

    /** @return UUID de la agencia asignada */
    public String getAgenciaId() {
        return agenciaId;
    }

    /** @param agenciaId nuevo UUID de agencia */
    public void setAgenciaId(String agenciaId) {
        this.agenciaId = agenciaId;
    }

    @Override
    public String toString() {
        return "Usuario{id='" + id + "', nombre='" + nombreCompleto
                + "', rol=" + rol + ", agenciaId='" + agenciaId + "'}";
    }
}
