package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.util.Set;

/**
 * Define la matriz de permisos de navegación del menú lateral según el rol del empleado.
 * <p>
 * Roles:
 * - TN1 Recepcionista: Ventanilla, Contingencia
 * - TN2 Almacenero: Almacén (Entrega, Lotes, Contingencia)
 * - TN3 Administrador: Ventanilla, Almacén, Contingencia, Dashboard, Empleados
 * - TN4 Chofer: Rutas
 * </p>
 * En la versión actual (Fase D1 + D2 + D3 + D4), los módulos Ventanilla, Almacén, Contingencia y Dashboard están operativos.
 * están plenamente operativos. Dashboard, Empleados y Rutas siguen marcados como 'Próximamente'.
 */
public final class ModeloMenuRol {

    public enum ModuloMenu {
        VENTANILLA("Ventanilla", "Registro y admisión con Doble QR", true),
        ALMACEN("Almacén", "Validación cruzada Doble QR, entrega física y lotes", true),
        CONTINGENCIA("Contingencia", "Búsqueda DNI/RUC, cobro e incidencias de etiquetas", true),
        COLA_LOCAL("Cola Local", "Gestión de registros sin conexión y sincronización", true),
        DASHBOARD("Dashboard", "KPIs de volumen, tiempos y recaudación", true),
        EMPLEADOS("Empleados", "Gestión de personal y auditoría", true),
        RUTAS("Rutas", "Programación de viajes y asignación de flotas", false);

        private final String titulo;
        private final String descripcion;
        private final boolean implementado;

        ModuloMenu(String titulo, String descripcion, boolean implementado) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.implementado = implementado;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public boolean isImplementado() {
            return implementado;
        }
    }

    private ModeloMenuRol() {
    }

    /**
     * Normaliza el rol recibido (soportando variantes con prefijo ROLE_ o código TNx).
     */
    public static String normalizarRol(String rol) {
        if (rol == null) return "";
        String r = rol.trim().toUpperCase();
        if (r.startsWith("ROLE_")) {
            r = r.substring(5);
        }
        return switch (r) {
            case "RECEPCIONISTA", "TN1", "TN1_RECEPCIONISTA" -> "TN1";
            case "ALMACENERO", "TN2", "TN2_ALMACENERO" -> "TN2";
            case "ADMINISTRADOR", "ADMIN", "TN3", "TN3_ADMINISTRADOR" -> "TN3";
            case "CHOFER", "TN4", "TN4_CHOFER" -> "TN4";
            default -> r;
        };
    }

    /**
     * Indica si un módulo está autorizado para el rol del usuario.
     */
    public static boolean estaAutorizado(String rol, ModuloMenu modulo) {
        String rolNorm = normalizarRol(rol);
        return switch (modulo) {
            case VENTANILLA, COLA_LOCAL -> Set.of("TN1", "TN3").contains(rolNorm);
            case ALMACEN -> Set.of("TN2", "TN3").contains(rolNorm);
            case CONTINGENCIA -> Set.of("TN1", "TN2", "TN3").contains(rolNorm);
            case DASHBOARD, EMPLEADOS -> Set.of("TN3").contains(rolNorm);
            case RUTAS -> Set.of("TN4", "TN3").contains(rolNorm);
        };
    }

    /**
     * Indica si el botón debe estar habilitado funcionalmente para hacer click (autorizado y ya implementado).
     */
    public static boolean estaHabilitado(String rol, ModuloMenu modulo) {
        return estaAutorizado(rol, modulo) && modulo.isImplementado();
    }
}
