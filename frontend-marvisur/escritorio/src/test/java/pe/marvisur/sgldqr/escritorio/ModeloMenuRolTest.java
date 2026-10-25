package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ModeloMenuRol;

import static org.assertj.core.api.Assertions.assertThat;

class ModeloMenuRolTest {

    @Test
    @DisplayName("estaAutorizado: TN1 Recepcionista tiene acceso a Ventanilla, Contingencia y Cola Local")
    void testPermisosTN1Recepcionista() {
        String rol = "ROLE_RECEPCIONISTA";

        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.VENTANILLA)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.COLA_LOCAL)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.ALMACEN)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.DASHBOARD)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.EMPLEADOS)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.RUTAS)).isFalse();
    }

    @Test
    @DisplayName("estaAutorizado: TN2 Almacenero tiene acceso a Almacén y Contingencia (sin acceso a Cola Local)")
    void testPermisosTN2Almacenero() {
        String rol = "ROLE_ALMACENERO";

        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.VENTANILLA)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.COLA_LOCAL)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.ALMACEN)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.DASHBOARD)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.RUTAS)).isFalse();
    }

    @Test
    @DisplayName("estaAutorizado: TN3 Administrador tiene acceso a todos los módulos incluyendo Cola Local")
    void testPermisosTN3Administrador() {
        String rol = "ROLE_ADMINISTRADOR";

        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.VENTANILLA)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.ALMACEN)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.COLA_LOCAL)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.DASHBOARD)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.EMPLEADOS)).isTrue();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.RUTAS)).isTrue();
    }

    @Test
    @DisplayName("estaAutorizado: TN4 Chofer tiene acceso a Rutas")
    void testPermisosTN4Chofer() {
        String rol = "ROLE_CHOFER";

        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.VENTANILLA)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.COLA_LOCAL)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.ALMACEN)).isFalse();
        assertThat(ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.RUTAS)).isTrue();
    }

    @Test
    @DisplayName("estaHabilitado: VENTANILLA, ALMACEN, CONTINGENCIA, COLA_LOCAL y DASHBOARD (TN3) habilitados")
    void testModulosHabilitadosFaseD3yOffline() {
        // TN1
        assertThat(ModeloMenuRol.estaHabilitado("TN1", ModeloMenuRol.ModuloMenu.VENTANILLA)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN1", ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN1", ModeloMenuRol.ModuloMenu.COLA_LOCAL)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN1", ModeloMenuRol.ModuloMenu.ALMACEN)).isFalse();

        // TN2
        assertThat(ModeloMenuRol.estaHabilitado("TN2", ModeloMenuRol.ModuloMenu.ALMACEN)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN2", ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN2", ModeloMenuRol.ModuloMenu.COLA_LOCAL)).isFalse();
        assertThat(ModeloMenuRol.estaHabilitado("TN2", ModeloMenuRol.ModuloMenu.VENTANILLA)).isFalse();

        // TN3
        assertThat(ModeloMenuRol.estaHabilitado("TN3", ModeloMenuRol.ModuloMenu.VENTANILLA)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN3", ModeloMenuRol.ModuloMenu.ALMACEN)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN3", ModeloMenuRol.ModuloMenu.CONTINGENCIA)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN3", ModeloMenuRol.ModuloMenu.COLA_LOCAL)).isTrue();

        // Módulos en desarrollo para fases posteriores
        assertThat(ModeloMenuRol.estaHabilitado("TN3", ModeloMenuRol.ModuloMenu.DASHBOARD)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN1", ModeloMenuRol.ModuloMenu.DASHBOARD)).isFalse();
        assertThat(ModeloMenuRol.estaHabilitado("TN2", ModeloMenuRol.ModuloMenu.DASHBOARD)).isFalse();
        assertThat(ModeloMenuRol.estaHabilitado("TN4", ModeloMenuRol.ModuloMenu.DASHBOARD)).isFalse();
        assertThat(ModeloMenuRol.estaHabilitado("TN4", ModeloMenuRol.ModuloMenu.RUTAS)).isFalse();

        // Módulo EMPLEADOS habilitado exclusivamente para TN3 Administrador
        assertThat(ModeloMenuRol.estaHabilitado("TN3", ModeloMenuRol.ModuloMenu.EMPLEADOS)).isTrue();
        assertThat(ModeloMenuRol.estaHabilitado("TN1", ModeloMenuRol.ModuloMenu.EMPLEADOS)).isFalse();
        assertThat(ModeloMenuRol.estaHabilitado("TN2", ModeloMenuRol.ModuloMenu.EMPLEADOS)).isFalse();
        assertThat(ModeloMenuRol.estaHabilitado("TN4", ModeloMenuRol.ModuloMenu.EMPLEADOS)).isFalse();
    }
}
