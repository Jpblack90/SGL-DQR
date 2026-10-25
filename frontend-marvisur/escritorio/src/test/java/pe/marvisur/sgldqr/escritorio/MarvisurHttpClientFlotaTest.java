package pe.marvisur.sgldqr.escritorio;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.*;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
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

class MarvisurHttpClientFlotaTest {

    private HttpServer servidorMock;
    private String baseUrl;
    private MarvisurHttpClient cliente;

    @BeforeEach
    void iniciarServidorMock() throws IOException {
        SesionContexto.limpiar();
        ServicioConexion.getInstancia().reportarExitoConexion();
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
        ServicioConexion.getInstancia().reportarExitoConexion();
    }

    @Test
    @DisplayName("listarVehiculos: 200 OK deserializa lista de vehículos")
    void testListarVehiculos_200_Exitoso() {
        String json = """
                [
                  {
                    "idVehiculo": "veh-01",
                    "placa": "V1A-852",
                    "modelo": "Volvo FH16",
                    "capacidadCargaKg": 25000.00
                  }
                ]
                """;

        servidorMock.createContext("/api/v1/vehiculos", exchange -> {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        List<VehiculoDto> vehiculos = cliente.listarVehiculos();

        assertThat(vehiculos).hasSize(1);
        VehiculoDto v = vehiculos.get(0);
        assertThat(v.idVehiculo()).isEqualTo("veh-01");
        assertThat(v.placa()).isEqualTo("V1A-852");
        assertThat(v.modelo()).isEqualTo("Volvo FH16");
        assertThat(v.capacidadCargaKg()).isEqualByComparingTo("25000.00");
    }

    @Test
    @DisplayName("registrarVehiculo: 201 Created retorna vehículo registrado")
    void testRegistrarVehiculo_201_Exitoso() {
        String json = """
                {
                  "idVehiculo": "veh-nuevo-01",
                  "placa": "A1B-999",
                  "modelo": "Scania R500",
                  "capacidadCargaKg": 30000.00
                }
                """;

        servidorMock.createContext("/api/v1/vehiculos", exchange -> {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(201, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        CrearVehiculoRequest req = new CrearVehiculoRequest("A1B-999", "Scania R500", new BigDecimal("30000.00"));
        VehiculoDto resp = cliente.registrarVehiculo(req);

        assertThat(resp).isNotNull();
        assertThat(resp.idVehiculo()).isEqualTo("veh-nuevo-01");
        assertThat(resp.placa()).isEqualTo("A1B-999");
        assertThat(resp.modelo()).isEqualTo("Scania R500");
    }

    @Test
    @DisplayName("listarRutas: 200 OK deserializa lista de rutas terrestres")
    void testListarRutas_200_Exitoso() {
        String json = """
                [
                  {
                    "id": "ruta-01",
                    "origen": {
                      "id": "suc-lima-01",
                      "nombreAgencia": "Agencia Lima Central"
                    },
                    "destino": {
                      "id": "suc-arequipa-01",
                      "nombreAgencia": "Agencia Arequipa Terminal"
                    },
                    "distanciaKm": 1010.50
                  }
                ]
                """;

        servidorMock.createContext("/api/v1/rutas", exchange -> {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        List<RutaDto> rutas = cliente.listarRutas();

        assertThat(rutas).hasSize(1);
        RutaDto r = rutas.get(0);
        assertThat(r.id()).isEqualTo("ruta-01");
        assertThat(r.origen().nombreAgencia()).isEqualTo("Agencia Lima Central");
        assertThat(r.destino().nombreAgencia()).isEqualTo("Agencia Arequipa Terminal");
        assertThat(r.distanciaKm()).isEqualByComparingTo("1010.50");
    }

    @Test
    @DisplayName("listarViajes: 200 OK deserializa lista de viajes con filtros")
    void testListarViajes_200_Exitoso() {
        String json = """
                [
                  {
                    "idViaje": "viaje-100",
                    "idRuta": "ruta-01",
                    "idSucursalOrigen": "suc-lima-01",
                    "idSucursalDestino": "suc-arequipa-01",
                    "distanciaKm": 1010.50,
                    "idVehiculo": "veh-01",
                    "placaVehiculo": "V1A-852",
                    "idChofer": "emp-04",
                    "nombreChofer": "Juan Chofer",
                    "fechaSalida": "2026-10-25T20:00:00-05:00",
                    "fechaLlegadaEstimada": "2026-10-26T12:00:00-05:00",
                    "estadoViaje": "PROGRAMADO",
                    "ruta": {
                      "id": "ruta-01",
                      "origen": {
                        "id": "suc-lima-01",
                        "nombreAgencia": "Agencia Lima Central"
                      },
                      "destino": {
                        "id": "suc-arequipa-01",
                        "nombreAgencia": "Agencia Arequipa Terminal"
                      },
                      "distanciaKm": 1010.50
                    },
                    "placa": "V1A-852",
                    "modelo": "Volvo FH16"
                  }
                ]
                """;

        servidorMock.createContext("/api/v1/viajes", exchange -> {
            assertThat(exchange.getRequestURI().getQuery()).contains("estado=PROGRAMADO");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        List<ViajeDto> viajes = cliente.listarViajes("PROGRAMADO", null, null);

        assertThat(viajes).hasSize(1);
        ViajeDto v = viajes.get(0);
        assertThat(v.idViaje()).isEqualTo("viaje-100");
        assertThat(v.estadoViaje()).isEqualTo("PROGRAMADO");
        assertThat(v.placa()).isEqualTo("V1A-852");
        assertThat(v.modelo()).isEqualTo("Volvo FH16");
        assertThat(v.ruta()).isNotNull();
    }

    @Test
    @DisplayName("crearViaje: 201 Created retorna viaje programado")
    void testCrearViaje_201_Exitoso() {
        String json = """
                {
                  "idViaje": "viaje-nuevo-01",
                  "idRuta": "ruta-01",
                  "idSucursalOrigen": "suc-lima-01",
                  "idSucursalDestino": "suc-arequipa-01",
                  "distanciaKm": 1010.50,
                  "idVehiculo": "veh-01",
                  "placaVehiculo": "V1A-852",
                  "idChofer": "emp-04",
                  "nombreChofer": "Juan Chofer",
                  "fechaSalida": "2026-10-25T20:00:00-05:00",
                  "fechaLlegadaEstimada": "2026-10-26T12:00:00-05:00",
                  "estadoViaje": "PROGRAMADO"
                }
                """;

        servidorMock.createContext("/api/v1/viajes", exchange -> {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(201, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        CrearViajeRequest req = new CrearViajeRequest(
                "ruta-01",
                "veh-01",
                "emp-04",
                "2026-10-25T20:00:00-05:00",
                "2026-10-26T12:00:00-05:00"
        );
        ViajeDto resp = cliente.crearViaje(req);

        assertThat(resp).isNotNull();
        assertThat(resp.idViaje()).isEqualTo("viaje-nuevo-01");
        assertThat(resp.estadoViaje()).isEqualTo("PROGRAMADO");
    }

    @Test
    @DisplayName("cambiarEstadoViaje: 200 OK actualiza estado a EN_RUTA")
    void testCambiarEstadoViaje_200_Exitoso() {
        String json = """
                {
                  "idViaje": "viaje-100",
                  "idRuta": "ruta-01",
                  "idSucursalOrigen": "suc-lima-01",
                  "idSucursalDestino": "suc-arequipa-01",
                  "distanciaKm": 1010.50,
                  "idVehiculo": "veh-01",
                  "placaVehiculo": "V1A-852",
                  "idChofer": "emp-04",
                  "nombreChofer": "Juan Chofer",
                  "fechaSalida": "2026-10-25T20:00:00-05:00",
                  "fechaLlegadaEstimada": "2026-10-26T12:00:00-05:00",
                  "estadoViaje": "EN_RUTA"
                }
                """;

        servidorMock.createContext("/api/v1/viajes/viaje-100/estado", exchange -> {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        ViajeDto resp = cliente.cambiarEstadoViaje("viaje-100", new CambiarEstadoViajeRequest("EN_RUTA"));

        assertThat(resp).isNotNull();
        assertThat(resp.estadoViaje()).isEqualTo("EN_RUTA");
    }

    @Test
    @DisplayName("obtenerManifiesto: 200 OK retorna lista de encomiendas sin datos personales")
    void testObtenerManifiesto_200_Exitoso() {
        String json = """
                [
                  {
                    "codigoComprobante": "MV-20261021-A1B2C3D4",
                    "estadoLogistico": "EN_RUTA",
                    "nombreAgenciaDestino": "Agencia Arequipa Terminal",
                    "pesoKg": 12.50
                  },
                  {
                    "codigoComprobante": "MV-20261021-E5F6G7H8",
                    "estadoLogistico": "EN_RUTA",
                    "nombreAgenciaDestino": "Agencia Arequipa Terminal",
                    "pesoKg": 8.00
                  }
                ]
                """;

        servidorMock.createContext("/api/v1/viajes/viaje-100/encomiendas", exchange -> {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        List<ManifiestoEncomiendaDto> items = cliente.obtenerManifiesto("viaje-100");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).codigoComprobante()).isEqualTo("MV-20261021-A1B2C3D4");
        assertThat(items.get(0).pesoKg()).isEqualByComparingTo("12.50");
        assertThat(items.get(1).pesoKg()).isEqualByComparingTo("8.00");
    }

    @Test
    @DisplayName("registrarVehiculo: 409 Conflict con código PLACA_DUPLICADA")
    void testRegistrarVehiculo_409_PlacaDuplicada() {
        String jsonError = """
                {
                  "type": "https://api.marvisur.pe/errores/conflicto",
                  "title": "Conflicto",
                  "status": 409,
                  "detail": "Ya existe un vehículo registrado con la placa V1A-852",
                  "codigo": "PLACA_DUPLICADA"
                }
                """;

        servidorMock.createContext("/api/v1/vehiculos", exchange -> {
            byte[] bytes = jsonError.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json; charset=utf-8");
            exchange.sendResponseHeaders(409, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        assertThatThrownBy(() -> cliente.registrarVehiculo(new CrearVehiculoRequest("V1A-852", "Volvo", new BigDecimal("1000"))))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> {
                    ApiExcepcion ae = (ApiExcepcion) ex;
                    assertThat(ae.getStatusCode()).isEqualTo(409);
                    assertThat(ae.getCodigoError()).isEqualTo("PLACA_DUPLICADA");
                });
    }

    @Test
    @DisplayName("crearViaje: 409 Conflict con códigos CHOFER_INVALIDO, CHOFER_OCUPADO y VEHICULO_OCUPADO")
    void testCrearViaje_409_Conflictos() {
        String jsonError = """
                {
                  "status": 409,
                  "detail": "El chofer seleccionado ya tiene un viaje asignado en ruta",
                  "codigo": "CHOFER_OCUPADO"
                }
                """;

        servidorMock.createContext("/api/v1/viajes", exchange -> {
            byte[] bytes = jsonError.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json; charset=utf-8");
            exchange.sendResponseHeaders(409, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        CrearViajeRequest req = new CrearViajeRequest("r1", "v1", "c1", "2026-10-25T20:00:00-05:00", "2026-10-26T12:00:00-05:00");
        assertThatThrownBy(() -> cliente.crearViaje(req))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> {
                    ApiExcepcion ae = (ApiExcepcion) ex;
                    assertThat(ae.getStatusCode()).isEqualTo(409);
                    assertThat(ae.getCodigoError()).isEqualTo("CHOFER_OCUPADO");
                });
    }

    @Test
    @DisplayName("cambiarEstadoViaje: 409 Conflict con TRANSICION_INVALIDA")
    void testCambiarEstadoViaje_409_TransicionInvalida() {
        String jsonError = """
                {
                  "status": 409,
                  "detail": "No se puede pasar directamente de PROGRAMADO a FINALIZADO",
                  "codigo": "TRANSICION_INVALIDA"
                }
                """;

        servidorMock.createContext("/api/v1/viajes/viaje-100/estado", exchange -> {
            byte[] bytes = jsonError.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json; charset=utf-8");
            exchange.sendResponseHeaders(409, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        assertThatThrownBy(() -> cliente.cambiarEstadoViaje("viaje-100", new CambiarEstadoViajeRequest("FINALIZADO")))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> {
                    ApiExcepcion ae = (ApiExcepcion) ex;
                    assertThat(ae.getStatusCode()).isEqualTo(409);
                    assertThat(ae.getCodigoError()).isEqualTo("TRANSICION_INVALIDA");
                });
    }

    @Test
    @DisplayName("crearViaje: 403 Forbidden para rol no autorizado")
    void testCrearViaje_403_Forbidden() {
        servidorMock.createContext("/api/v1/viajes", exchange -> {
            String jsonError = "{\"status\":403,\"detail\":\"Acceso denegado\",\"codigo\":\"ACCESO_DENEGADO\"}";
            byte[] bytes = jsonError.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json; charset=utf-8");
            exchange.sendResponseHeaders(403, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        CrearViajeRequest req = new CrearViajeRequest("r1", "v1", "c1", "2026-10-25T20:00:00-05:00", "2026-10-26T12:00:00-05:00");
        assertThatThrownBy(() -> cliente.crearViaje(req))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> assertThat(((ApiExcepcion) ex).getStatusCode()).isEqualTo(403));
    }

    @Test
    @DisplayName("listarViajes: 401 Unauthorized lanza ApiExcepcion 401")
    void testListarViajes_401_Unauthorized() {
        servidorMock.createContext("/api/v1/viajes", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
        servidorMock.start();

        assertThatThrownBy(() -> cliente.listarViajes("TODOS", null, null))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> assertThat(((ApiExcepcion) ex).getStatusCode()).isEqualTo(401));
    }

    @Test
    @DisplayName("errorRed: Lanza ApiExcepcion con código SIN_CONEXION si servidor inalcanzable")
    void testFalloRed_ServidorInalcanzable() {
        // Apagamos servidor para simular fallo de red
        servidorMock.stop(0);

        assertThatThrownBy(() -> cliente.listarVehiculos())
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> {
                    ApiExcepcion ae = (ApiExcepcion) ex;
                    assertThat(ae.getCodigoError()).isEqualTo("SIN_CONEXION");
                });
    }
}
