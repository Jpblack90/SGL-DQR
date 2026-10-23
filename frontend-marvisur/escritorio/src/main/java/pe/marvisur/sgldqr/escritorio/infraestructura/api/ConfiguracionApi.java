package pe.marvisur.sgldqr.escritorio.infraestructura.api;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Carga y expone la configuración del cliente de escritorio.
 * Lee desde el classpath `escritorio.properties` con prioridad de reemplazo por propiedades de sistema.
 */
public final class ConfiguracionApi {

    private static final Logger LOGGER = Logger.getLogger(ConfiguracionApi.class.getName());
    private static final String ARCHIVO_CONFIG = "/escritorio.properties";
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final String baseUrl;

    public ConfiguracionApi() {
        this(cargarPropiedades());
    }

    public ConfiguracionApi(String baseUrl) {
        this.baseUrl = normalizarUrl(baseUrl);
    }

    private ConfiguracionApi(Properties props) {
        String urlProp = System.getProperty("marvisur.api.base-url", props.getProperty("api.base-url", DEFAULT_BASE_URL));
        this.baseUrl = normalizarUrl(urlProp);
    }

    private static Properties cargarPropiedades() {
        Properties props = new Properties();
        try (InputStream in = ConfiguracionApi.class.getResourceAsStream(ARCHIVO_CONFIG)) {
            if (in != null) {
                props.load(in);
            } else {
                LOGGER.info("No se encontró " + ARCHIVO_CONFIG + " en el classpath; usando valores por defecto.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar configuración desde " + ARCHIVO_CONFIG + ": " + e.getMessage(), e);
        }
        return props;
    }

    private static String normalizarUrl(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String limpia = url.trim();
        if (limpia.endsWith("/")) {
            return limpia.substring(0, limpia.length() - 1);
        }
        return limpia;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
