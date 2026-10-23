package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PestanasBloqueadasOfflineViewModel;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PestanasBloqueadasOfflineViewModel.ModuloPestana;

import static org.assertj.core.api.Assertions.assertThat;

class PestanasBloqueadasOfflineViewModelTest {

    @Test
    @DisplayName("Modo offline: solo Ventanilla está habilitada; resto muestra «Requiere conexión»")
    void testPestanasBloqueadasEnModoOffline() {
        PestanasBloqueadasOfflineViewModel vm = new PestanasBloqueadasOfflineViewModel(false);

        // 1. Ventanilla de registro siempre permitida (offline-first con Doble QR y SQLite)
        assertThat(vm.esVentanillaHabilitada()).isTrue();
        assertThat(vm.estaHabilitado(ModuloPestana.VENTANILLA_REGISTRO)).isTrue();
        assertThat(vm.obtenerMensaje(ModuloPestana.VENTANILLA_REGISTRO)).isEqualTo("Operación disponible.");

        // 2. Módulos que requieren obligatoriamente conexión central
        assertThat(vm.esCodigoExpressHabilitado()).isFalse();
        assertThat(vm.obtenerMensaje(ModuloPestana.CODIGO_EXPRESS)).contains("Requiere conexión");

        assertThat(vm.esEntregaHabilitada()).isFalse();
        assertThat(vm.obtenerMensaje(ModuloPestana.ENTREGA)).contains("Requiere conexión");

        assertThat(vm.esLotesHabilitado()).isFalse();
        assertThat(vm.obtenerMensaje(ModuloPestana.LOTES)).contains("Requiere conexión");

        assertThat(vm.esContingenciaHabilitada()).isFalse();
        assertThat(vm.obtenerMensaje(ModuloPestana.CONTINGENCIA)).contains("Requiere conexión");

        assertThat(vm.esLoginNuevoHabilitado()).isFalse();
        assertThat(vm.obtenerMensaje(ModuloPestana.LOGIN_NUEVO)).contains("Requiere conexión");
    }

    @Test
    @DisplayName("Modo online: todas las pestañas y módulos están habilitados")
    void testTodasPestanasHabilitadasEnModoOnline() {
        PestanasBloqueadasOfflineViewModel vm = new PestanasBloqueadasOfflineViewModel(true);

        assertThat(vm.esVentanillaHabilitada()).isTrue();
        assertThat(vm.esCodigoExpressHabilitado()).isTrue();
        assertThat(vm.esEntregaHabilitada()).isTrue();
        assertThat(vm.esLotesHabilitado()).isTrue();
        assertThat(vm.esContingenciaHabilitada()).isTrue();
        assertThat(vm.esLoginNuevoHabilitado()).isTrue();

        for (ModuloPestana modulo : ModuloPestana.values()) {
            assertThat(vm.estaHabilitado(modulo)).isTrue();
            assertThat(vm.obtenerMensaje(modulo)).isEqualTo("Operación disponible.");
        }
    }
}
