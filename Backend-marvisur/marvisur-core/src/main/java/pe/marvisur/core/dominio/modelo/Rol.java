package pe.marvisur.core.dominio.modelo;

/**
 * Roles de usuario en el sistema SGL-DQR.
 * <p>
 * Nomenclatura TN (Tipo de Nivel):
 * <ul>
 *   <li>TN1 — Recepcionista: registra encomiendas y emite tickets QR.</li>
 *   <li>TN2 — Almacenero: valida entregas escaneando QR1 + QR2.</li>
 *   <li>TN3 — Administrador: acceso completo, reportes y configuración.</li>
 *   <li>TN4 — Chofer: consulta rutas y registra llegadas.</li>
 * </ul>
 * </p>
 */
public enum Rol {

    /** Recepcionista de agencia: registra y emite encomiendas. */
    TN1_RECEPCIONISTA,

    /** Almacenero de agencia: valida y entrega encomiendas. */
    TN2_ALMACENERO,

    /** Administrador del sistema: configuración y reportes globales. */
    TN3_ADMINISTRADOR,

    /** Chofer: transporta encomiendas y registra llegadas de ruta. */
    TN4_CHOFER
}
