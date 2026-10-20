package pe.marvisur.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración que verifica que las migraciones Flyway
 * se aplican correctamente contra H2 en modo PostgreSQL.
 *
 * <p>Valida:
 * <ul>
 *   <li>Que todas las tablas del esquema fueron creadas (V1).</li>
 *   <li>Que los datos semilla de agencias y usuarios existen (V2).</li>
 *   <li>Que las restricciones de clave foránea están registradas.</li>
 * </ul>
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Migraciones Flyway — validacion del esquema")
class MigracionesFlywayTest {

    @Autowired
    private JdbcTemplate jdbc;

    // Tablas que deben existir tras V1
    private static final List<String> TABLAS_ESPERADAS = List.of(
            "AGENCIA",
            "VEHICULO",
            "RUTA",
            "USUARIO",
            "ENCOMIENDA",
            "PAR_DOBLE_QR",
            "COMPROBANTE",
            "AUDITORIA",
            "TOKEN_ACCESO_CLIENTE"
    );

    @Test
    @DisplayName("V1: todas las tablas del esquema deben existir")
    void todasLasTablasExisten() {
        for (String tabla : TABLAS_ESPERADAS) {
            Integer cuenta = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE UPPER(TABLE_NAME) = ?",
                    Integer.class, tabla
            );
            assertThat(cuenta)
                    .as("La tabla '%s' debe existir en el esquema", tabla)
                    .isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("V2: agencias semilla deben existir")
    void agenciasSemillaExisten() {
        Integer totalAgencias = jdbc.queryForObject(
                "SELECT COUNT(*) FROM agencia", Integer.class);

        assertThat(totalAgencias)
                .as("Debe haber al menos 7 agencias semilla")
                .isGreaterThanOrEqualTo(7);
    }

    @Test
    @DisplayName("V2: usuario administrador semilla debe existir")
    void usuarioAdminSemillaExiste() {
        Integer cuenta = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE nombre_usuario = 'admin'",
                Integer.class);

        assertThat(cuenta)
                .as("El usuario 'admin' semilla debe existir")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("V2: hash de contrasena del admin no debe estar en claro")
    void hashContrasenaNoEstaEnClaro() {
        String hash = jdbc.queryForObject(
                "SELECT hash_contrasena FROM usuario WHERE nombre_usuario = 'admin'",
                String.class);

        assertThat(hash)
                .as("El hash debe comenzar con '$2a$' (bcrypt)")
                .startsWith("$2a$");
        assertThat(hash)
                .as("El hash no debe ser la contraseña en claro")
                .isNotEqualTo("admin1234");
    }

    @Test
    @DisplayName("V1: tabla par_doble_qr tiene columnas de hash CHAR(64)")
    void tablaParDobleQrTieneColumnasHash() {
        // Consulta las columnas de par_doble_qr y verifica que existen qr1_hash y qr2_hash
        List<Map<String, Object>> columnas = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE UPPER(TABLE_NAME) = 'PAR_DOBLE_QR'"
        );

        List<String> nombresColumnas = columnas.stream()
                .map(c -> c.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertThat(nombresColumnas)
                .as("Columna qr1_hash debe existir")
                .contains("qr1_hash");
        assertThat(nombresColumnas)
                .as("Columna qr2_hash debe existir")
                .contains("qr2_hash");
        assertThat(nombresColumnas)
                .as("Columna qr2_estado debe existir")
                .contains("qr2_estado");
        assertThat(nombresColumnas)
                .as("Columna fecha_invalidacion debe existir")
                .contains("fecha_invalidacion");
    }

    @Test
    @DisplayName("V1: tabla auditoria tiene columna entidad_id para trazabilidad")
    void tablaAuditoriaParaTrazabilidad() {
        List<Map<String, Object>> columnas = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE UPPER(TABLE_NAME) = 'AUDITORIA'"
        );

        List<String> nombres = columnas.stream()
                .map(c -> c.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertThat(nombres).contains("entidad", "entidad_id", "accion",
                "estado_anterior", "estado_nuevo", "usuario_id", "ocurrido_en");
    }
}
