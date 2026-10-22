package pe.marvisur.sgldqr.api;

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
 * Prueba de integración que verifica que las migraciones Flyway (V1 a V7)
 * se aplican correctamente sobre una base de datos vacía, validando
 * exactamente las 18 tablas, columnas, tipos y datos iniciales del Diccionario de Datos.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Migraciones Flyway PostgreSQL — Validación contra Diccionario de Datos")
class MigracionesFlywayTest {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Las 18 tablas exactas definidas en docs/Diccionario_Datos_SGL_DQR.md
     */
    private static final List<String> TABLAS_DICCIONARIO = List.of(
            // Módulo 1: Seguridad, Personal y Auditoría
            "ROLES",
            "EMPLEADOS",
            "AUDITORIA_LOGS",
            // Módulo 2: Sucursales, Flotas y Rutas
            "SUCURSALES",
            "VEHICULOS",
            "RUTAS",
            "PROGRAMACION_VIAJES",
            // Módulo 3: Clientes y Pre-Registros
            "CLIENTES",
            "PRE_REGISTROS_EXPRESS",
            // Módulo 4: Encomiendas, Pagos y Almacén
            "ENCOMIENDAS",
            "DESPACHOS_LOTE_DETALLES",
            "INCIDENCIAS_EMERGENCIA",
            // Módulo 5: Motor Criptográfico del Doble QR y Tokens
            "PAREJAS_DOBLE_QR",
            "TOKENS_QR2_ESTATUS",
            "VALIDACIONES_CRUZADAS_LOGS",
            // Módulo 6: Trazabilidad, Tracking y Comprobantes PDF
            "HISTORIAL_TRACKING",
            "COMPROBANTES_PAGO",
            "ARCHIVOS_PDF_COMPROBANTE"
    );

    @Test
    @DisplayName("Diccionario: las 18 tablas exactas deben existir en el esquema")
    void todasLas18TablasDelDiccionarioExisten() {
        for (String tabla : TABLAS_DICCIONARIO) {
            Integer cuenta = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = ?",
                    Integer.class, tabla
            );
            assertThat(cuenta)
                    .as("La tabla '%s' requerida por el Diccionario de Datos debe existir", tabla)
                    .isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("V7: Los 4 roles del Diccionario deben existir con sus nombres exactos")
    void rolesInicialesExisten() {
        List<String> nombresRoles = jdbc.queryForList(
                "SELECT nombre_rol FROM roles ORDER BY nombre_rol",
                String.class
        );

        assertThat(nombresRoles)
                .as("Deben existir exactamente los 4 roles del Diccionario")
                .containsExactlyInAnyOrder("Administrador", "Almacenero", "Recepcionista", "Chofer");
    }

    @Test
    @DisplayName("V7: Sucursal de ejemplo debe existir")
    void sucursalEjemploExiste() {
        Integer cuenta = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sucursales WHERE id_sucursal = 'SUC-LIM-01'",
                Integer.class
        );

        assertThat(cuenta).isEqualTo(1);
    }

    @Test
    @DisplayName("V7: Usuario Administrador (TN3) existe con rol Administrador y hash BCrypt válido")
    void usuarioAdminTn3ExisteConHashValido() {
        Map<String, Object> admin = jdbc.queryForMap(
                "SELECT e.dni, e.correo, e.password_hash, e.estado, r.nombre_rol " +
                "FROM empleados e JOIN roles r ON e.id_rol = r.id_rol " +
                "WHERE e.id_empleado = 'EMP-ADMIN-01'"
        );

        assertThat(admin.get("DNI")).isEqualTo("00000001");
        assertThat(admin.get("CORREO")).isEqualTo("admin@marvisur.pe");
        assertThat(admin.get("ESTADO")).isEqualTo(true);
        assertThat(admin.get("NOMBRE_ROL")).isEqualTo("Administrador");

        String hash = (String) admin.get("PASSWORD_HASH");
        assertThat(hash)
                .as("El hash debe tener formato BCrypt ($2a$ o $2b$)")
                .matches("^\\$2[ab]\\$\\d{2}\\$[A-Za-z0-9./]{53}$");
    }

    @Test
    @DisplayName("Módulo 5: Tabla parejas_doble_qr contiene uuid_qr1_operativo y hash_qr2_llave_digital")
    void parejaDobleQrTieneColumnasCriptograficas() {
        List<Map<String, Object>> columnas = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = 'PAREJAS_DOBLE_QR'"
        );

        List<String> nombres = columnas.stream()
                .map(c -> c.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertThat(nombres)
                .contains("id_pareja_qr", "id_encomienda", "uuid_qr1_operativo", "hash_qr2_llave_digital", "fecha_generacion");
    }

    @Test
    @DisplayName("Módulo 4: Tabla encomiendas contiene estado_pago y estado_logistico")
    void encomiendasTieneEstadosDelDiccionario() {
        List<Map<String, Object>> columnas = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = 'ENCOMIENDAS'"
        );

        List<String> nombres = columnas.stream()
                .map(c -> c.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertThat(nombres)
                .contains("id_encomienda", "codigo_comprobante", "id_remitente", "id_destinatario",
                        "id_sucursal_origen", "id_sucursal_destino", "peso_kg", "volumen_m3",
                        "descripcion_contenido", "monto_total", "estado_pago", "estado_logistico", "fecha_registro");
    }

    @Test
    @DisplayName("Flyway: Las 8 versiones de migración SQL se aplicaron con éxito")
    void todasLasMigracionesFlywayFueronExitosas() {
        Integer migracionesExitosas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"type\" = 'SQL' AND \"success\" = TRUE",
                Integer.class
        );

        assertThat(migracionesExitosas)
                .as("Deben haberse aplicado exitosamente las 8 migraciones Flyway (V1 a V8)")
                .isEqualTo(8);
    }
}
