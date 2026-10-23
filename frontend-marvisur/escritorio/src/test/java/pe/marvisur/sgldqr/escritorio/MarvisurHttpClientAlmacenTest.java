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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarvisurHttpClientAlmacenTest {

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

        // Simulamos usuario autenticado TN2 en el contexto
        SesionContexto.establecerSesion(new UsuarioSesion(
                "token-jwt-almacenero",
                "EMP-002",
                "Carlos",
                "Almacenero",
                "carlos@marvisur.pe",
                "ROLE_ALMACENERO",
                "SUC-LIM-01"
        ));
    }

    @AfterEach
    void detenerServidorMock() {
        if (servidorMock != null) {
            servidorMock.stop(0);
        }
        SesionContexto.limpiar();
    }

    @Test
    @DisplayName("validarEntrega: 200 OK devuelve resultado EXITOSO con datos de entrega")
    void testValidarEntregaExitoso() {
        servidorMock.createContext("/api/v1/entregas/validar", exchange -> {
            String respuesta = """
                    {
                      "idEncomienda": "UUID-ENC-001",
                      "codigoComprobante": "T001-00000001",
                      "estado": "ENTREGADA",
                      "fechaHora": "2026-10-22T10:00:00",
                      "remitente": "Ana Rojas",
                      "destinatario": "Carlos Torres",
                      "mensaje": "Entrega validada y completada con éxito"
                    }
                    """;
            responderJson(exchange, 200, respuesta);
        });
        servidorMock.start();

        ValidacionEntregaResultado resultado = cliente.validarEntrega("QR1-DEMO-001", "QR2-DEMO-001");

        assertThat(resultado.tipo()).isEqualTo(ValidacionEntregaResultado.TipoValidacion.EXITOSO);
        assertThat(resultado.idEncomienda()).isEqualTo("UUID-ENC-001");
        assertThat(resultado.codigoComprobante()).isEqualTo("T001-00000001");
        assertThat(resultado.latenciaMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("validarEntrega: 409 ENTREGA_BLOQUEADA_PAGO_PENDIENTE devuelve PAGO_PENDIENTE con montos")
    void testValidarEntregaBloqueadaPagoPendiente() {
        servidorMock.createContext("/api/v1/entregas/validar", exchange -> {
            String respuesta = """
                    {
                      "type": "about:blank",
                      "title": "ENTREGA_BLOQUEADA_PAGO_PENDIENTE",
                      "status": 409,
                      "detail": "La encomienda requiere pago Contra Entrega antes de autorizar el retiro",
                      "idEncomienda": "UUID-ENC-002",
                      "codigoComprobante": "T001-00000002",
                      "montoTotal": 45.50
                    }
                    """;
            responderJson(exchange, 409, respuesta);
        });
        servidorMock.start();

        ValidacionEntregaResultado resultado = cliente.validarEntrega("QR1-DEMO-002", "QR2-DEMO-002");

        assertThat(resultado.tipo()).isEqualTo(ValidacionEntregaResultado.TipoValidacion.PAGO_PENDIENTE);
        assertThat(resultado.idEncomienda()).isEqualTo("UUID-ENC-002");
        assertThat(resultado.codigoComprobante()).isEqualTo("T001-00000002");
        assertThat(resultado.montoPendiente()).isEqualByComparingTo("45.50");
        assertThat(resultado.mensaje()).contains("Contra Entrega");
    }

    @Test
    @DisplayName("validarEntrega: 409 TOKEN_QR2_INVALIDO_O_EXPIRADO devuelve TOKEN_INVALIDO")
    void testValidarEntregaTokenInvalido() {
        servidorMock.createContext("/api/v1/entregas/validar", exchange -> {
            String respuesta = """
                    {
                      "title": "TOKEN_QR2_INVALIDO_O_EXPIRADO",
                      "status": 409,
                      "detail": "El código QR2 no corresponde o ya fue utilizado"
                    }
                    """;
            responderJson(exchange, 409, respuesta);
        });
        servidorMock.start();

        ValidacionEntregaResultado resultado = cliente.validarEntrega("QR1-DEMO-001", "QR2-INVALIDO");

        assertThat(resultado.tipo()).isEqualTo(ValidacionEntregaResultado.TipoValidacion.TOKEN_INVALIDO);
        assertThat(resultado.mensaje()).contains("no corresponde o ya fue utilizado");
    }

    @Test
    @DisplayName("validarEntrega: 409 TRANSICION_ESTADO_INVALIDA devuelve TRANSICION_INVALIDA")
    void testValidarEntregaTransicionInvalida() {
        servidorMock.createContext("/api/v1/entregas/validar", exchange -> {
            String respuesta = """
                    {
                      "title": "TRANSICION_ESTADO_INVALIDA",
                      "status": 409,
                      "detail": "La encomienda no se encuentra en estado EN_ALMACEN_DESTINO"
                    }
                    """;
            responderJson(exchange, 409, respuesta);
        });
        servidorMock.start();

        ValidacionEntregaResultado resultado = cliente.validarEntrega("QR1-DEMO-001", "QR2-DEMO-001");

        assertThat(resultado.tipo()).isEqualTo(ValidacionEntregaResultado.TipoValidacion.TRANSICION_INVALIDA);
        assertThat(resultado.mensaje()).contains("EN_ALMACEN_DESTINO");
    }

    @Test
    @DisplayName("validarEntrega: 404 NOT FOUND devuelve NO_ENCONTRADO")
    void testValidarEntregaNoEncontrada() {
        servidorMock.createContext("/api/v1/entregas/validar", exchange -> {
            String respuesta = """
                    {
                      "title": "ENCOMIENDA_NO_ENCONTRADA",
                      "status": 404,
                      "detail": "No existe encomienda vinculada al QR1 escaneado"
                    }
                    """;
            responderJson(exchange, 404, respuesta);
        });
        servidorMock.start();

        ValidacionEntregaResultado resultado = cliente.validarEntrega("QR1-INEXISTENTE", "QR2-DEMO-001");

        assertThat(resultado.tipo()).isEqualTo(ValidacionEntregaResultado.TipoValidacion.NO_ENCONTRADO);
    }

    @Test
    @DisplayName("validarEntrega: 403 FORBIDDEN lanza ApiExcepcion")
    void testValidarEntregaForbidden403() {
        servidorMock.createContext("/api/v1/entregas/validar", exchange -> {
            responderJson(exchange, 403, "{\"title\":\"Forbidden\",\"status\":403}");
        });
        servidorMock.start();

        assertThatThrownBy(() -> cliente.validarEntrega("QR1-DEMO-001", "QR2-DEMO-001"))
                .isInstanceOf(ApiExcepcion.class)
                .hasMessageContaining("permiso");
    }

    @Test
    @DisplayName("validarEntrega: fallo de red / desconexión lanza ApiExcepcion")
    void testValidarEntregaFallaConexion() {
        servidorMock.stop(0); // Servidor caído

        assertThatThrownBy(() -> cliente.validarEntrega("QR1-DEMO-001", "QR2-DEMO-001"))
                .isInstanceOf(ApiExcepcion.class)
                .hasMessageContaining("comunicación");
    }

    @Test
    @DisplayName("registrarCobro: 200 OK devuelve RespuestaCobroDto con PAGADO")
    void testRegistrarCobroExitoso() {
        servidorMock.createContext("/api/v1/encomiendas/UUID-ENC-002/cobro", exchange -> {
            String respuesta = """
                    {
                      "idEncomienda": "UUID-ENC-002",
                      "codigoComprobante": "T001-00000002",
                      "montoTotal": 45.50,
                      "estadoPago": "PAGADO",
                      "estadoLogistico": "EN_ALMACEN_DESTINO",
                      "mensaje": "Cobro registrado exitosamente",
                      "fechaCobro": "2026-10-22T10:05:00"
                    }
                    """;
            responderJson(exchange, 200, respuesta);
        });
        servidorMock.start();

        RespuestaCobroDto respuesta = cliente.registrarCobro("UUID-ENC-002");

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.idEncomienda()).isEqualTo("UUID-ENC-002");
        assertThat(respuesta.codigoComprobante()).isEqualTo("T001-00000002");
        assertThat(respuesta.estadoPago()).isEqualTo("PAGADO");
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("45.50");
    }

    @Test
    @DisplayName("listarViajes: 200 OK devuelve lista de ViajeDto filtrados")
    void testListarViajesExitoso() {
        servidorMock.createContext("/api/v1/viajes", exchange -> {
            String respuesta = """
                    [
                      {
                        "idViaje": "VIAJE-001",
                        "idRuta": "RUTA-LIM-HYO",
                        "idSucursalOrigen": "SUC-LIM-01",
                        "idSucursalDestino": "SUC-HYO-01",
                        "distanciaKm": 310.5,
                        "idVehiculo": "VEH-01",
                        "placaVehiculo": "ABC-123",
                        "idChofer": "EMP-CHO-01",
                        "nombreChofer": "Mario Chofer",
                        "fechaSalida": "2026-10-22T20:00:00",
                        "fechaLlegadaEstimada": "2026-10-23T04:00:00",
                        "estadoViaje": "PROGRAMADO"
                      }
                    ]
                    """;
            responderJson(exchange, 200, respuesta);
        });
        servidorMock.start();

        List<ViajeDto> viajes = cliente.listarViajes("PROGRAMADO", "SUC-LIM-01");

        assertThat(viajes).hasSize(1);
        ViajeDto v = viajes.get(0);
        assertThat(v.idViaje()).isEqualTo("VIAJE-001");
        assertThat(v.placaVehiculo()).isEqualTo("ABC-123");
        assertThat(v.idSucursalDestino()).isEqualTo("SUC-HYO-01");
    }

    @Test
    @DisplayName("procesarLote: 200 OK procesa masivamente y devuelve conteos detallados")
    void testProcesarLoteExitoso() {
        servidorMock.createContext("/api/v1/almacen/lotes/escaneo", exchange -> {
            String respuesta = """
                    {
                      "totalProcesados": 3,
                      "totalExitosos": 2,
                      "totalYaRegistrados": 1,
                      "totalErrores": 0,
                      "tiempoProcesamientoMs": 42,
                      "detalles": [
                        {"qr1": "QR1-001", "resultado": "OK", "motivo": "Despachado exitosamente"},
                        {"qr1": "QR1-002", "resultado": "OK", "motivo": "Despachado exitosamente"},
                        {"qr1": "QR1-003", "resultado": "YA_REGISTRADA", "motivo": "Ya se encontraba en viaje"}
                      ]
                    }
                    """;
            responderJson(exchange, 200, respuesta);
        });
        servidorMock.start();

        RespuestaLoteEscaneoDto respuesta = cliente.procesarLote(
                new LoteEscaneoComando("DESPACHAR_A_VIAJE", "VIAJE-001", List.of("QR1-001", "QR1-002", "QR1-003"))
        );

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.totalProcesados()).isEqualTo(3);
        assertThat(respuesta.totalExitosos()).isEqualTo(2);
        assertThat(respuesta.totalYaRegistrados()).isEqualTo(1);
        assertThat(respuesta.detalles()).hasSize(3);
    }

    @Test
    @DisplayName("buscarEncomiendasPorDocumento: 200 OK devuelve encomiendas para contingencia")
    void testBuscarEncomiendasPorDocumentoExitoso() {
        servidorMock.createContext("/api/v1/almacen/encomiendas/buscar", exchange -> {
            String respuesta = """
                    [
                      {
                        "id": "ENC-001",
                        "codigoComprobante": "T001-00000001",
                        "tipoDocumentoRemitente": "DNI",
                        "numeroDocumentoRemitente": "45678901",
                        "nombreRemitente": "Juan Perez",
                        "tipoDocumentoDestinatario": "DNI",
                        "numeroDocumentoDestinatario": "78945612",
                        "nombreDestinatario": "Maria Lopez",
                        "idSucursalOrigen": "SUC-LIM-01",
                        "idSucursalDestino": "SUC-HYO-01",
                        "pesoKg": 5.0,
                        "precioTotal": 25.00,
                        "estadoPago": "PAGADO",
                        "estadoLogistico": "EN_ALMACEN_DESTINO",
                        "fechaRegistro": "2026-10-22T09:00:00"
                      }
                    ]
                    """;
            responderJson(exchange, 200, respuesta);
        });
        servidorMock.start();

        List<EncomiendaBusquedaDto> resultados = cliente.buscarEncomiendasPorDocumento("45678901");

        assertThat(resultados).hasSize(1);
        EncomiendaBusquedaDto enc = resultados.get(0);
        assertThat(enc.codigoComprobante()).isEqualTo("T001-00000001");
        assertThat(enc.nombreRemitente()).isEqualTo("Juan Perez");
        assertThat(enc.estadoLogistico()).isEqualTo("EN_ALMACEN_DESTINO");
    }

    @Test
    @DisplayName("reportarIncidencia: 201 CREATED registra incidencia y devuelve QR1 regenerado en Base64")
    void testReportarIncidenciaExitoso() {
        servidorMock.createContext("/api/v1/almacen/incidencias", exchange -> {
            String respuesta = """
                    {
                      "idIncidencia": "INC-001",
                      "idEncomienda": "ENC-001",
                      "idEmpleadoReporta": "EMP-002",
                      "tipoIncidencia": "ETIQUETA_DANADA",
                      "motivoReimpresion": "Etiqueta desgastada por lluvia",
                      "fechaIncidencia": "2026-10-22T10:30:00",
                      "qr1Base64": "iVBORw0KGgoAAAANSUhEUgAAASwAAAEs..."
                    }
                    """;
            responderJson(exchange, 201, respuesta);
        });
        servidorMock.start();

        RespuestaIncidenciaDto respuesta = cliente.reportarIncidencia(
                new RegistrarIncidenciaComando("ENC-001", "ETIQUETA_DANADA", "Etiqueta desgastada por lluvia")
        );

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.idIncidencia()).isEqualTo("INC-001");
        assertThat(respuesta.tipoIncidencia()).isEqualTo("ETIQUETA_DANADA");
        assertThat(respuesta.qr1Base64()).startsWith("iVBOR");
    }

    @Test
    @DisplayName("listarIncidencias: 200 OK devuelve historial de incidencias previas")
    void testListarIncidenciasExitoso() {
        servidorMock.createContext("/api/v1/almacen/incidencias", exchange -> {
            String respuesta = """
                    [
                      {
                        "idIncidencia": "INC-001",
                        "idEncomienda": "ENC-001",
                        "idEmpleadoReporta": "EMP-002",
                        "tipoIncidencia": "ETIQUETA_DANADA",
                        "motivoReimpresion": "Reimpresión solicitada",
                        "fechaIncidencia": "2026-10-22T10:30:00"
                      }
                    ]
                    """;
            responderJson(exchange, 200, respuesta);
        });
        servidorMock.start();

        List<IncidenciaHistorialDto> incidencias = cliente.listarIncidencias("ENC-001");

        assertThat(incidencias).hasSize(1);
        IncidenciaHistorialDto inc = incidencias.get(0);
        assertThat(inc.idIncidencia()).isEqualTo("INC-001");
        assertThat(inc.tipoIncidencia()).isEqualTo("ETIQUETA_DANADA");
    }

    private void responderJson(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String cuerpoJson) throws IOException {
        byte[] bytes = cuerpoJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
