package pe.marvisur.sgldqr.escritorio.infraestructura.sqlite;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ElementoColaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EstadoElementoCola;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.SucursalDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.ColaLocalPort;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptador de persistencia SQLite para la cola de registros offline y la caché local de sucursales (O1, O2).
 *
 * <p><strong>Seguridad:</strong>
 * La base de datos local SQLite almacena únicamente los hashes de la pareja criptográfica (Doble QR)
 * y los metadatos de envío. En ningún momento se persiste ni se escribe en disco el token QR2 en claro.
 * </p>
 */
public class SQLiteColaAdapter implements ColaLocalPort {

    private static final Logger LOGGER = Logger.getLogger(SQLiteColaAdapter.class.getName());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MAX_INTENTOS_REINTENTO = 5;

    private final String dbUrl;

    public SQLiteColaAdapter() {
        this(obtenerRutaPorDefecto());
    }

    public SQLiteColaAdapter(String rutaArchivoDb) {
        Objects.requireNonNull(rutaArchivoDb, "La ruta de la base de datos SQLite no puede ser nula");
        if (!":memory:".equals(rutaArchivoDb) && !rutaArchivoDb.startsWith("jdbc:sqlite:")) {
            asegurarDirectorioPadre(rutaArchivoDb);
            this.dbUrl = "jdbc:sqlite:" + rutaArchivoDb;
        } else if (rutaArchivoDb.startsWith("jdbc:sqlite:")) {
            this.dbUrl = rutaArchivoDb;
        } else {
            this.dbUrl = "jdbc:sqlite::memory:";
        }

        inicializarTablas();
    }

    private static String obtenerRutaPorDefecto() {
        try (InputStream is = SQLiteColaAdapter.class.getResourceAsStream("/escritorio.properties")) {
            if (is != null) {
                Properties prop = new Properties();
                prop.load(is);
                String customPath = prop.getProperty("sqlite.db-path");
                if (customPath != null && !customPath.isBlank()) {
                    return customPath.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "datos-locales/marvisur-local.db";
    }

    private void asegurarDirectorioPadre(String ruta) {
        try {
            Path path = Paths.get(ruta);
            Path padre = path.getParent();
            if (padre != null && !Files.exists(padre)) {
                Files.createDirectories(padre);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo asegurar el directorio padre para SQLite: " + e.getMessage());
        }
    }

    private Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(this.dbUrl);
    }

    private void inicializarTablas() {
        String sqlCola = """
                CREATE TABLE IF NOT EXISTS cola_registro (
                    id TEXT PRIMARY KEY,
                    payload TEXT NOT NULL,
                    estado TEXT NOT NULL,
                    intentos INTEGER NOT NULL DEFAULT 0,
                    ultimo_error TEXT,
                    creado_en TEXT NOT NULL,
                    actualizado_en TEXT
                );
                """;

        String sqlCache = """
                CREATE TABLE IF NOT EXISTS cache_sucursales (
                    id TEXT PRIMARY KEY,
                    nombre_agencia TEXT NOT NULL,
                    ciudad TEXT NOT NULL,
                    direccion TEXT NOT NULL,
                    actualizado_en TEXT NOT NULL
                );
                """;

        String sqlIndex = "CREATE INDEX IF NOT EXISTS idx_cola_estado_creado ON cola_registro(estado, creado_en);";

        try (Connection conn = obtenerConexion(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCola);
            stmt.execute(sqlCache);
            stmt.execute(sqlIndex);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al inicializar las tablas de la base local SQLite: " + e.getMessage(), e);
            throw new RuntimeException("Error al inicializar SQLite local: " + e.getMessage(), e);
        }
    }

    @Override
    public void guardarEnCola(String id, String payloadJson) {
        Objects.requireNonNull(id, "El id no puede ser nulo");
        Objects.requireNonNull(payloadJson, "El payload no puede ser nulo");

        String sql = """
                INSERT OR REPLACE INTO cola_registro (id, payload, estado, intentos, ultimo_error, creado_en, actualizado_en)
                VALUES (?, ?, 'PENDIENTE', 0, NULL, ?, ?)
                """;

        String ahoraIso = LocalDateTime.now().format(ISO_FORMATTER);
        try (Connection conn = obtenerConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim());
            ps.setString(2, payloadJson);
            ps.setString(3, ahoraIso);
            ps.setString(4, ahoraIso);
            ps.executeUpdate();
            LOGGER.info("Registro " + id + " guardado en la cola local SQLite como PENDIENTE.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al guardar en cola_registro: " + e.getMessage(), e);
            throw new RuntimeException("Error al guardar en cola local SQLite: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ElementoColaDto> listarPendientes(int limite) {
        int max = limite > 0 ? limite : 50;
        String sql = """
                SELECT id, payload, estado, intentos, ultimo_error, creado_en, actualizado_en
                FROM cola_registro
                WHERE estado = 'PENDIENTE' AND intentos < ?
                ORDER BY creado_en ASC
                LIMIT ?
                """;

        List<ElementoColaDto> lista = new ArrayList<>();
        try (Connection conn = obtenerConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, MAX_INTENTOS_REINTENTO);
            ps.setInt(2, max);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearFila(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar pendientes de cola_registro: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public List<ElementoColaDto> listarTodos() {
        String sql = """
                SELECT id, payload, estado, intentos, ultimo_error, creado_en, actualizado_en
                FROM cola_registro
                ORDER BY creado_en DESC
                """;

        List<ElementoColaDto> lista = new ArrayList<>();
        try (Connection conn = obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearFila(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al listar todos los elementos de cola_registro: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void marcarSincronizada(String id) {
        Objects.requireNonNull(id, "El id no puede ser nulo");
        String sql = "UPDATE cola_registro SET estado = 'SINCRONIZADA', actualizado_en = ? WHERE id = ?";
        String ahoraIso = LocalDateTime.now().format(ISO_FORMATTER);

        try (Connection conn = obtenerConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ahoraIso);
            ps.setString(2, id.trim());
            ps.executeUpdate();
            LOGGER.info("Registro " + id + " actualizado a estado SINCRONIZADA en la cola local.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al marcar como SINCRONIZADA: " + e.getMessage(), e);
        }
    }

    @Override
    public void marcarError(String id, String ultimoError, boolean reintentable) {
        Objects.requireNonNull(id, "El id no puede ser nulo");
        String ahoraIso = LocalDateTime.now().format(ISO_FORMATTER);

        String sql;
        if (!reintentable) {
            sql = "UPDATE cola_registro SET estado = 'ERROR', intentos = intentos + 1, ultimo_error = ?, actualizado_en = ? WHERE id = ?";
        } else {
            sql = """
                  UPDATE cola_registro
                  SET intentos = intentos + 1,
                      ultimo_error = ?,
                      actualizado_en = ?,
                      estado = CASE WHEN intentos + 1 >= ? THEN 'ERROR' ELSE 'PENDIENTE' END
                  WHERE id = ?
                  """;
        }

        try (Connection conn = obtenerConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ultimoError != null ? ultimoError : "Error no especificado");
            ps.setString(2, ahoraIso);
            if (!reintentable) {
                ps.setString(3, id.trim());
            } else {
                ps.setInt(3, MAX_INTENTOS_REINTENTO);
                ps.setString(4, id.trim());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al marcar error en cola_registro: " + e.getMessage(), e);
        }
    }

    @Override
    public void reiniciarErroresReintentables() {
        String sql = """
                UPDATE cola_registro
                SET estado = 'PENDIENTE', intentos = 0, actualizado_en = ?
                WHERE estado = 'ERROR' AND (ultimo_error NOT LIKE '%VALIDACION%' OR ultimo_error IS NULL)
                """;

        String ahoraIso = LocalDateTime.now().format(ISO_FORMATTER);
        try (Connection conn = obtenerConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ahoraIso);
            int filas = ps.executeUpdate();
            LOGGER.info("Se reiniciaron " + filas + " registros en error a estado PENDIENTE.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al reiniciar errores: " + e.getMessage(), e);
        }
    }

    @Override
    public int contarPendientes() {
        String sql = "SELECT COUNT(*) FROM cola_registro WHERE estado = 'PENDIENTE'";
        try (Connection conn = obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al contar pendientes en cola_registro: " + e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public void guardarCacheSucursales(List<SucursalDto> sucursales) {
        if (sucursales == null || sucursales.isEmpty()) return;

        String sql = """
                INSERT OR REPLACE INTO cache_sucursales (id, nombre_agencia, ciudad, direccion, actualizado_en)
                VALUES (?, ?, ?, ?, ?)
                """;

        String ahoraIso = LocalDateTime.now().format(ISO_FORMATTER);
        try (Connection conn = obtenerConexion()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (SucursalDto s : sucursales) {
                    ps.setString(1, s.id());
                    ps.setString(2, s.nombreAgencia());
                    ps.setString(3, s.ciudad() != null ? s.ciudad() : "");
                    ps.setString(4, s.direccion() != null ? s.direccion() : "");
                    ps.setString(5, ahoraIso);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
            LOGGER.info("Caché de sucursales actualizada exitosamente (" + sucursales.size() + " registros).");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al guardar sucursales en caché local: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SucursalDto> obtenerCacheSucursales() {
        String sql = "SELECT id, nombre_agencia, ciudad, direccion FROM cache_sucursales ORDER BY ciudad ASC, nombre_agencia ASC";
        List<SucursalDto> lista = new ArrayList<>();
        try (Connection conn = obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new SucursalDto(
                        rs.getString("id"),
                        rs.getString("nombre_agencia"),
                        rs.getString("ciudad"),
                        rs.getString("direccion")
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error al consultar sucursales desde caché local: " + e.getMessage(), e);
        }
        return lista;
    }

    private ElementoColaDto mapearFila(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String payload = rs.getString("payload");
        EstadoElementoCola estado = EstadoElementoCola.valueOf(rs.getString("estado"));
        int intentos = rs.getInt("intentos");
        String ultimoError = rs.getString("ultimo_error");

        LocalDateTime creadoEn = LocalDateTime.parse(rs.getString("creado_en"), ISO_FORMATTER);
        String actStr = rs.getString("actualizado_en");
        LocalDateTime actualizadoEn = actStr != null ? LocalDateTime.parse(actStr, ISO_FORMATTER) : null;

        return new ElementoColaDto(id, payload, estado, intentos, ultimoError, creadoEn, actualizadoEn);
    }

    public void cerrar() {
        // Conexiones manejadas por sentencia con try-with-resources; no-op para compatibilidad de ciclo de vida
    }
}
