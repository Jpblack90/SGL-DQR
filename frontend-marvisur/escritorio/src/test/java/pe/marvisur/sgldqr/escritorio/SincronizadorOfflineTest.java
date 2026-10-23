package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.*;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.SyncPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.SincronizadorOffline;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;
import pe.marvisur.sgldqr.escritorio.infraestructura.sqlite.SQLiteColaAdapter;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SincronizadorOfflineTest {

    @TempDir
    Path tempDir;

    private SQLiteColaAdapter colaAdapter;
    private ServicioConexion servicioConexion;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("marvisur-sync-test.db");
        colaAdapter = new SQLiteColaAdapter(dbPath.toString());
        servicioConexion = new ServicioConexion("http://localhost:8080", Duration.ofSeconds(2), null);
        servicioConexion.reportarExitoConexion();

        UsuarioSesion sesion = new UsuarioSesion("emp-01", "Ana", "Torres", "ana@marvisur.pe", "ROLE_RECEPCIONISTA", "SUC-LIM-01", "jwt-token-valido");
        SesionContexto.establecerSesion(sesion);
    }

    @AfterEach
    void tearDown() {
        if (colaAdapter != null) {
            colaAdapter.cerrar();
        }
        SesionContexto.limpiar();
    }

    @Test
    @DisplayName("Sincronización 200 OK: marca ítems como SINCRONIZADA y limpia cola pendiente")
    void testSincronizacionExitosa200() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        colaAdapter.guardarEnCola(id1, "{\"idEncomienda\":\"" + id1 + "\",\"montoTotal\":25.0}");
        colaAdapter.guardarEnCola(id2, "{\"idEncomienda\":\"" + id2 + "\",\"montoTotal\":35.0}");

        assertThat(colaAdapter.contarPendientes()).isEqualTo(2);

        SyncPort fakeSyncPort = jsonLote -> {
            ItemResultadoSyncDto item1 = new ItemResultadoSyncDto(id1, "MV-20261022-0001", ResultadoSync.OK, null, null, "Sincronizado");
            ItemResultadoSyncDto item2 = new ItemResultadoSyncDto(id2, "MV-20261022-0002", ResultadoSync.OK, null, null, "Sincronizado");
            return new RespuestaSyncLoteDto(2, 2, 0, 0, 15, List.of(item1, item2));
        };

        SincronizadorOffline sincronizador = new SincronizadorOffline(colaAdapter, fakeSyncPort, servicioConexion);
        int exitosos = sincronizador.sincronizarLote(true);

        assertThat(exitosos).isEqualTo(2);
        assertThat(colaAdapter.contarPendientes()).isEqualTo(0);

        List<ElementoColaDto> todos = colaAdapter.listarTodos();
        assertThat(todos).allMatch(item -> item.getEstado() == EstadoElementoCola.SINCRONIZADA);
    }

    @Test
    @DisplayName("Idempotencia: ítem YA_SINCRONIZADA se marca como éxito local")
    void testSincronizacionIdempotenteYaSincronizada() {
        String id = UUID.randomUUID().toString();
        colaAdapter.guardarEnCola(id, "{\"idEncomienda\":\"" + id + "\"}");

        SyncPort fakeSyncPort = jsonLote -> {
            ItemResultadoSyncDto item = new ItemResultadoSyncDto(id, "MV-20261022-0001", ResultadoSync.YA_SINCRONIZADA, null, null, "Ya existía en servidor");
            return new RespuestaSyncLoteDto(1, 0, 1, 0, 10, List.of(item));
        };

        SincronizadorOffline sincronizador = new SincronizadorOffline(colaAdapter, fakeSyncPort, servicioConexion);
        int exitosos = sincronizador.sincronizarLote(true);

        assertThat(exitosos).isEqualTo(1);
        assertThat(colaAdapter.contarPendientes()).isEqualTo(0);

        List<ElementoColaDto> todos = colaAdapter.listarTodos();
        assertThat(todos.getFirst().getEstado()).isEqualTo(EstadoElementoCola.SINCRONIZADA);
    }

    @Test
    @DisplayName("Error de validación: se marca inmediatamente como ERROR y no se reintenta")
    void testErrorValidacionNoReintentable() {
        String id = UUID.randomUUID().toString();
        colaAdapter.guardarEnCola(id, "{\"idEncomienda\":\"" + id + "\"}");

        SyncPort fakeSyncPort = jsonLote -> {
            ItemResultadoSyncDto item = new ItemResultadoSyncDto(id, "MV-20261022-0001", ResultadoSync.ERROR, null, MotivoErrorSync.VALIDACION, "DNI inválido");
            return new RespuestaSyncLoteDto(1, 0, 0, 1, 12, List.of(item));
        };

        SincronizadorOffline sincronizador = new SincronizadorOffline(colaAdapter, fakeSyncPort, servicioConexion);
        int exitosos = sincronizador.sincronizarLote(true);

        assertThat(exitosos).isEqualTo(0);
        assertThat(colaAdapter.contarPendientes()).isEqualTo(0); // Ya no es pendiente, está en ERROR

        List<ElementoColaDto> todos = colaAdapter.listarTodos();
        ElementoColaDto itemGuardado = todos.getFirst();
        assertThat(itemGuardado.getEstado()).isEqualTo(EstadoElementoCola.ERROR);
        assertThat(itemGuardado.getUltimoError()).contains("DNI inválido");
    }

    @Test
    @DisplayName("Reintento y backoff exponencial: fórmula 2^intentos y control de espera")
    void testCalculoBackoffExponencial() {
        assertThat(SincronizadorOffline.calcularSegundosBackoff(0)).isEqualTo(0);
        assertThat(SincronizadorOffline.calcularSegundosBackoff(1)).isEqualTo(2);
        assertThat(SincronizadorOffline.calcularSegundosBackoff(2)).isEqualTo(4);
        assertThat(SincronizadorOffline.calcularSegundosBackoff(3)).isEqualTo(8);
        assertThat(SincronizadorOffline.calcularSegundosBackoff(4)).isEqualTo(16);
        assertThat(SincronizadorOffline.calcularSegundosBackoff(5)).isEqualTo(32);
        assertThat(SincronizadorOffline.calcularSegundosBackoff(10)).isEqualTo(32); // Tope de exponente 5

        LocalDateTime ahora = LocalDateTime.now();
        // Ítem que falló hace 1 segundo y tiene 1 intento (requiere esperar 2 segundos)
        ElementoColaDto itemEsperando = new ElementoColaDto(
                "id-test", "{}", EstadoElementoCola.PENDIENTE, 1, "TIMEOUT", ahora.minusMinutes(5), ahora.minusSeconds(1)
        );
        assertThat(SincronizadorOffline.debeEsperarBackoff(itemEsperando, ahora)).isTrue();

        // Ítem que falló hace 3 segundos y tiene 1 intento (espera de 2s ya transcurrió)
        ElementoColaDto itemListo = new ElementoColaDto(
                "id-test", "{}", EstadoElementoCola.PENDIENTE, 1, "TIMEOUT", ahora.minusMinutes(5), ahora.minusSeconds(3)
        );
        assertThat(SincronizadorOffline.debeEsperarBackoff(itemListo, ahora)).isFalse();
    }

    @Test
    @DisplayName("HTTP 401 Unauthorized: preserva la cola local intacta sin perder registros")
    void testError401PreservaColaLocal() {
        String id1 = UUID.randomUUID().toString();
        colaAdapter.guardarEnCola(id1, "{\"idEncomienda\":\"" + id1 + "\"}");

        AtomicBoolean sesionExpiradaNotificada = new AtomicBoolean(false);
        SesionContexto.registrarListenerExpiracion(msg -> sesionExpiradaNotificada.set(true));

        SyncPort fakeSyncPort = jsonLote -> {
            throw new ApiExcepcion(401, "NO_AUTORIZADO", "Token expirado", null);
        };

        SincronizadorOffline sincronizador = new SincronizadorOffline(colaAdapter, fakeSyncPort, servicioConexion);
        int exitosos = sincronizador.sincronizarLote(true);

        assertThat(exitosos).isEqualTo(0);
        // La cola se mantiene íntegra y pendiente
        assertThat(colaAdapter.contarPendientes()).isEqualTo(1);
        List<ElementoColaDto> pendientes = colaAdapter.listarPendientes(10);
        assertThat(pendientes.getFirst().getEstado()).isEqualTo(EstadoElementoCola.PENDIENTE);
        assertThat(sesionExpiradaNotificada.get()).isTrue();
    }
}
