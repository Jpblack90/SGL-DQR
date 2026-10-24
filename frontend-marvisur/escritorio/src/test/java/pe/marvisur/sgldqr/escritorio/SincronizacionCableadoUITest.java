package pe.marvisur.sgldqr.escritorio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ClienteDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ElementoColaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EstadoElementoCola;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RegistrarEncomiendaComando;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.UsuarioSesion;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.AutenticacionPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.ColaLocalPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.EstadoConexionPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.SincronizadorOffline;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.VentanillaOfflineServicio;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;
import pe.marvisur.sgldqr.escritorio.infraestructura.sqlite.SQLiteColaAdapter;
import pe.marvisur.sgldqr.escritorio.ui.ControladorAlmacen;
import pe.marvisur.sgldqr.escritorio.ui.ControladorPrincipal;

import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SincronizacionCableadoUITest {

    @TempDir
    Path tempDir;

    private SQLiteColaAdapter colaAdapter;
    private HttpServer httpServer;
    private int httpPort;
    private AtomicInteger peticionesSyncRecibidas;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("cableado-ui-test.db");
        colaAdapter = new SQLiteColaAdapter(dbPath.toString());
        peticionesSyncRecibidas = new AtomicInteger(0);

        UsuarioSesion sesion = new UsuarioSesion("emp-01", "Carlos", "Perez", "carlos@marvisur.pe", "ROLE_RECEPCIONISTA", "SUC-LIM-01", "jwt-token-valido");
        SesionContexto.establecerSesion(sesion);
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (colaAdapter != null) {
            colaAdapter.cerrar();
        }
        SesionContexto.limpiar();
    }

    private void iniciarServidorSimulado(boolean healthUp, boolean syncExitoso) throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpPort = httpServer.getAddress().getPort();

        httpServer.createContext("/actuator/health", exchange -> {
            byte[] resp = (healthUp ? "{\"status\":\"UP\"}" : "{\"status\":\"DOWN\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(healthUp ? 200 : 503, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        httpServer.createContext("/api/v1/sync/encomiendas", exchange -> {
            peticionesSyncRecibidas.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            // Armar respuesta según los IDs recibidos en el payload
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);
            JsonNode items = root.get("items");

            StringBuilder responseJson = new StringBuilder();
            responseJson.append("{\"totalRecibidos\":").append(items != null ? items.size() : 0)
                    .append(",\"totalExitosos\":").append(items != null ? items.size() : 0)
                    .append(",\"totalYaSincronizados\":0,\"totalErrores\":0,\"duracionMs\":5,\"detalles\":[");

            if (items != null) {
                for (int i = 0; i < items.size(); i++) {
                    String id = items.get(i).get("idEncomienda").asText();
                    String comp = items.get(i).get("codigoComprobante").asText();
                    if (i > 0) responseJson.append(",");
                    responseJson.append("{\"idEncomienda\":\"").append(id).append("\",")
                            .append("\"codigoComprobante\":\"").append(comp).append("\",")
                            .append("\"resultado\":\"OK\",\"motivoError\":null,\"mensaje\":\"Sincronizado correctamente\"}");
                }
            }
            responseJson.append("]}");

            byte[] respBytes = responseJson.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(syncExitoso ? 200 : 500, respBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        });

        httpServer.start();
    }

    @Test
    @DisplayName("Flujo completo: caída de servidor -> registro offline -> reconexión -> envío ÚNICO -> cola SINCRONIZADA")
    void testFlujoCaidaRegistroReconexionYSincronizacionUnica() throws Exception {
        // 1. Simular servidor caído apuntando a puerto libre sin proceso
        ServicioConexion servicio = new ServicioConexion("http://localhost:59999", Duration.ofSeconds(1), null);
        servicio.reportarFalloConexion("Servidor inactivo");
        assertThat(servicio.estaEnLinea()).isFalse();

        // 2. Registrar encomienda offline en ventanilla
        VentanillaOfflineServicio ventanilla = new VentanillaOfflineServicio(
                new MarvisurHttpClient("http://localhost:59999"),
                colaAdapter,
                servicio,
                new pe.marvisur.sgldqr.dominio.servicio.MotorQR(),
                new pe.marvisur.sgldqr.infraestructura.qr.ZXingAdapter(),
                pe.marvisur.sgldqr.aplicacion.TarifarioPorPeso.conValoresPorDefecto(),
                new pe.marvisur.sgldqr.escritorio.aplicacion.servicio.GeneradorCodigoComprobanteLocal(),
                "http://localhost:5173"
        );

        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                new ClienteDto("DNI", "12345678", "Juan Perez", "999888777", "juan@test.com"),
                new ClienteDto("RUC", "20123456789", "Empresa SAC", "988777666", "contacto@empresa.com"),
                "SUC-HYO-01",
                new java.math.BigDecimal("4.5"),
                new java.math.BigDecimal("0.015"),
                "Repuestos de prueba",
                "CONTADO"
        );

        ventanilla.registrarEncomienda(comando);
        assertThat(colaAdapter.contarPendientes()).isEqualTo(1);

        // 3. El servidor vuelve a estar disponible
        iniciarServidorSimulado(true, true);
        String urlServidor = "http://localhost:" + httpPort;
        MarvisurHttpClient clienteReal = new MarvisurHttpClient(urlServidor);

        ServicioConexion servicioConectado = new ServicioConexion(urlServidor, Duration.ofSeconds(2), null);
        boolean healthOk = servicioConectado.verificarConectividad();
        assertThat(healthOk).isTrue();
        assertThat(servicioConectado.estaEnLinea()).isTrue();

        // 4. Sincronizador procesa el lote
        SincronizadorOffline sincronizador = new SincronizadorOffline(colaAdapter, clienteReal, servicioConectado);
        int sincronizados = sincronizador.sincronizarLote(true);

        assertThat(sincronizados).isEqualTo(1);
        assertThat(peticionesSyncRecibidas.get()).isEqualTo(1);
        assertThat(colaAdapter.contarPendientes()).isEqualTo(0);

        List<ElementoColaDto> items = colaAdapter.listarTodos();
        assertThat(items.getFirst().getEstado()).isEqualTo(EstadoElementoCola.SINCRONIZADA);
    }

    @Test
    @DisplayName("Sincronización al iniciar sesión con registros pendientes en cola local")
    void testSincronizacionAlIniciarSesionConPendientes() throws Exception {
        iniciarServidorSimulado(true, true);
        String urlServidor = "http://localhost:" + httpPort;

        // Cargar 2 registros pendientes previos en la cola local
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        colaAdapter.guardarEnCola(id1, "{\"idEncomienda\":\"" + id1 + "\",\"codigoComprobante\":\"MV-TEST-001\"}");
        colaAdapter.guardarEnCola(id2, "{\"idEncomienda\":\"" + id2 + "\",\"codigoComprobante\":\"MV-TEST-002\"}");

        assertThat(colaAdapter.contarPendientes()).isEqualTo(2);

        ServicioConexion servicio = new ServicioConexion(urlServidor, Duration.ofSeconds(2), null);
        servicio.reportarExitoConexion();
        MarvisurHttpClient httpClient = new MarvisurHttpClient(urlServidor);
        SincronizadorOffline sincronizador = new SincronizadorOffline(colaAdapter, httpClient, servicio);

        // Al iniciar sesión se detectan pendientes y se sincroniza
        int sincronizados = sincronizador.sincronizarLote(true);

        assertThat(sincronizados).isEqualTo(2);
        assertThat(colaAdapter.contarPendientes()).isEqualTo(0);
        assertThat(peticionesSyncRecibidas.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Cableado UI: iniciar() del SincronizadorOffline se invoca al instanciar/arrancar ControladorPrincipal")
    void testCableadoIniciarSincronizador() {
        AtomicBoolean sincronizadorIniciado = new AtomicBoolean(false);
        SincronizadorOffline sincronizadorEspia = new SincronizadorOffline(colaAdapter, json -> null, ServicioConexion.getInstancia()) {
            @Override
            public synchronized void iniciar() {
                sincronizadorIniciado.set(true);
            }
        };

        AutenticacionPort authMock = new AutenticacionPort() {
            @Override public UsuarioSesion autenticar(String correo, String password) { return null; }
            @Override public void cerrarSesion() {}
            @Override public UsuarioSesion obtenerSesionActual() { return null; }
        };

        ControladorPrincipal controlador = new ControladorPrincipal(
                authMock,
                ServicioConexion.getInstancia(),
                sincronizadorEspia,
                colaAdapter
        );

        // Simular inicio de monitoreo de conectividad del controlador
        try {
            var metodo = ControladorPrincipal.class.getDeclaredMethod("iniciarMonitoreoConectividad");
            metodo.setAccessible(true);
            metodo.invoke(controlador);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            controlador.detenerTareasFondo();
        }

        assertThat(sincronizadorIniciado.get()).isTrue();
    }

    @Test
    @DisplayName("Verificación de formato: el payload de cola real cumple con SyncEncomiendaItemDto de openapi-interno.json")
    void testFormatoPayloadSyncContraEsquemaOpenApi() throws Exception {
        VentanillaOfflineServicio ventanilla = new VentanillaOfflineServicio(
                new MarvisurHttpClient("http://localhost:59999"),
                colaAdapter,
                ServicioConexion.getInstancia(),
                new pe.marvisur.sgldqr.dominio.servicio.MotorQR(),
                new pe.marvisur.sgldqr.infraestructura.qr.ZXingAdapter(),
                pe.marvisur.sgldqr.aplicacion.TarifarioPorPeso.conValoresPorDefecto(),
                new pe.marvisur.sgldqr.escritorio.aplicacion.servicio.GeneradorCodigoComprobanteLocal(),
                "http://localhost:5173"
        );

        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                new ClienteDto("DNI", "87654321", "Maria Lopez", "912345678", "maria@gmail.com"),
                new ClienteDto("RUC", "20987654321", "Distribuidora Central EIRL", "923456789", "ventas@central.pe"),
                "SUC-AQP-01",
                new java.math.BigDecimal("8.75"),
                new java.math.BigDecimal("0.035"),
                "Textiles y confecciones",
                "CONTRA_ENTREGA"
        );

        var resultado = ventanilla.registrarEncomienda(comando);
        assertThat(resultado).isNotNull();

        List<ElementoColaDto> pendientes = colaAdapter.listarPendientes(10);
        assertThat(pendientes).hasSize(1);

        String jsonPayload = pendientes.getFirst().payload();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonPayload);

        // 1. Campos obligatorios de primer nivel
        assertThat(root.hasNonNull("idEncomienda")).isTrue();
        assertThat(UUID.fromString(root.get("idEncomienda").asText())).isNotNull();

        assertThat(root.hasNonNull("codigoComprobante")).isTrue();
        assertThat(root.get("codigoComprobante").asText()).startsWith("MV-");

        assertThat(root.hasNonNull("idSucursalOrigen")).isTrue();
        assertThat(root.hasNonNull("idSucursalDestino")).isTrue();
        assertThat(root.get("idSucursalDestino").asText()).isEqualTo("SUC-AQP-01");

        assertThat(root.hasNonNull("pesoKg")).isTrue();
        assertThat(root.get("pesoKg").asDouble()).isGreaterThan(0.0);

        assertThat(root.hasNonNull("modalidadPago")).isTrue();
        assertThat(root.get("modalidadPago").asText()).isIn("CONTADO", "CONTRA_ENTREGA");

        assertThat(root.hasNonNull("fechaRegistro")).isTrue();
        assertThat(LocalDateTime.parse(root.get("fechaRegistro").asText())).isNotNull();

        // 2. Cliente remitente
        JsonNode rem = root.get("remitente");
        assertThat(rem).isNotNull();
        assertThat(rem.hasNonNull("tipoDocumento")).isTrue();
        assertThat(rem.hasNonNull("numeroDocumento")).isTrue();
        assertThat(rem.hasNonNull("nombresRazonSocial")).isTrue();
        assertThat(rem.get("numeroDocumento").asText()).isEqualTo("87654321");

        // 3. Cliente destinatario
        JsonNode dest = root.get("destinatario");
        assertThat(dest).isNotNull();
        assertThat(dest.hasNonNull("tipoDocumento")).isTrue();
        assertThat(dest.hasNonNull("numeroDocumento")).isTrue();
        assertThat(dest.hasNonNull("nombresRazonSocial")).isTrue();
        assertThat(dest.get("numeroDocumento").asText()).isEqualTo("20987654321");

        // 4. Doble QR (QrSyncDto)
        JsonNode qr = root.get("qr");
        assertThat(qr).isNotNull();
        assertThat(qr.hasNonNull("idPareja")).isTrue();
        assertThat(UUID.fromString(qr.get("idPareja").asText())).isNotNull();

        assertThat(qr.hasNonNull("uuidQr1")).isTrue();
        assertThat(qr.get("uuidQr1").asText()).startsWith("MARVISUR:QR1:");

        assertThat(qr.hasNonNull("hashQr2")).isTrue();
        String hashQr2 = qr.get("hashQr2").asText();
        assertThat(hashQr2).matches("^[a-fA-F0-9]{64}$");

        // 5. Cero token en claro en payload
        assertThat(root.has("qr2TokenEnClaro")).isFalse();
        assertThat(qr.has("qr2TokenEnClaro")).isFalse();
    }

    @Test
    @DisplayName("Mensajes de error: obtenerMensajeErrorClaro transforma errores nulos o de red en texto amigable")
    void testObtenerMensajeErrorClaroControladorAlmacen() {
        assertThat(ControladorAlmacen.obtenerMensajeErrorClaro(null))
                .isEqualTo("Sin conexión con el servidor.");

        assertThat(ControladorAlmacen.obtenerMensajeErrorClaro(new ConnectException()))
                .isEqualTo("Sin conexión con el servidor.");

        assertThat(ControladorAlmacen.obtenerMensajeErrorClaro(new RuntimeException("null")))
                .isEqualTo("Sin conexión con el servidor.");

        assertThat(ControladorAlmacen.obtenerMensajeErrorClaro(new RuntimeException("   ")))
                .isEqualTo("Sin conexión con el servidor.");

        assertThat(ControladorAlmacen.obtenerMensajeErrorClaro(new RuntimeException(new HttpTimeoutException("timeout"))))
                .isEqualTo("Sin conexión con el servidor.");

        assertThat(ControladorAlmacen.obtenerMensajeErrorClaro(new RuntimeException("QR1 no registrado en el sistema")))
                .isEqualTo("QR1 no registrado en el sistema");
    }
}
