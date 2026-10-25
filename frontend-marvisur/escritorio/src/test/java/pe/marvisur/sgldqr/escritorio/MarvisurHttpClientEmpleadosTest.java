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
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarvisurHttpClientEmpleadosTest {

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
    @DisplayName("listarEmpleados: 200 OK deserializa Page JSON de Spring Data")
    void testListarEmpleados_200_Exitoso() {
        String jsonRespuesta = """
                {
                  "content": [
                    {
                      "idEmpleado": "11111111-1111-1111-1111-111111111111",
                      "dni": "12345678",
                      "nombres": "Juan",
                      "apellidos": "Perez",
                      "correo": "juan.perez@marvisur.pe",
                      "idRol": "TN1_RECEPCIONISTA",
                      "nombreRol": "Recepcionista",
                      "idSucursal": "SUC-LIM-01",
                      "nombreSucursal": "Agencia Central Lima",
                      "estado": true
                    }
                  ],
                  "totalElements": 1,
                  "totalPages": 1,
                  "size": 20,
                  "number": 0,
                  "numberOfElements": 1,
                  "first": true,
                  "last": true,
                  "empty": false
                }
                """;

        servidorMock.createContext("/api/v1/empleados", exchange -> {
            byte[] bytes = jsonRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        PaginaDto<EmpleadoDto> pagina = cliente.listarEmpleados("ACTIVOS", "TN1_RECEPCIONISTA", "SUC-LIM-01", "1234", 0, 20);

        assertThat(pagina).isNotNull();
        assertThat(pagina.totalElements()).isEqualTo(1);
        assertThat(pagina.totalPages()).isEqualTo(1);
        assertThat(pagina.content()).hasSize(1);
        EmpleadoDto emp = pagina.content().get(0);
        assertThat(emp.dni()).isEqualTo("12345678");
        assertThat(emp.nombres()).isEqualTo("Juan");
        assertThat(emp.nombreRol()).isEqualTo("Recepcionista");
        assertThat(emp.esActivo()).isTrue();
    }

    @Test
    @DisplayName("crearEmpleado: 201 Created retorna empleado creado")
    void testCrearEmpleado_201_Exitoso() {
        String jsonRespuesta = """
                {
                  "idEmpleado": "22222222-2222-2222-2222-222222222222",
                  "dni": "87654321",
                  "nombres": "Carlos",
                  "apellidos": "Gomez",
                  "correo": "carlos.gomez@marvisur.pe",
                  "idRol": "TN2_ALMACENERO",
                  "nombreRol": "Almacenero",
                  "idSucursal": "SUC-AQP-01",
                  "nombreSucursal": "Agencia Arequipa",
                  "estado": true
                }
                """;

        servidorMock.createContext("/api/v1/empleados", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            byte[] bytes = jsonRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(201, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        CrearEmpleadoRequest req = new CrearEmpleadoRequest("87654321", "Carlos", "Gomez", "carlos.gomez@marvisur.pe", "TN2_ALMACENERO", "SUC-AQP-01", "Pass1234");
        EmpleadoDto creado = cliente.crearEmpleado(req);

        assertThat(creado).isNotNull();
        assertThat(creado.dni()).isEqualTo("87654321");
        assertThat(creado.idRol()).isEqualTo("TN2_ALMACENERO");
    }

    @Test
    @DisplayName("crearEmpleado: 409 Conflict por DNI_DUPLICADO lanza ApiExcepcion con código")
    void testCrearEmpleado_409_DniDuplicado() {
        servidorMock.createContext("/api/v1/empleados", exchange -> {
            String errorJson = """
                    {
                      "type": "about:blank",
                      "title": "Conflicto",
                      "status": 409,
                      "detail": "Ya existe un empleado con el DNI proporcionado",
                      "codigo": "DNI_DUPLICADO"
                    }
                    """;
            byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json");
            exchange.sendResponseHeaders(409, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        CrearEmpleadoRequest req = new CrearEmpleadoRequest("12345678", "Juan", "Perez", "correo@marvisur.pe", "TN1_RECEPCIONISTA", "SUC-LIM-01", "Pass1234");
        assertThatThrownBy(() -> cliente.crearEmpleado(req))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> {
                    ApiExcepcion ae = (ApiExcepcion) ex;
                    assertThat(ae.getStatusCode()).isEqualTo(409);
                    assertThat(ae.getCodigoError()).isEqualTo("DNI_DUPLICADO");
                });
    }

    @Test
    @DisplayName("cambiarEstado: 409 Conflict por ULTIMO_ADMINISTRADOR")
    void testCambiarEstado_409_UltimoAdmin() {
        servidorMock.createContext("/api/v1/empleados/emp-admin/estado", exchange -> {
            String errorJson = """
                    {
                      "type": "about:blank",
                      "title": "Conflicto",
                      "status": 409,
                      "detail": "No se puede dar de baja al único administrador",
                      "codigo": "ULTIMO_ADMINISTRADOR"
                    }
                    """;
            byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/problem+json");
            exchange.sendResponseHeaders(409, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        CambiarEstadoEmpleadoRequest req = new CambiarEstadoEmpleadoRequest(false);
        assertThatThrownBy(() -> cliente.cambiarEstado("emp-admin", req))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> {
                    ApiExcepcion ae = (ApiExcepcion) ex;
                    assertThat(ae.getStatusCode()).isEqualTo(409);
                    assertThat(ae.getCodigoError()).isEqualTo("ULTIMO_ADMINISTRADOR");
                });
    }

    @Test
    @DisplayName("restablecerPassword: 204 No Content se ejecuta sin lanzar excepción")
    void testRestablecerPassword_204_Exitoso() {
        servidorMock.createContext("/api/v1/empleados/emp-1/password", exchange -> {
            assertThat(exchange.getRequestMethod()).isEqualTo("POST");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        servidorMock.start();

        RestablecerPasswordRequest req = new RestablecerPasswordRequest("NuevaClave2026");
        cliente.restablecerPassword("emp-1", req);
    }

    @Test
    @DisplayName("listarAuditoria: 200 OK con Page de ItemAuditoriaDto")
    void testListarAuditoria_200_Exitoso() {
        String jsonRespuesta = """
                {
                  "content": [
                    {
                      "idLog": "101",
                      "idEmpleado": "11111111-1111-1111-1111-111111111111",
                      "nombreEmpleado": "Juan Perez",
                      "accion": "CREACION_EMPLEADO",
                      "tablaAfectada": "empleado",
                      "idRegistroAfectado": "22222222-2222-2222-2222-222222222222",
                      "ipOrigen": "127.0.0.1",
                      "fechaHora": "2026-10-24T12:00:00-05:00"
                    }
                  ],
                  "totalElements": 1,
                  "totalPages": 1,
                  "size": 50,
                  "number": 0,
                  "numberOfElements": 1,
                  "first": true,
                  "last": true,
                  "empty": false
                }
                """;

        servidorMock.createContext("/api/v1/auditoria", exchange -> {
            assertThat(exchange.getRequestURI().getQuery()).contains("idEmpleado=1111");
            byte[] bytes = jsonRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidorMock.start();

        PaginaDto<ItemAuditoriaDto> pagina = cliente.listarAuditoria("1111", "CREACION_EMPLEADO", OffsetDateTime.now().minusDays(1), OffsetDateTime.now(), 0, 50);

        assertThat(pagina).isNotNull();
        assertThat(pagina.totalElements()).isEqualTo(1);
        assertThat(pagina.content()).hasSize(1);
        ItemAuditoriaDto audit = pagina.content().get(0);
        assertThat(audit.accion()).isEqualTo("CREACION_EMPLEADO");
        assertThat(audit.nombreEmpleado()).isEqualTo("Juan Perez");
        assertThat(audit.tablaAfectada()).isEqualTo("empleado");
    }

    @Test
    @DisplayName("listarEmpleados: 403 Forbidden lanza ApiExcepcion con status 403")
    void testListarEmpleados_403_Forbidden() {
        servidorMock.createContext("/api/v1/empleados", exchange -> {
            String errorJson = """
                    {
                      "type": "about:blank",
                      "title": "Acceso Denegado",
                      "status": 403,
                      "detail": "Acceso denegado. Se requiere rol TN3.",
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

        assertThatThrownBy(() -> cliente.listarEmpleados(null, null, null, null, 0, 20))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> assertThat(((ApiExcepcion) ex).getStatusCode()).isEqualTo(403));
    }

    @Test
    @DisplayName("listarEmpleados: 401 Not Authorized notifica expiración de sesión")
    void testListarEmpleados_401_NotAuthorized() {
        AtomicBoolean expirado = new AtomicBoolean(false);
        SesionContexto.registrarListenerExpiracion(m -> expirado.set(true));

        servidorMock.createContext("/api/v1/empleados", exchange -> {
            exchange.sendResponseHeaders(401, 0);
            exchange.close();
        });
        servidorMock.start();

        assertThatThrownBy(() -> cliente.listarEmpleados(null, null, null, null, 0, 20))
                .isInstanceOf(ApiExcepcion.class)
                .satisfies(ex -> assertThat(((ApiExcepcion) ex).getStatusCode()).isEqualTo(401));

        assertThat(expirado.get()).isTrue();
    }

    @Test
    @DisplayName("listarEmpleados: falla de red lanza excepción sin contener la palabra 'null'")
    void testListarEmpleados_FallaRed() {
        servidorMock.stop(0);

        assertThatThrownBy(() -> cliente.listarEmpleados(null, null, null, null, 0, 20))
                .isInstanceOf(RuntimeException.class)
                .hasMessageNotContaining("null");
    }
}
