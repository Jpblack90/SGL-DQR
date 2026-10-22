package pe.marvisur.sgldqr.api.seguridad;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Constantes de seguridad, roles del sistema según el Diccionario de Datos y Matriz de Permisos (RBAC).
 *
 * <p><strong>Mapeo de Roles:</strong></p>
 * <ul>
 *   <li>TN3 - Administrador: {@code ROLE_ADMINISTRADOR} (id_rol: {@code ROL-TN3-ADMIN})</li>
 *   <li>TN2 - Almacenero: {@code ROLE_ALMACENERO} (id_rol: {@code ROL-TN2-ALMACEN})</li>
 *   <li>TN1 - Recepcionista: {@code ROLE_RECEPCIONISTA} (id_rol: {@code ROL-TN1-RECEPCION})</li>
 *   <li>TN4 - Chofer: {@code ROLE_CHOFER} (id_rol: {@code ROL-TN4-CHOFER})</li>
 * </ul>
 */
public final class ConstantesSeguridad {

    private ConstantesSeguridad() {
    }

    // Nombres canónicos de roles
    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ROL_ALMACENERO = "ALMACENERO";
    public static final String ROL_RECEPCIONISTA = "RECEPCIONISTA";
    public static final String ROL_CHOFER = "CHOFER";

    // Authorities de Spring Security (prefijo ROLE_)
    public static final String AUTH_ADMINISTRADOR = "ROLE_ADMINISTRADOR";
    public static final String AUTH_ALMACENERO = "ROLE_ALMACENERO";
    public static final String AUTH_RECEPCIONISTA = "ROLE_RECEPCIONISTA";
    public static final String AUTH_CHOFER = "ROLE_CHOFER";

    // Identificadores de rol en base de datos (según V7__datos_iniciales.sql)
    public static final String ID_ROL_ADMINISTRADOR = "ROL-TN3-ADMIN";
    public static final String ID_ROL_ALMACENERO = "ROL-TN2-ALMACEN";
    public static final String ID_ROL_RECEPCIONISTA = "ROL-TN1-RECEPCION";
    public static final String ID_ROL_CHOFER = "ROL-TN4-CHOFER";

    // Matriz de permisos por rol
    public static final Set<String> PERMISOS_ADMINISTRADOR = Set.of(
            "GESTION_PERSONAL",
            "AUDITORIA_CONSULTA",
            "CONFIGURACION_SISTEMA",
            "GESTION_SUCURSALES",
            "GESTION_FLOTAS",
            "GESTION_RUTAS",
            "REPORTES_OPERATIVOS",
            "ACCESO_TOTAL"
    );

    public static final Set<String> PERMISOS_ALMACENERO = Set.of(
            "RECEPCION_ALMACEN",
            "DESPACHO_LOTE",
            "VALIDACION_CRUZADA_DOBLE_QR",
            "ENTREGA_ENCOMIENDA",
            "REGISTRO_INCIDENCIAS_ALMACEN"
    );

    public static final Set<String> PERMISOS_RECEPCIONISTA = Set.of(
            "REGISTRAR_ENCOMIENDA",
            "COBRAR_ENCOMIENDA",
            "EMISION_TICKET_QR1",
            "CONSULTAR_TRACKING",
            "REGISTRAR_CLIENTE"
    );

    public static final Set<String> PERMISOS_CHOFER = Set.of(
            "CONSULTAR_RUTA_ASIGNADA",
            "REPORTAR_SALIDA_VIAJE",
            "REPORTAR_LLEGADA_VIAJE",
            "REPORTAR_INCIDENCIA_RUTA"
    );

    public static final Map<String, Set<String>> MATRIZ_PERMISOS;

    static {
        Map<String, Set<String>> mapa = new HashMap<>();
        mapa.put(AUTH_ADMINISTRADOR, Collections.unmodifiableSet(PERMISOS_ADMINISTRADOR));
        mapa.put(AUTH_ALMACENERO, Collections.unmodifiableSet(PERMISOS_ALMACENERO));
        mapa.put(AUTH_RECEPCIONISTA, Collections.unmodifiableSet(PERMISOS_RECEPCIONISTA));
        mapa.put(AUTH_CHOFER, Collections.unmodifiableSet(PERMISOS_CHOFER));
        MATRIZ_PERMISOS = Collections.unmodifiableMap(mapa);
    }

    /**
     * Resuelve el nombre del Authority de Spring Security a partir del id_rol o nombre_rol de la base de datos.
     *
     * @param rolIdONombre id de rol o nombre textual del rol
     * @return Authority con prefijo ROLE_
     */
    public static String mapearAAuthority(String rolIdONombre) {
        if (rolIdONombre == null) {
            throw new IllegalArgumentException("El rol no puede ser nulo");
        }
        String normalizado = rolIdONombre.trim().toUpperCase();
        if (normalizado.contains("ADMIN")) {
            return AUTH_ADMINISTRADOR;
        } else if (normalizado.contains("ALMACEN")) {
            return AUTH_ALMACENERO;
        } else if (normalizado.contains("RECEPCION")) {
            return AUTH_RECEPCIONISTA;
        } else if (normalizado.contains("CHOFER")) {
            return AUTH_CHOFER;
        }
        return "ROLE_" + normalizado;
    }
}
