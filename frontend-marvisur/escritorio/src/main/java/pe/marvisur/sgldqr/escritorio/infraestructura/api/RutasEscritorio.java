package pe.marvisur.sgldqr.escritorio.infraestructura.api;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor central de rutas y configuración de entorno para la aplicación de escritorio.
 * <p>
 * Resuelve transparentemente las rutas según el entorno de ejecución:
 * <ul>
 *     <li><b>Modo desarrollo (Maven / IDE):</b> Preserva las rutas relativas actuales
 *     ({@code datos-locales/marvisur-local.db} y {@code salida/}).</li>
 *     <li><b>Modo empaquetado (jpackage / EXE):</b> Redirige almacenamiento modificable
 *     a {@code %LOCALAPPDATA%\SGL-DQR\} para evitar fallos de permisos en directorios de instalación
 *     de solo lectura (como {@code Program Files}).</li>
 * </ul>
 * </p>
 */
public final class RutasEscritorio {

    private static final Logger LOGGER = Logger.getLogger(RutasEscritorio.class.getName());

    public static final String NOMBRE_APP = "SGL-DQR";
    public static final String ARCHIVO_CONFIG = "escritorio.properties";
    public static final String ARCHIVO_DB_LOCAL = "marvisur-local.db";
    public static final String SUBDIR_DATOS = "datos-locales";
    public static final String SUBDIR_SALIDA = "salida";

    public static final String PROP_JPACKAGE_PATH = "jpackage.app-path";
    public static final String PROP_MODO_EMPAQUETADO = "app.empaquetada";
    public static final String PROP_DIR_DATOS = "app.dir.datos";
    public static final String PROP_RUTA_CONFIG = "app.ruta.config";

    private RutasEscritorio() {
    }

    /**
     * Determina si la aplicación corre empaquetada mediante jpackage o con la bandera correspondiente.
     */
    public static boolean esModoEmpaquetado() {
        return Boolean.getBoolean(PROP_MODO_EMPAQUETADO)
                || System.getProperty(PROP_JPACKAGE_PATH) != null;
    }

    /**
     * Obtiene el directorio base de datos para la aplicación.
     * En modo empaquetado: %LOCALAPPDATA%\SGL-DQR (o ~/.sgl-dqr como fallback).
     * En modo desarrollo: directorio actual de trabajo.
     */
    public static Path obtenerDirectorioBaseApp() {
        String dirForzado = System.getProperty(PROP_DIR_DATOS);
        if (dirForzado != null && !dirForzado.isBlank()) {
            return Paths.get(dirForzado.trim());
        }

        if (esModoEmpaquetado()) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                return Paths.get(localAppData, NOMBRE_APP);
            }
            String userHome = System.getProperty("user.home", ".");
            return Paths.get(userHome, ".sgl-dqr");
        }

        return Paths.get(".");
    }

    /**
     * Obtiene la ruta absoluta o relativa al archivo de base de datos SQLite.
     */
    public static String obtenerRutaBaseDatos() {
        if (esModoEmpaquetado()) {
            Path base = obtenerDirectorioBaseApp();
            Path dirDatos = base.resolve(SUBDIR_DATOS);
            asegurarDirectorio(dirDatos);
            return dirDatos.resolve(ARCHIVO_DB_LOCAL).toAbsolutePath().toString();
        }

        // Modo desarrollo / Maven
        if (Files.exists(Paths.get("frontend-marvisur/escritorio/datos-locales/marvisur-local.db"))) {
            return "frontend-marvisur/escritorio/datos-locales/marvisur-local.db";
        }
        return SUBDIR_DATOS + "/" + ARCHIVO_DB_LOCAL;
    }

    /**
     * Obtiene la ruta al directorio de salida (tickets simulados PNG, reportes CSV de lotes).
     */
    public static Path obtenerDirectorioSalida() {
        Path dirSalida;
        if (esModoEmpaquetado()) {
            dirSalida = obtenerDirectorioBaseApp().resolve(SUBDIR_SALIDA);
        } else {
            // Modo desarrollo: respeta si se ejecuta desde raíz del proyecto multimódulo
            if (Files.isDirectory(Paths.get("frontend-marvisur/escritorio"))) {
                dirSalida = Paths.get("frontend-marvisur/escritorio", SUBDIR_SALIDA);
            } else {
                dirSalida = Paths.get(SUBDIR_SALIDA);
            }
        }
        asegurarDirectorio(dirSalida);
        return dirSalida;
    }

    /**
     * Resuelve el archivo escritorio.properties externo si existe junto al ejecutable o en el directorio de trabajo.
     */
    public static Path resolverArchivoConfiguracionExterna() {
        String rutaForzada = System.getProperty(PROP_RUTA_CONFIG);
        if (rutaForzada != null && !rutaForzada.isBlank()) {
            Path p = Paths.get(rutaForzada.trim());
            if (Files.isRegularFile(p)) {
                return p;
            }
        }

        // 1. Directorio del ejecutable generado por jpackage
        String appPath = System.getProperty(PROP_JPACKAGE_PATH);
        if (appPath != null && !appPath.isBlank()) {
            try {
                Path dirExe = Paths.get(appPath).toAbsolutePath().getParent();
                if (dirExe != null) {
                    Path enDirExe = dirExe.resolve(ARCHIVO_CONFIG);
                    if (Files.isRegularFile(enDirExe)) {
                        return enDirExe;
                    }
                    Path enDirApp = dirExe.resolve("app").resolve(ARCHIVO_CONFIG);
                    if (Files.isRegularFile(enDirApp)) {
                        return enDirApp;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "No se pudo resolver directorio del ejecutable: " + e.getMessage());
            }
        }

        // 2. En el directorio de trabajo actual
        Path enWorkingDir = Paths.get(ARCHIVO_CONFIG);
        if (Files.isRegularFile(enWorkingDir)) {
            return enWorkingDir;
        }

        // 3. En la raíz del módulo escritorio si se corre desde la raíz del proyecto
        Path enModulo = Paths.get("frontend-marvisur/escritorio", ARCHIVO_CONFIG);
        if (Files.isRegularFile(enModulo)) {
            return enModulo;
        }

        return null;
    }

    /**
     * Carga propiedades combinando classpath y archivo externo si existe.
     */
    public static Properties cargarPropiedadesCombinadas() {
        Properties props = new Properties();

        // 1. Cargar del classpath
        try (InputStream in = RutasEscritorio.class.getResourceAsStream("/" + ARCHIVO_CONFIG)) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al leer " + ARCHIVO_CONFIG + " del classpath: " + e.getMessage());
        }

        // 2. Sobrescribir con archivo externo si existe
        Path archivoExt = resolverArchivoConfiguracionExterna();
        if (archivoExt != null) {
            try (InputStream in = Files.newInputStream(archivoExt)) {
                props.load(in);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al leer configuración externa desde " + archivoExt + ": " + e.getMessage());
            }
        }

        return props;
    }

    private static void asegurarDirectorio(Path dir) {
        try {
            if (dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo asegurar directorio: " + dir + " -> " + e.getMessage());
        }
    }
}
