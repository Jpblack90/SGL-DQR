package pe.marvisur.sgldqr.dominio.modelo;

/**
 * Roles del personal interno de Expreso Marvisur según el Diccionario de Datos.
 * <p>
 * Valores exactos de la columna {@code nombre_rol}:
 * <ul>
 *   <li>{@link #ADMINISTRADOR}: Control total, auditoría y seguridad (TN3).</li>
 *   <li>{@link #ALMACENERO}: Escaneo masivo y validación cruzada en almacén (TN2).</li>
 *   <li>{@link #RECEPCIONISTA}: Registro en ventanilla y cobro (TN1).</li>
 *   <li>{@link #CHOFER}: Transporte de carga y reporte en viaje (TN4).</li>
 * </ul>
 * </p>
 */
public enum Rol {
    ADMINISTRADOR("Administrador"),
    ALMACENERO("Almacenero"),
    RECEPCIONISTA("Recepcionista"),
    CHOFER("Chofer");

    private final String nombre;

    Rol(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
