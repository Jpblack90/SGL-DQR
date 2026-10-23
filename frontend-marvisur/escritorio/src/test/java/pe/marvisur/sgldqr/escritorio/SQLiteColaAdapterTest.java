package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ElementoColaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EstadoElementoCola;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.SucursalDto;
import pe.marvisur.sgldqr.escritorio.infraestructura.sqlite.SQLiteColaAdapter;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias — SQLiteColaAdapter (O1)")
class SQLiteColaAdapterTest {

    @TempDir
    Path tempDir;

    private SQLiteColaAdapter adapter;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("test-marvisur.db");
        adapter = new SQLiteColaAdapter(dbPath.toString());
    }

    @Test
    @DisplayName("Guarda y lista elementos en cola con orden FIFO")
    void testGuardarYListarPendientes() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();

        adapter.guardarEnCola(id1, "{\"item\": 1}");
        adapter.guardarEnCola(id2, "{\"item\": 2}");

        assertThat(adapter.contarPendientes()).isEqualTo(2);

        List<ElementoColaDto> pendientes = adapter.listarPendientes(10);
        assertThat(pendientes).hasSize(2);
        assertThat(pendientes.get(0).getId()).isEqualTo(id1);
        assertThat(pendientes.get(0).getEstado()).isEqualTo(EstadoElementoCola.PENDIENTE);
        assertThat(pendientes.get(1).getId()).isEqualTo(id2);
    }

    @Test
    @DisplayName("Marcar como SINCRONIZADA remueve de pendientes")
    void testMarcarSincronizada() {
        String id = UUID.randomUUID().toString();
        adapter.guardarEnCola(id, "{\"payload\":\"test\"}");

        assertThat(adapter.contarPendientes()).isEqualTo(1);

        adapter.marcarSincronizada(id);

        assertThat(adapter.contarPendientes()).isEqualTo(0);
        List<ElementoColaDto> todos = adapter.listarTodos();
        assertThat(todos).hasSize(1);
        assertThat(todos.get(0).getEstado()).isEqualTo(EstadoElementoCola.SINCRONIZADA);
    }

    @Test
    @DisplayName("Error no reintentable cambia a estado ERROR inmediatamente y no se lista en pendientes")
    void testMarcarErrorNoReintentable() {
        String id = UUID.randomUUID().toString();
        adapter.guardarEnCola(id, "{\"payload\":\"test\"}");

        adapter.marcarError(id, "VALIDACION: documento invalido", false);

        assertThat(adapter.contarPendientes()).isEqualTo(0);
        List<ElementoColaDto> pendientes = adapter.listarPendientes(10);
        assertThat(pendientes).isEmpty();

        List<ElementoColaDto> todos = adapter.listarTodos();
        assertThat(todos.get(0).getEstado()).isEqualTo(EstadoElementoCola.ERROR);
        assertThat(todos.get(0).getUltimoError()).contains("VALIDACION");
    }

    @Test
    @DisplayName("Error reintentable incrementa intentos y al llegar a 5 pasa a ERROR definitivo")
    void testMarcarErrorReintentable() {
        String id = UUID.randomUUID().toString();
        adapter.guardarEnCola(id, "{\"payload\":\"test\"}");

        // 4 intentos -> sigue PENDIENTE
        for (int i = 1; i <= 4; i++) {
            adapter.marcarError(id, "TIMEOUT intento " + i, true);
            assertThat(adapter.contarPendientes()).isEqualTo(1);
        }

        // Intento 5 -> pasa a ERROR definitivo
        adapter.marcarError(id, "TIMEOUT intento 5", true);
        assertThat(adapter.contarPendientes()).isEqualTo(0);

        List<ElementoColaDto> todos = adapter.listarTodos();
        assertThat(todos.get(0).getEstado()).isEqualTo(EstadoElementoCola.ERROR);
        assertThat(todos.get(0).getIntentos()).isEqualTo(5);
    }

    @Test
    @DisplayName("Reiniciar errores reintentables restablece los que no sean de VALIDACION")
    void testReiniciarErroresReintentables() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();

        adapter.guardarEnCola(id1, "{\"item\": 1}");
        adapter.guardarEnCola(id2, "{\"item\": 2}");

        // id1 error timeout (reintentable)
        for (int i = 0; i < 5; i++) {
            adapter.marcarError(id1, "TIMEOUT de red", true);
        }
        // id2 error validacion (no reintentable)
        adapter.marcarError(id2, "VALIDACION: ruc incorrecto", false);

        assertThat(adapter.contarPendientes()).isEqualTo(0);

        adapter.reiniciarErroresReintentables();

        // id1 debe haber vuelto a PENDIENTE, id2 debe permanecer en ERROR
        assertThat(adapter.contarPendientes()).isEqualTo(1);
        List<ElementoColaDto> pendientes = adapter.listarPendientes(10);
        assertThat(pendientes).hasSize(1);
        assertThat(pendientes.get(0).getId()).isEqualTo(id1);
    }

    @Test
    @DisplayName("Caché de sucursales se guarda y se recupera correctamente")
    void testCacheSucursales() {
        List<SucursalDto> sucursales = List.of(
                new SucursalDto("SUC-LIM-01", "Agencia Lima Central", "Lima", "Av. Grau 450"),
                new SucursalDto("SUC-HYO-01", "Agencia Huancayo", "Huancayo", "Av. Ferrocarril 123")
        );

        adapter.guardarCacheSucursales(sucursales);

        List<SucursalDto> recuperadas = adapter.obtenerCacheSucursales();
        assertThat(recuperadas).hasSize(2);
        assertThat(recuperadas.stream().anyMatch(s -> s.id().equals("SUC-LIM-01"))).isTrue();
        assertThat(recuperadas.stream().anyMatch(s -> s.id().equals("SUC-HYO-01"))).isTrue();
    }
}
