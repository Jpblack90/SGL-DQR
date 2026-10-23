package pe.marvisur.sgldqr.escritorio;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.*;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarvisurHttpClientTest {

    private HttpServer servidorMock;
    private String baseUrl;
    private MarvisurHttpClient cliente;

    @BeforeEach
    void iniciarServidorMock() throws IOException {
        SesionContexto.limpiar();
        servidorMock = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        int puerto = servidorMock.getAddress().getPort();
        baseUrl = "http://localhost:" + puerto;
        cliente = new MarvisurHttpClient(baseUrl);
    }

    @AfterEach
    void detenerServidorMock() {
        if (servidorMock != null) {
            servidorMock.stop(0);
        }
        SesionContexto.limpiar();
    }

    @Test
    @DisplayName("autenticar exitoso: almacena token en memoria y devuelve UsuarioSesion")
    void testAutenticarExitoso() {
        servidorMock.createContext("/api/v1/auth/login", exchange -> {
            String respuesta = """
                    {
                      "token": "jwt.mock.token.para.pruebas",
                      "tipo": "Bearer",
                      "expiracion": "2026-10-23T00:00:00",
                      "expiraEnSegundos": 28800,
                      "empleado": {
                        "id": "EMP-001",
                        "nombres": "Juan",
                        "apellidos": "Pérez",
                        "correo": "juan.perez@marvisur.pe",
                        "rol": "ROLE_RECEPCIONISTA",
                        "idSucursal": "SUC-LIM-01"
                      }
                    }
                    """;
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        UsuarioSesion sesion = cliente.autenticar("juan.perez@marvisur.pe", "ClaveSecreta123");

        assertThat(sesion).isNotNull();
        assertThat(sesion.token()).isEqualTo("jwt.mock.token.para.pruebas");
        assertThat(sesion.id()).isEqualTo("EMP-001");
        assertThat(sesion.getNombreCompleto()).isEqualTo("Juan Pérez");
        assertThat(sesion.rol()).isEqualTo("ROLE_RECEPCIONISTA");
        assertThat(sesion.idSucursal()).isEqualTo("SUC-LIM-01");

        // Verifica que la sesión viva en memoria en SesionContexto
        assertThat(SesionContexto.estaAutenticado()).isTrue();
        assertThat(SesionContexto.obtenerToken()).isEqualTo("jwt.mock.token.para.pruebas");
    }

    @Test
    @DisplayName("autenticar con credenciales inválidas: lanza ApiExcepcion 401")
    void testAutenticarCredencialesInvalidas() {
        servidorMock.createContext("/api/v1/auth/login", exchange -> {
            String respuesta = """
                    {
                      "title": "Credenciales inválidas",
                      "status": 401,
                      "detail": "El correo o contraseña no coinciden"
                    }
                    """;
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        assertThatThrownBy(() -> cliente.autenticar("usuario@invalido.pe", "mala_clave"))
                .isInstanceOf(ApiExcepcion.class)
                .hasMessageContaining("Credenciales inválidas");

        assertThat(SesionContexto.estaAutenticado()).isFalse();
    }

    @Test
    @DisplayName("listarSucursales: devuelve lista de agencias parseadas desde JSON")
    void testListarSucursales() {
        servidorMock.createContext("/api/v1/publico/sucursales", exchange -> {
            String respuesta = """
                    [
                      {
                        "id": "SUC-LIM-01",
                        "nombreAgencia": "Agencia Lima Central",
                        "ciudad": "Lima",
                        "direccion": "Av. Grau 450"
                      },
                      {
                        "id": "SUC-AQP-01",
                        "nombreAgencia": "Agencia Arequipa",
                        "ciudad": "Arequipa",
                        "direccion": "Calle Jacinto Ibañez 315"
                      }
                    ]
                    """;
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        List<SucursalDto> sucursales = cliente.listarSucursales();

        assertThat(sucursales).hasSize(2);
        assertThat(sucursales.get(0).id()).isEqualTo("SUC-LIM-01");
        assertThat(sucursales.get(0).nombreAgencia()).isEqualTo("Agencia Lima Central");
        assertThat(sucursales.get(1).ciudad()).isEqualTo("Arequipa");
    }

    @Test
    @DisplayName("cotizar: devuelve monto estimado y moneda")
    void testCotizar() {
        servidorMock.createContext("/api/v1/publico/cotizacion", exchange -> {
            String respuesta = """
                    {
                      "montoEstimado": 16.25,
                      "moneda": "PEN"
                    }
                    """;
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        CotizacionComando comando = new CotizacionComando("SUC-LIM-01", "SUC-AQP-01", new BigDecimal("4.5"), null);
        CotizacionResultado res = cliente.cotizar(comando);

        assertThat(res).isNotNull();
        assertThat(res.montoEstimado()).isEqualByComparingTo("16.25");
        assertThat(res.moneda()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("consultarPreRegistro: recupera datos de Código Express")
    void testConsultarPreRegistroExitoso() {
        servidorMock.createContext("/api/v1/preregistros/CEX-481920", exchange -> {
            String respuesta = """
                    {
                      "idPreRegistro": "PRE-UUID-001",
                      "codigoExpress": "CEX-481920",
                      "remitente": {
                        "tipoDocumento": "DNI",
                        "numeroDocumento": "45678901",
                        "nombresRazonSocial": "Ana Flores",
                        "telefono": "987654321",
                        "correo": "ana@ejemplo.com"
                      },
                      "destinatario": {
                        "tipoDocumento": "DNI",
                        "numeroDocumento": "45678902",
                        "nombresRazonSocial": "Luis Morales",
                        "telefono": "912345678",
                        "correo": "luis@ejemplo.com"
                      },
                      "idSucursalDestino": "SUC-AQP-01",
                      "pesoEstimado": 4.5,
                      "montoCotizado": 16.25,
                      "fechaCreacion": "2026-10-22T10:00:00"
                    }
                    """;
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        PreRegistroDto dto = cliente.consultarPreRegistro("CEX-481920");

        assertThat(dto).isNotNull();
        assertThat(dto.codigoExpress()).isEqualTo("CEX-481920");
        assertThat(dto.remitente().numeroDocumento()).isEqualTo("45678901");
        assertThat(dto.destinatario().nombresRazonSocial()).isEqualTo("Luis Morales");
        assertThat(dto.pesoEstimado()).isEqualByComparingTo("4.5");
        assertThat(dto.montoCotizado()).isEqualByComparingTo("16.25");
    }

    @Test
    @DisplayName("consultarPreRegistro inexistente: lanza ApiExcepcion 404")
    void testConsultarPreRegistroNoEncontrado() {
        servidorMock.createContext("/api/v1/preregistros/CEX-999999", exchange -> {
            exchange.sendResponseHeaders(404, -1);
        });
        servidorMock.start();

        assertThatThrownBy(() -> cliente.consultarPreRegistro("CEX-999999"))
                .isInstanceOf(ApiExcepcion.class)
                .hasMessageContaining("No se encontró ningún pre-registro");
    }

    @Test
    @DisplayName("registrarEncomienda: envía JSON con cabecera Bearer y procesa respuesta con Doble QR")
    void testRegistrarEncomiendaExitoso() {
        // Simulamos sesión previa
        SesionContexto.establecerSesion(new UsuarioSesion("EMP-01", "Recepcionista", "Demo", "rec@demo.pe", "ROLE_RECEPCIONISTA", "SUC-LIM-01", "jwt.token.valido"));

        servidorMock.createContext("/api/v1/encomiendas", exchange -> {
            // Verificar que se envíe la cabecera Bearer
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            assertThat(auth).isEqualTo("Bearer jwt.token.valido");

            String respuesta = """
                    {
                      "idEncomienda": "UUID-ENC-001",
                      "codigoComprobante": "MV-20261022-ABCD1234",
                      "idRemitente": "CLI-01",
                      "idDestinatario": "CLI-02",
                      "idSucursalOrigen": "SUC-LIM-01",
                      "idSucursalDestino": "SUC-AQP-01",
                      "pesoKg": 5.0,
                      "montoTotal": 17.50,
                      "estadoPago": "PAGADO",
                      "estadoLogistico": "REGISTRADO",
                      "fechaRegistro": "2026-10-22T12:00:00",
                      "qr1": {
                        "payload": "UUID-ENC-001",
                        "imagenPngBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
                      },
                      "qr2Token": "TOKEN-DIGITAL-UNICO-12345",
                      "enlaceBilletera": "http://localhost:5173/billetera#TOKEN-DIGITAL-UNICO-12345"
                    }
                    """;
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                new ClienteDto("DNI", "45678901", "Ana Flores", "987654321", "ana@ejemplo.com"),
                new ClienteDto("DNI", "45678902", "Luis Morales", "912345678", "luis@ejemplo.com"),
                "SUC-AQP-01",
                new BigDecimal("5.0"),
                null,
                "Ropa",
                "CONTADO"
        );

        EncomiendaRegistradaResultado resultado = cliente.registrarEncomienda(comando);

        assertThat(resultado).isNotNull();
        assertThat(resultado.idEncomienda()).isEqualTo("UUID-ENC-001");
        assertThat(resultado.codigoComprobante()).isEqualTo("MV-20261022-ABCD1234");
        assertThat(resultado.montoTotal()).isEqualByComparingTo("17.50");
        assertThat(resultado.estadoPago()).isEqualTo("PAGADO");
        assertThat(resultado.qr1().payload()).isEqualTo("UUID-ENC-001");
        assertThat(resultado.qr2Token()).isEqualTo("TOKEN-DIGITAL-UNICO-12345");
        assertThat(resultado.enlaceBilletera()).contains("TOKEN-DIGITAL-UNICO-12345");
    }

    @Test
    @DisplayName("verificarConectividad: responde true cuando /actuator/health retorna 200 UP, false si falla")
    void testVerificarConectividad() {
        servidorMock.createContext("/actuator/health", exchange -> {
            String respuesta = "{\"status\":\"UP\"}";
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        assertThat(cliente.verificarConectividad()).isTrue();

        // Si el servidor se detiene, verificarConectividad debe responder false sin lanzar excepción no capturada
        servidorMock.stop(0);
        assertThat(cliente.verificarConectividad()).isFalse();
    }

    @Test
    @DisplayName("auto-logout en 401: notifica al listener de expiración de sesión")
    void testNotificacionExpiracionSesion() {
        SesionContexto.establecerSesion(new UsuarioSesion("EMP-01", "Ana", "Torres", "ana@marvisur.pe", "ROLE_RECEPCIONISTA", "SUC-LIM-01", "jwt.token.expirado"));

        AtomicBoolean notificado = new AtomicBoolean(false);
        SesionContexto.registrarListenerExpiracion(motivo -> notificado.set(true));

        servidorMock.createContext("/api/v1/encomiendas", exchange -> {
            exchange.sendResponseHeaders(401, -1);
        });
        servidorMock.start();

        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                new ClienteDto("DNI", "45678901", "Remitente", null, null),
                new ClienteDto("DNI", "45678902", "Destinatario", null, null),
                "SUC-AQP-01",
                new BigDecimal("2.0"),
                null,
                "Caja",
                "CONTADO"
        );

        assertThatThrownBy(() -> cliente.registrarEncomienda(comando))
                .isInstanceOf(ApiExcepcion.class);

        assertThat(notificado.get()).isTrue();
        assertThat(SesionContexto.estaAutenticado()).isFalse();
    }
}
