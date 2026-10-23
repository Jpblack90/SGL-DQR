package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ModeloMenuRol;

import static org.assertj.core.api.Assertions.assertThat;

class ModeloMenuRolTest {

    @Test
    @DisplayName("estaAutorizado: TN1 Recepcionista tiene acceso a Ventanilla y Contingencia")
    void testPermisosTN1Recepcionista() {
        String rol = "ROLE_RECEPCIONISTA";

        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.VENTANILLA)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.ALMACEN)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.DASHBOARD)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.EMPLEADOS)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.RUTAS)).isFalse();
    }

    @Test
    @DisplayName("estaAutorizado: TN2 Almacenero tiene acceso a Almacén y Contingencia")
    void testPermisosTN2Almacenero() {
        String rol = "ROLE_ALMACENERO";

        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.VENTANILLA)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.ALMACEN)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.DASHBOARD)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.RUTAS)).isFalse();
    }

    @Test
    @DisplayName("estaAutorizado: TN3 Administrador tiene acceso a todos los módulos")
    void testPermisosTN3Administrador() {
        String rol = "ROLE_ADMINISTRADOR";

        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.VENTANILLA)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.ALMACEN)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.DASHBOARD)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.EMPLEADOS)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.RUTAS)).isTrue();
    }

    @Test
    @DisplayName("estaAutorizado: TN4 Chofer tiene acceso a Rutas")
    void testPermisosTN4Chofer() {
        String rol = "ROLE_CHOFER";

        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.VENTANILLA)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.ALMACEN)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.RUTAS)).isTrue();
    }

    @Test
    @DisplayName("estaHabilitado: VENTANILLA, ALMACEN y CONTINGENCIA están habilitados funcionalmente en D3")
    void testModulosHabilitadosFaseD3() {
        // TN1
        assertThat(ModeloMenuRol.estaHabilitado("TN1", ModeloMenuRol.ModuloMenu.VENTANILLA)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN1", ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN1", ModeloMenuRol.ModuloMenu.ALMACEN)).isFalse();

        // TN2
        assertThat(ModeloMenuRol.estaHabilitado("TN2", ModeloMenuRol.ModuloMenu.ALMACEN)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN2", ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN2", ModeloMenuRol.ModuloMenu.VENTANILLA)).isFalse();

        // TN3
        assertThat(ModeloMenuRol.estaHabilitado("TN3", ModeloMenuRol.ModuloMenu.VENTANILLA)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN3", ModeloMenuRol.ModuloMenu.ALMACEN)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN3", ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();

        // Módulos en desarrollo para fases posteriores
        assertThat(ModeloMenuRol.estaHabilitado("TN3", ModeloMenuRol.ModuloMenu.DASHBOARD)).isFalse();
        assertThat(ModeloMenuRol.estaHabilitado("TN4", ModeloMenuRol.ModuloMenu.RUTAS)).isFalse();
    }
}
