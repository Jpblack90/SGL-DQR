package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EncabezadoConexionViewModel;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EstadoConexionUI;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias del Modelo de Vista del Encabezado (EncabezadoConexionViewModel)")
class EncabezadoConexionViewModelTest {

    @Test
    @DisplayName("Sin conexión con 0 pendientes: muestra 'Sin conexión', color rojo y oculta badge")
    void testSinConexionCeroPendientes() {
        EncabezadoConexionViewModel vm = EncabezadoConexionViewModel.calcular(
                EstadoConexionUI.SIN_CONEXION, 0, null
        );

        assertThat(vm.textoEstado()).isEqualTo("Sin conexión");
        assertThat(vm.colorCirculoHex()).isEqualTo("#E30613");
        assertThat(vm.mostrarBadgePendientes()).isFalse();
        assertThat(vm.textoBadgePendientes()).isEmpty();
    }

    @Test
    @DisplayName("Sin conexión con pendientes > 0: muestra 'Sin conexión', color rojo y badge visible 'Pendientes: N'")
    void testSinConexionConPendientes() {
        EncabezadoConexionViewModel vm = EncabezadoConexionViewModel.calcular(
                EstadoConexionUI.SIN_CONEXION, 1, null
        );

        assertThat(vm.textoEstado()).isEqualTo("Sin conexión");
        assertThat(vm.colorCirculoHex()).isEqualTo("#E30613");
        assertThat(vm.mostrarBadgePendientes()).isTrue();
        assertThat(vm.textoBadgePendientes()).isEqualTo("Pendientes: 1");

        EncabezadoConexionViewModel vmMultiple = EncabezadoConexionViewModel.calcular(
                EstadoConexionUI.SIN_CONEXION, 5, ""
        );
        assertThat(vmMultiple.mostrarBadgePendientes()).isTrue();
        assertThat(vmMultiple.textoBadgePendientes()).isEqualTo("Pendientes: 5");
    }

    @Test
    @DisplayName("Sincronizando: muestra 'Sincronizando…', color azul y badge con cantidad")
    void testSincronizando() {
        EncabezadoConexionViewModel vm = EncabezadoConexionViewModel.calcular(
                EstadoConexionUI.SINCRONIZANDO, 3, null
        );

        assertThat(vm.textoEstado()).isEqualTo("Sincronizando…");
        assertThat(vm.colorCirculoHex()).isEqualTo("#3B82F6");
        assertThat(vm.mostrarBadgePendientes()).isTrue();
        assertThat(vm.textoBadgePendientes()).isEqualTo("Pendientes: 3");
    }

    @Test
    @DisplayName("En línea con 0 pendientes: muestra 'En línea', color verde y oculta badge")
    void testEnLineaCeroPendientes() {
        EncabezadoConexionViewModel vm = EncabezadoConexionViewModel.calcular(
                EstadoConexionUI.EN_LINEA, 0, null
        );

        assertThat(vm.textoEstado()).isEqualTo("En línea");
        assertThat(vm.colorCirculoHex()).isEqualTo("#10B981");
        assertThat(vm.mostrarBadgePendientes()).isFalse();
        assertThat(vm.textoBadgePendientes()).isEmpty();
    }

    @Test
    @DisplayName("En línea con notificación temporal (ej. 'Sincronizado'): muestra la notificación como texto de estado")
    void testEnLineaConNotificacion() {
        EncabezadoConexionViewModel vm = EncabezadoConexionViewModel.calcular(
                EstadoConexionUI.EN_LINEA, 0, "Sincronizado"
        );

        assertThat(vm.textoEstado()).isEqualTo("Sincronizado");
        assertThat(vm.colorCirculoHex()).isEqualTo("#10B981");
        assertThat(vm.mostrarBadgePendientes()).isFalse();
    }

    @Test
    @DisplayName("En línea con pendientes remanentes: muestra 'En línea' y badge 'Pendientes: N'")
    void testEnLineaConPendientes() {
        EncabezadoConexionViewModel vm = EncabezadoConexionViewModel.calcular(
                EstadoConexionUI.EN_LINEA, 2, ""
        );

        assertThat(vm.textoEstado()).isEqualTo("En línea");
        assertThat(vm.colorCirculoHex()).isEqualTo("#10B981");
        assertThat(vm.mostrarBadgePendientes()).isTrue();
        assertThat(vm.textoBadgePendientes()).isEqualTo("Pendientes: 2");
    }
}
