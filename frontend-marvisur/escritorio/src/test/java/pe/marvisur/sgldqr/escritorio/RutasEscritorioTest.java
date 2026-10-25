package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.ConfiguracionApi;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.RutasEscritorio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class RutasEscritorioTest {

    private String oldEmpaquetada;
    private String oldJpackage;
    private String oldDirDatos;
    private String oldRutaConfig;
    private String oldApiBaseUrl;
    private String oldMarvisurApiBaseUrl;

    @BeforeEach
    void guardarPropiedades() {
        oldEmpaquetada = System.getProperty(RutasEscritorio.PROP_MODO_EMPAQUETADO);
        oldJpackage = System.getProperty(RutasEscritorio.PROP_JPACKAGE_PATH);
        oldDirDatos = System.getProperty(RutasEscritorio.PROP_DIR_DATOS);
        oldRutaConfig = System.getProperty(RutasEscritorio.PROP_RUTA_CONFIG);
        oldApiBaseUrl = System.getProperty("api.base-url");
        oldMarvisurApiBaseUrl = System.getProperty("marvisur.api.base-url");

        System.clearProperty(RutasEscritorio.PROP_MODO_EMPAQUETADO);
        System.clearProperty(RutasEscritorio.PROP_JPACKAGE_PATH);
        System.clearProperty(RutasEscritorio.PROP_DIR_DATOS);
        System.clearProperty(RutasEscritorio.PROP_RUTA_CONFIG);
        System.clearProperty("api.base-url");
        System.clearProperty("marvisur.api.base-url");
    }

    @AfterEach
    void restaurarPropiedades() {
        restaurarPropiedad(RutasEscritorio.PROP_MODO_EMPAQUETADO, oldEmpaquetada);
        restaurarPropiedad(RutasEscritorio.PROP_JPACKAGE_PATH, oldJpackage);
        restaurarPropiedad(RutasEscritorio.PROP_DIR_DATOS, oldDirDatos);
        restaurarPropiedad(RutasEscritorio.PROP_RUTA_CONFIG, oldRutaConfig);
        restaurarPropiedad("api.base-url", oldApiBaseUrl);
        restaurarPropiedad("marvisur.api.base-url", oldMarvisurApiBaseUrl);
    }

    private void restaurarPropiedad(String clave, String valor) {
        if (valor != null) {
            System.setProperty(clave, valor);
        } else {
            System.clearProperty(clave);
        }
    }

    @Test
    @DisplayName("Modo desarrollo: detecta que no corre empaquetada y preserva rutas relativas")
    void testModoDesarrolloRutasPorDefecto() {
        assertThat(RutasEscritorio.esModoEmpaquetado()).isFalse();

        String dbPath = RutasEscritorio.obtenerRutaBaseDatos();
        assertThat(dbPath).endsWith("marvisur-local.db");

        Path dirSalida = RutasEscritorio.obtenerDirectorioSalida();
        assertThat(dirSalida.toString()).endsWith("salida");
    }

    @Test
    @DisplayName("Modo empaquetado: redirige SQLite y salida/ a directorio configurable / LocalAppData")
    void testModoEmpaquetadoRutasRedirigidas(@TempDir Path tempDir) {
        System.setProperty(RutasEscritorio.PROP_MODO_EMPAQUETADO, "true");
        System.setProperty(RutasEscritorio.PROP_DIR_DATOS, tempDir.toAbsolutePath().toString());

        assertThat(RutasEscritorio.esModoEmpaquetado()).isTrue();
        assertThat(RutasEscritorio.obtenerDirectorioBaseApp()).isEqualTo(tempDir);

        String rutaDb = RutasEscritorio.obtenerRutaBaseDatos();
        assertThat(rutaDb).startsWith(tempDir.toAbsolutePath().toString());
        assertThat(rutaDb).endsWith("marvisur-local.db");
        assertThat(Files.exists(tempDir.resolve("datos-locales"))).isTrue();

        Path dirSalida = RutasEscritorio.obtenerDirectorioSalida();
        assertThat(dirSalida).isEqualTo(tempDir.resolve("salida"));
        assertThat(Files.exists(dirSalida)).isTrue();
    }

    @Test
    @DisplayName("Modo empaquetado por jpackage.app-path: detecta bandera nativa de jpackage")
    void testModoEmpaquetadoPorJpackageProp(@TempDir Path tempDir) {
        Path mockExe = tempDir.resolve("SGL-DQR.exe");
        System.setProperty(RutasEscritorio.PROP_JPACKAGE_PATH, mockExe.toAbsolutePath().toString());

        assertThat(RutasEscritorio.esModoEmpaquetado()).isTrue();
    }

    @Test
    @DisplayName("Configuración externa: escritorio.properties externo sobrescribe URL de classpath")
    void testConfiguracionExternaSobrescribeClasspath(@TempDir Path tempDir) throws IOException {
        Path mockProp = tempDir.resolve("escritorio.properties");
        Files.writeString(mockProp, "api.base-url=http://servidor-produccion.marvisur.pe:8080\n");
        System.setProperty(RutasEscritorio.PROP_RUTA_CONFIG, mockProp.toAbsolutePath().toString());

        Properties props = RutasEscritorio.cargarPropiedadesCombinadas();
        assertThat(props.getProperty("api.base-url")).isEqualTo("http://servidor-produccion.marvisur.pe:8080");

        ConfiguracionApi config = new ConfiguracionApi();
        assertThat(config.getBaseUrl()).isEqualTo("http://servidor-produccion.marvisur.pe:8080");
    }

    @Test
    @DisplayName("Prioridad: propiedad de sistema -Dmarvisur.api.base-url tiene prioridad sobre archivo externo")
    void testPrioridadPropiedadSistemaSobreArchivoExterno(@TempDir Path tempDir) throws IOException {
        Path mockProp = tempDir.resolve("escritorio.properties");
        Files.writeString(mockProp, "api.base-url=http://servidor-externo:8080\n");
        System.setProperty(RutasEscritorio.PROP_RUTA_CONFIG, mockProp.toAbsolutePath().toString());

        System.setProperty("marvisur.api.base-url", "http://servidor-cli:9999");

        ConfiguracionApi config = new ConfiguracionApi();
        assertThat(config.getBaseUrl()).isEqualTo("http://servidor-cli:9999");
    }

    @Test
    @DisplayName("ConfiguracionApi: URL por defecto cuando no hay propiedades ni argumentos")
    void testUrlPorDefecto() {
        ConfiguracionApi config = new ConfiguracionApi((String) null);
        assertThat(config.getBaseUrl()).isEqualTo("http://localhost:8080");

        ConfiguracionApi configVacia = new ConfiguracionApi("   ");
        assertThat(configVacia.getBaseUrl()).isEqualTo("http://localhost:8080");

        ConfiguracionApi configConSlash = new ConfiguracionApi("http://servidor:8080/");
        assertThat(configConSlash.getBaseUrl()).isEqualTo("http://servidor:8080");
    }
}
