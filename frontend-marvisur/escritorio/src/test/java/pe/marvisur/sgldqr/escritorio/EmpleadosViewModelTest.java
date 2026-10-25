package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ApiExcepcion;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CrearEmpleadoRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EmpleadoDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EmpleadosViewModel;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RestablecerPasswordRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RolComboDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.SucursalDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EmpleadosViewModelTest {

    @Test
    @DisplayName("validarFormularioNuevo: campos válidos retornan lista de errores vacía")
    void testValidarFormularioNuevo_Exitoso() {
        List<String> errores = EmpleadosViewModel.validarFormularioNuevo(
                "12345678", "Juan Carlos", "Perez Gomez", "juan.perez@marvisur.pe",
                "ROL-01", "SUC-LIM-01", "Admin1234", "Admin1234"
        );
        assertThat(errores).isEmpty();
        assertThat(EmpleadosViewModel.primerError(errores)).isEmpty();
    }

    @Test
    @DisplayName("validarFormularioNuevo: valida DNI de 8 dígitos numéricos exactos")
    void testValidarFormularioNuevo_DniInvalido() {
        List<String> erroresNulo = EmpleadosViewModel.validarFormularioNuevo(
                null, "Juan", "Perez", "juan@test.com", "ROL-01", "SUC-01", "Admin1234", "Admin1234"
        );
        assertThat(erroresNulo).contains("El DNI debe tener exactamente 8 dígitos numéricos");

        List<String> erroresCorto = EmpleadosViewModel.validarFormularioNuevo(
                "1234567", "Juan", "Perez", "juan@test.com", "ROL-01", "SUC-01", "Admin1234", "Admin1234"
        );
        assertThat(erroresCorto).contains("El DNI debe tener exactamente 8 dígitos numéricos");

        List<String> erroresLetras = EmpleadosViewModel.validarFormularioNuevo(
                "1234567A", "Juan", "Perez", "juan@test.com", "ROL-01", "SUC-01", "Admin1234", "Admin1234"
        );
        assertThat(erroresLetras).contains("El DNI debe tener exactamente 8 dígitos numéricos");
    }

    @Test
    @DisplayName("validarFormularioNuevo: valida longitud de nombres y apellidos (2 a 80)")
    void testValidarFormularioNuevo_NombresApellidos() {
        List<String> nombresCortos = EmpleadosViewModel.validarFormularioNuevo(
                "12345678", "J", "Perez", "juan@test.com", "ROL-01", "SUC-01", "Admin1234", "Admin1234"
        );
        assertThat(nombresCortos).contains("Los nombres deben tener entre 2 y 80 caracteres");

        List<String> apellidosVacios = EmpleadosViewModel.validarFormularioNuevo(
                "12345678", "Juan", " ", "juan@test.com", "ROL-01", "SUC-01", "Admin1234", "Admin1234"
        );
        assertThat(apellidosVacios).contains("Los apellidos deben tener entre 2 y 80 caracteres");
    }

    @Test
    @DisplayName("validarFormularioNuevo: valida formato de correo electrónico")
    void testValidarFormularioNuevo_CorreoInvalido() {
        List<String> errores = EmpleadosViewModel.validarFormularioNuevo(
                "12345678", "Juan", "Perez", "correo-invalido", "ROL-01", "SUC-01", "Admin1234", "Admin1234"
        );
        assertThat(errores).contains("El formato de correo no es válido");
    }

    @Test
    @DisplayName("validarFormularioNuevo: valida contraseña (8 caracteres, letra, número) y confirmación")
    void testValidarFormularioNuevo_Password() {
        List<String> corta = EmpleadosViewModel.validarFormularioNuevo(
                "12345678", "Juan", "Perez", "juan@test.com", "ROL-01", "SUC-01", "pass1", "pass1"
        );
        assertThat(corta).contains("La contraseña debe tener al menos 8 caracteres y contener al menos una letra y un número");

        List<String> soloLetras = EmpleadosViewModel.validarFormularioNuevo(
                "12345678", "Juan", "Perez", "juan@test.com", "ROL-01", "SUC-01", "sololetras", "sololetras"
        );
        assertThat(soloLetras).contains("La contraseña debe tener al menos 8 caracteres y contener al menos una letra y un número");

        List<String> noCoincide = EmpleadosViewModel.validarFormularioNuevo(
                "12345678", "Juan", "Perez", "juan@test.com", "ROL-01", "SUC-01", "Admin1234", "OtraClave1234"
        );
        assertThat(noCoincide).contains("Las contraseñas no coinciden");
    }

    @Test
    @DisplayName("validarFormularioEdicion: valida nombres, apellidos, correo, rol y sucursal")
    void testValidarFormularioEdicion() {
        List<String> validos = EmpleadosViewModel.validarFormularioEdicion(
                "Carlos", "Santillana", "carlos@marvisur.pe", "ROL-02", "SUC-02"
        );
        assertThat(validos).isEmpty();

        List<String> sinRol = EmpleadosViewModel.validarFormularioEdicion(
                "Carlos", "Santillana", "carlos@marvisur.pe", null, "SUC-02"
        );
        assertThat(sinRol).contains("Debe seleccionar un rol para el colaborador");

        List<String> sinSucursal = EmpleadosViewModel.validarFormularioEdicion(
                "Carlos", "Santillana", "carlos@marvisur.pe", "ROL-02", ""
        );
        assertThat(sinSucursal).contains("Debe seleccionar una agencia para el colaborador");
    }

    @Test
    @DisplayName("validarPasswordRestablecimiento: valida longitud, caracteres y coincidencia")
    void testValidarPasswordRestablecimiento() {
        List<String> ok = EmpleadosViewModel.validarPasswordRestablecimiento("NuevaClave2026", "NuevaClave2026");
        assertThat(ok).isEmpty();

        List<String> corta = EmpleadosViewModel.validarPasswordRestablecimiento("abc1", "abc1");
        assertThat(corta).contains("La contraseña debe tener al menos 8 caracteres y contener al menos una letra y un número");

        List<String> mismatch = EmpleadosViewModel.validarPasswordRestablecimiento("NuevaClave2026", "Distinta2026");
        assertThat(mismatch).contains("Las contraseñas no coinciden");
    }

    @Test
    @DisplayName("traducirCodigoError y limpiarMensajeError: traduce códigos y elimina 'null'")
    void testTraducirCodigoErrorYLimpiarMensaje() {
        assertThat(EmpleadosViewModel.traducirCodigoError("DNI_DUPLICADO", null))
                .isEqualTo("Ya existe un empleado con ese DNI");
        assertThat(EmpleadosViewModel.traducirCodigoError("CORREO_DUPLICADO", null))
                .isEqualTo("Ya existe un empleado con ese correo");
        assertThat(EmpleadosViewModel.traducirCodigoError("ACCION_NO_PERMITIDA", null))
                .isEqualTo("No puedes realizar esta acción sobre tu propia cuenta");
        assertThat(EmpleadosViewModel.traducirCodigoError("ULTIMO_ADMINISTRADOR", null))
                .isEqualTo("Debe quedar al menos un administrador activo");
        assertThat(EmpleadosViewModel.traducirCodigoError("ACCESO_DENEGADO", null))
                .isEqualTo("No tienes permiso para esta acción");
        assertThat(EmpleadosViewModel.traducirCodigoError("SIN_CONEXION", null))
                .isEqualTo("Requiere conexión");
        assertThat(EmpleadosViewModel.traducirCodigoError("VALIDACION_FALLIDA", "DNI inválido"))
                .isEqualTo("DNI inválido");

        ApiExcepcion ex403 = new ApiExcepcion(403, "ACCESO_DENEGADO", "Forbidden", null);
        assertThat(EmpleadosViewModel.limpiarMensajeError(ex403)).isEqualTo("No tienes permiso para esta acción");

        ApiExcepcion ex401 = new ApiExcepcion(401, "NO_AUTORIZADO", "Unauthorized", null);
        assertThat(EmpleadosViewModel.limpiarMensajeError(ex401)).isEqualTo("Sesión expirada o no autorizada");

        RuntimeException exNull = new RuntimeException((String) null);
        assertThat(EmpleadosViewModel.limpiarMensajeError(exNull)).doesNotContain("null");

        assertThat(EmpleadosViewModel.limpiarMensajeError(null)).isEqualTo("Error inesperado en el módulo de personal");
    }

    @Test
    @DisplayName("convertirAInicioDiaLima y convertirAFinDiaLima: maneja zona horaria America/Lima (-05:00)")
    void testConversionFechasLima() {
        LocalDate fecha = LocalDate.of(2026, 10, 24);

        OffsetDateTime inicio = EmpleadosViewModel.convertirAInicioDiaLima(fecha);
        assertThat(inicio).isNotNull();
        assertThat(inicio.getOffset()).isEqualTo(ZoneOffset.ofHours(-5));
        assertThat(inicio.getHour()).isEqualTo(0);
        assertThat(inicio.getMinute()).isEqualTo(0);
        assertThat(inicio.getSecond()).isEqualTo(0);

        OffsetDateTime fin = EmpleadosViewModel.convertirAFinDiaLima(fecha);
        assertThat(fin).isNotNull();
        assertThat(fin.getOffset()).isEqualTo(ZoneOffset.ofHours(-5));
        assertThat(fin.getHour()).isEqualTo(23);
        assertThat(fin.getMinute()).isEqualTo(59);

        assertThat(EmpleadosViewModel.convertirAInicioDiaLima(null)).isNull();
        assertThat(EmpleadosViewModel.convertirAFinDiaLima(null)).isNull();
    }

    @Test
    @DisplayName("formatearFechaHoraLocal: maneja null y formatos con éxito")
    void testFormatearFechaHoraLocal() {
        OffsetDateTime odt = OffsetDateTime.of(2026, 10, 24, 15, 30, 0, 0, ZoneOffset.ofHours(-5));
        String formatted = EmpleadosViewModel.formatearFechaHoraLocal(odt);
        assertThat(formatted).isNotBlank();
        assertThat(formatted).doesNotContain("null");

        assertThat(EmpleadosViewModel.formatearFechaHoraLocal(null)).isEqualTo("--/--/---- --:--:--");
    }

    @Test
    @DisplayName("etiquetas, estados y permisos de botones de empleado")
    void testReglasBotonesYEstados() {
        EmpleadoDto empActivo = new EmpleadoDto("1", "12345678", "Juan", "Perez", "juan@test.com", "TN1", "Recepcionista", "SUC-01", "Lima", true);
        EmpleadoDto empInactivo = new EmpleadoDto("2", "87654321", "Maria", "Lopez", "maria@test.com", "TN2", "Almacenero", "SUC-02", "Arequipa", false);

        assertThat(EmpleadosViewModel.etiquetaEstado(true)).isEqualTo("Activo");
        assertThat(EmpleadosViewModel.etiquetaEstado(false)).isEqualTo("Baja");

        assertThat(EmpleadosViewModel.etiquetaBotonCambiarEstado(empActivo)).isEqualTo("Dar de baja");
        assertThat(EmpleadosViewModel.etiquetaBotonCambiarEstado(empInactivo)).isEqualTo("Reactivar");

        assertThat(EmpleadosViewModel.puedeEditar(empActivo)).isTrue();
        assertThat(EmpleadosViewModel.puedeEditar(null)).isFalse();
        assertThat(EmpleadosViewModel.puedeCambiarEstado(empActivo)).isTrue();
        assertThat(EmpleadosViewModel.puedeCambiarEstado(null)).isFalse();
        assertThat(EmpleadosViewModel.puedeRestablecerPassword(empActivo)).isTrue();
        assertThat(EmpleadosViewModel.puedeRestablecerPassword(null)).isFalse();

        assertThat(EmpleadosViewModel.textoPaginacion(0, 5, 100)).isEqualTo("Página 1 de 5 (Total: 100)");
        assertThat(EmpleadosViewModel.textoPaginacion(0, 0, 0)).isEqualTo("Página 0 de 0 (Total: 0)");
    }

    @Test
    @DisplayName("seguridad: CrearEmpleadoRequest y RestablecerPasswordRequest protegen contraseñas en toString()")
    void testPasswordNoExpuestaEnToString() {
        CrearEmpleadoRequest crear = new CrearEmpleadoRequest("12345678", "Juan", "Perez", "juan@test.com", "TN1_RECEPCIONISTA", "LIM01", "SuperSecreto123");
        assertThat(crear.toString()).doesNotContain("SuperSecreto123");
        assertThat(crear.toString()).contains("[PROTEGIDO]");

        RestablecerPasswordRequest reset = new RestablecerPasswordRequest("OtraClaveSecreta99");
        assertThat(reset.toString()).doesNotContain("OtraClaveSecreta99");
        assertThat(reset.toString()).contains("[PROTEGIDO]");
    }

    @Test
    @DisplayName("combos: formatearColaboradorCombo maneja wildcard null y datos completos")
    void testFormatearColaboradorCombo() {
        assertThat(EmpleadosViewModel.formatearColaboradorCombo(null))
                .isEqualTo("Todos los colaboradores");

        EmpleadoDto wildcard = new EmpleadoDto(null, null, null, null, null, null, null, null, null, true);
        assertThat(EmpleadosViewModel.formatearColaboradorCombo(wildcard))
                .isEqualTo("Todos los colaboradores");
        assertThat(wildcard.toString()).isEqualTo("Todos los colaboradores");

        EmpleadoDto empConDni = new EmpleadoDto("E-1", "71234567", "Carlos", "Mendoza", "c@m.pe", "R1", "Chofer", "S1", "Lima", true);
        assertThat(EmpleadosViewModel.formatearColaboradorCombo(empConDni))
                .isEqualTo("Carlos Mendoza (71234567)");
        assertThat(empConDni.toString()).isEqualTo("Carlos Mendoza (71234567)");

        EmpleadoDto empSinDni = new EmpleadoDto("E-2", null, "Ana", "Ruiz", "a@r.pe", "R1", "Chofer", "S1", "Lima", true);
        assertThat(EmpleadosViewModel.formatearColaboradorCombo(empSinDni))
                .isEqualTo("Ana Ruiz");
    }

    @Test
    @DisplayName("combos: formatearSucursalCombo formatea Nombre (Ciudad) evitando paréntesis vacíos")
    void testFormatearSucursalCombo() {
        assertThat(EmpleadosViewModel.formatearSucursalCombo(null))
                .isEqualTo("Todas las agencias");

        SucursalDto wildcard = new SucursalDto(null, "Todas las agencias", null, null);
        assertThat(EmpleadosViewModel.formatearSucursalCombo(wildcard))
                .isEqualTo("Todas las agencias");
        assertThat(wildcard.toString()).isEqualTo("Todas las agencias");

        SucursalDto sucConCiudad = new SucursalDto("SUC-1", "Agencia Central", "Lima", "Av. Principal 123");
        assertThat(EmpleadosViewModel.formatearSucursalCombo(sucConCiudad))
                .isEqualTo("Agencia Central (Lima)");
        assertThat(sucConCiudad.toString()).isEqualTo("Agencia Central (Lima)");

        SucursalDto sucSinCiudad = new SucursalDto("SUC-2", "Agencia Almacen", null, "Av. Industrial");
        assertThat(EmpleadosViewModel.formatearSucursalCombo(sucSinCiudad))
                .isEqualTo("Agencia Almacen");
        assertThat(sucSinCiudad.toString()).isEqualTo("Agencia Almacen");

        SucursalDto sucCiudadBlanco = new SucursalDto("SUC-3", "Agencia Norte", "   ", "Av. Norte");
        assertThat(EmpleadosViewModel.formatearSucursalCombo(sucCiudadBlanco))
                .isEqualTo("Agencia Norte");
        assertThat(sucCiudadBlanco.toString()).isEqualTo("Agencia Norte");
    }

    @Test
    @DisplayName("combos: formatearRolCombo maneja rol válido y wildcard")
    void testFormatearRolCombo() {
        assertThat(EmpleadosViewModel.formatearRolCombo(null))
                .isEqualTo("Todos los roles");
        assertThat(EmpleadosViewModel.formatearRolCombo(new RolComboDto(null, "Todos los roles")))
                .isEqualTo("Todos los roles");
        assertThat(EmpleadosViewModel.formatearRolCombo(new RolComboDto("ROL-1", "Administrador")))
                .isEqualTo("Administrador");
    }

    @Test
    @DisplayName("combos: mapeo bidireccional de etiquetas de estado y claves API")
    void testMapeoEstadosCombo() {
        assertThat(EmpleadosViewModel.mapearEstadoApi("Activos")).isEqualTo("ACTIVOS");
        assertThat(EmpleadosViewModel.mapearEstadoApi("Inactivos")).isEqualTo("INACTIVOS");
        assertThat(EmpleadosViewModel.mapearEstadoApi("Todos")).isEqualTo("TODOS");
        assertThat(EmpleadosViewModel.mapearEstadoApi(null)).isEqualTo("TODOS");
        assertThat(EmpleadosViewModel.mapearEstadoApi("DESCONOCIDO")).isEqualTo("TODOS");

        assertThat(EmpleadosViewModel.mapearEstadoEtiqueta("ACTIVOS")).isEqualTo("Activos");
        assertThat(EmpleadosViewModel.mapearEstadoEtiqueta("INACTIVOS")).isEqualTo("Inactivos");
        assertThat(EmpleadosViewModel.mapearEstadoEtiqueta("TODOS")).isEqualTo("Todos");
        assertThat(EmpleadosViewModel.mapearEstadoEtiqueta(null)).isEqualTo("Todos");
    }

    @Test
    @DisplayName("auditoria: lista de acciones incluye todas las acciones reales del backend")
    void testAccionesAuditoriaCompletas() {
        List<String> acciones = EmpleadosViewModel.ACCIONES_AUDITORIA;
        assertThat(acciones).contains(
                "TODAS",
                "LOGIN_EXITOSO", "LOGIN_FALLIDO",
                "EMPLEADO_CREADO", "EMPLEADO_ACTUALIZADO", "EMPLEADO_DADO_DE_BAJA", "EMPLEADO_REACTIVADO", "EMPLEADO_PASSWORD_RESTABLECIDA",
                "REGISTRO_ENCOMIENDA", "COBRO_CONTRA_ENTREGA", "VALIDACION_ENTREGA", "INCIDENCIA_REGISTRADA", "SYNC_ENCOMIENDAS",
                "LOTE_RECEPCION_ORIGEN", "LOTE_DESPACHO", "LOTE_RECEPCION_DESTINO",
                "VEHICULO_REGISTRADO", "VIAJE_CREADO", "VIAJE_INICIADO", "VIAJE_FINALIZADO"
        );
    }

    @Test
    @DisplayName("auditoria: mapeo amigable de etiquetas para combo y tabla")
    void testEtiquetasAccionAuditoria() {
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("TODAS")).isEqualTo("Todas las acciones");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("LOGIN_EXITOSO")).isEqualTo("Inicio de sesión exitoso");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("EMPLEADO_CREADO")).isEqualTo("Empleado registrado");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("REGISTRO_ENCOMIENDA")).isEqualTo("Registro de encomienda");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("COBRO_CONTRA_ENTREGA")).isEqualTo("Cobro contra entrega");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("VALIDACION_ENTREGA")).isEqualTo("Validación física y entrega");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("VEHICULO_REGISTRADO")).isEqualTo("Vehículo registrado");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("VIAJE_CREADO")).isEqualTo("Viaje programado");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("VIAJE_INICIADO")).isEqualTo("Viaje iniciado");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("VIAJE_FINALIZADO")).isEqualTo("Viaje finalizado");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria("ACCION_DESCONOCIDA")).isEqualTo("ACCION_DESCONOCIDA");
        assertThat(EmpleadosViewModel.etiquetaAccionAuditoria(null)).isEqualTo("Todas las acciones");
    }
}
