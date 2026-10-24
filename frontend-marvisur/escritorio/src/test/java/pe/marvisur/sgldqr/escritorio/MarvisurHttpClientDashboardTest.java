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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarvisurHttpClientDashboardTest {

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
    @DisplayName("obtenerResumenDashboard: 200 OK con JSON deserializa todos los KPIs, métricas y estancados")
    void testObtenerResumenDashboard_200_Exitoso() {
        String jsonRespuesta = """
                {
                  "generadoEn": "2026-10-24T18:30:00-05:00",
                  "periodo": "HOY",
                  "idSucursal": null,
                  "totales": {
                    "registradas": 42,
                    "entregadas": 35,
                    "enRuta": 4,
                    "pendientesDePago": {
                      "cantidad": 2,
                      "monto": 90.00
                    },
                    "recaudado": 1850.50
                  },
                  "porEstadoLogistico": {
                    "REGISTRADO": 3,
                    "EN_ALMACEN_ORIGEN": 2,
                    "EN_RUTA": 4,
                    "EN_ALMACEN_DESTINO": 2,
                    "ENTREGADO": 35
                  },
                  "recaudacionPorSucursal": [
                    {
                      "idSucursal": "SUC-LIM-01",
                      "nombreAgencia": "Agencia Central Lima",
                      "recaudado": 1200.00,
                      "pendiente": 50.00
                    },
                    {
                      "idSucursal": "SUC-AQP-01",
                      "nombreAgencia": "Agencia Arequipa",
                      "recaudado": 650.50,
                      "pendiente": 40.00
                    }
                  ],
                  "validaciones": {
                    "total": 35,
                    "exitosas": 33,
                    "rechazadasPagoPendiente": 1,
                    "rechazadasTokenInvalido": 1,
                    "latenciaPromedioMs": 142.5,
                    "latenciaMaximaMs": 310
                  },
                  "estancados": [
                    {
                      "codigoComprobante": "T001-00000042",
                      "estadoLogistico": "EN_ALMACEN_ORIGEN",
                      "nombreAgenciaDestino": "Agencia Arequipa",
                      "horasSinMovimiento": 52
                    }
                  ],
                  "umbralEstancadoHoras": 48
                }
                """;

        servidorMock.createContext("/api/v1/dashboard/resumen", exchange -> {
            assertThat(exchange.getRequestURI().getQuery()).contains("periodo=HOY");
            byte[] bytes = jsonRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        DashboardResumenDto dto = cliente.obtenerResumenDashboard(PeriodoDashboard.HOY, null);

        assertThat(dto).isNotNull();
        assertThat(dto.periodo()).isEqualTo(PeriodoDashboard.HOY);
        assertThat(dto.idSucursal()).isNull();
        assertThat(dto.umbralEstancadoHoras()).isEqualTo(48);

        // Totales
        assertThat(dto.totales().registradas()).isEqualTo(42);
        assertThat(dto.totales().entregadas()).isEqualTo(35);
        assertThat(dto.totales().enRuta()).isEqualTo(4);
        assertThat(dto.totales().pendientesDePago().cantidad()).isEqualTo(2);
        assertThat(dto.totales().pendientesDePago().monto()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(dto.totales().recaudado()).isEqualByComparingTo(new BigDecimal("1850.50"));

        // Estados
        assertThat(dto.porEstadoLogistico()).containsEntry("REGISTRADO", 3L);
        assertThat(dto.porEstadoLogistico()).containsEntry("ENTREGADO", 35L);

        // Recaudación
        assertThat(dto.recaudacionPorSucursal()).hasSize(2);
        assertThat(dto.recaudacionPorSucursal().get(0).nombreAgencia()).isEqualTo("Agencia Central Lima");

        // Validaciones
        assertThat(dto.validaciones().total()).isEqualTo(35);
        assertThat(dto.validaciones().latenciaPromedioMs()).isEqualTo(142.5);
        assertThat(dto.validaciones().latenciaMaximaMs()).isEqualTo(310);

        // Estancados
        assertThat(dto.estancados()).hasSize(1);
        assertThat(dto.estancados().get(0).codigoComprobante()).isEqualTo("T001-00000042");
        assertThat(dto.estancados().get(0).horasSinMovimiento()).isEqualTo(52);
    }

    @Test
    @DisplayName("obtenerResumenDashboard: 401 Not Authorized lanza ApiExcepcion y notifica sesión expirada")
    void testObtenerResumenDashboard_401_NotAuthorized() {
        AtomicBoolean sesionExpiradaNotificada = new AtomicBoolean(false);
        SesionContexto.registrarListenerExpiracion(motivo -> sesionExpiradaNotificada.set(true));

        servidorMock.createContext("/api/v1/dashboard/resumen", exchange -> {
            String errorJson = """
                    {
                      "type": "about:blank",
                      "title": "No Autorizado",
                      "status": 401,
                      "detail": "Acceso no autorizado",
                      "codigo": "NO_AUTORIZADO"
                    }
                    """;
            byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json");
            exchange.sendResponseHeaders(401, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        assertThatThrownBy(() -> cliente.obtenerResumenDashboard(PeriodoDashboard.HOY, "SUC-01"))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> {
                    ApiExcepcion ae = (ApiExcepcion) ex;
                    assertThat(ae.getStatusCode()).isEqualTo(401);
                });

        assertThat(sesionExpiradaNotificada.get()).isTrue();
    }

    @Test
    @DisplayName("obtenerResumenDashboard: 403 Forbidden lanza ApiExcepcion con status 403")
    void testObtenerResumenDashboard_403_Forbidden() {
        servidorMock.createContext("/api/v1/dashboard/resumen", exchange -> {
            String errorJson = """
                    {
                      "type": "about:blank",
                      "title": "Acceso Denegado",
                      "status": 403,
                      "detail": "Acceso denegado. Se requiere el rol TN3 (ADMINISTRADOR).",
                      "codigo": "ACCESO_DENEGADO"
                    }
                    """;
            byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json");
            exchange.sendResponseHeaders(403, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        assertThatThrownBy(() -> cliente.obtenerResumenDashboard(PeriodoDashboard.SIETE_DIAS, null))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> {
                    ApiExcepcion ae = (ApiExcepcion) ex;
                    assertThat(ae.getStatusCode()).isEqualTo(403);
                    assertThat(ae.getMessage()).contains("Acceso denegado");
                });
    }

    @Test
    @DisplayName("obtenerResumenDashboard: red caída lanza ApiExcepcion con código 0")
    void testObtenerResumenDashboard_RedCaida() {
        servidorMock.stop(0);

        assertThatThrownBy(() -> cliente.obtenerResumenDashboard(PeriodoDashboard.TREINTA_DIAS, null))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> {
                    ApiExcepcion ae = (ApiExcepcion) ex;
                    assertThat(ae.getStatusCode()).isEqualTo(0);
                    assertThat(ae.getMessage()).contains("comunicación");
                });
    }
}
