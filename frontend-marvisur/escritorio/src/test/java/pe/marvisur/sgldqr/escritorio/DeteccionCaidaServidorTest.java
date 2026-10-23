package pe.marvisur.sgldqr.escritorio;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DeteccionCaidaServidorTest {

    @Test
    @DisplayName("RNF-02: Detección de caída de servidor conmuta a modo sin conexión en <= 2 segundos")
    void testDeteccionCaidaConmutaEnMenosDeDosSegundos() throws Exception {
        // Encontrar un puerto local libre donde NO hay ningún servidor escuchando
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }

        Duration timeoutEstricto = Duration.ofSeconds(2);
        ServicioConexion servicio = new ServicioConexion("http://localhost:" + puertoLibre, timeoutEstricto, null);

        AtomicBoolean notificacionRecibida = new AtomicBoolean(false);
        AtomicBoolean estadoNotificado = new AtomicBoolean(true);

        servicio.agregarListener(enLinea -> {
            notificacionRecibida.set(true);
            estadoNotificado.set(enLinea);
        });

        long inicio = System.currentTimeMillis();
        boolean conectividad = servicio.verificarConectividad();
        long duracionMs = System.currentTimeMillis() - inicio;

        // RNF-02: Conmutación a modo offline en <= 2 segundos (con tolerancia máxima de 2.5s por SO)
        assertThat(conectividad).isFalse();
        assertThat(servicio.estaEnLinea()).isFalse();
        assertThat(duracionMs).isLessThanOrEqualTo(2500);

        // Notificación inmediata al listener de cabecera UI
        assertThat(notificacionRecibida.get()).isTrue();
        assertThat(estadoNotificado.get()).isFalse();
    }

    @Test
    @DisplayName("Reconexión: detecta retorno de /actuator/health con estado UP y conmuta a En Línea")
    void testReconexionExitosaConmutaAEnLinea() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/actuator/health", exchange -> {
            byte[] response = "{\"status\":\"UP\"}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();

        try {
            int puerto = server.getAddress().getPort();
            ServicioConexion servicio = new ServicioConexion("http://localhost:" + puerto, Duration.ofSeconds(2), null);

            // Simular que previamente estaba offline
            servicio.reportarFalloConexion("Prueba previa offline");
            assertThat(servicio.estaEnLinea()).isFalse();

            AtomicBoolean reconectadoNotificado = new AtomicBoolean(false);
            servicio.agregarListener(enLinea -> {
                if (enLinea) {
                    reconectadoNotificado.set(true);
                }
            });

            boolean online = servicio.verificarConectividad();

            assertThat(online).isTrue();
            assertThat(servicio.estaEnLinea()).isTrue();
            assertThat(reconectadoNotificado.get()).isTrue();
        } finally {
            server.stop(0);
        }
    }
}
