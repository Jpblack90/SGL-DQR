package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Lógica de presentación y validación desacoplada para la gestión de empleados y auditoría (RF-03).
 * Puede probarse de forma headless sin inicializar el entorno gráfico de JavaFX.
 */
public final class EmpleadosViewModel {

    public static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Pattern PATRON_DNI = Pattern.compile("^\\d{8}$");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PATRON_PASSWORD = Pattern.compile("^(?=.*[a-zA-Z])(?=.*\\d).{8,}$");

    public static final List<String> ACCIONES_AUDITORIA = List.of(
            "TODAS",
            "LOGIN_EXITOSO",
            "LOGIN_FALLIDO",
            "EMPLEADO_CREADO",
            "EMPLEADO_ACTUALIZADO",
            "EMPLEADO_DADO_DE_BAJA",
            "EMPLEADO_REACTIVADO",
            "EMPLEADO_PASSWORD_RESTABLECIDA",
            "REGISTRO_ENCOMIENDA",
            "ENTREGA_VALIDADA",
            "COBRO_REGISTRADO",
            "LOTE_RECEPCION_ORIGEN",
            "LOTE_DESPACHO",
            "LOTE_RECEPCION_DESTINO",
            "INCIDENCIA_REGISTRADA",
            "SYNC_ENCOMIENDAS"
    );

    public static final List<String> ESTADOS_FILTRO = List.of(
            "ACTIVOS",
            "INACTIVOS",
            "TODOS"
    );

    public static final List<String> ESTADOS_FILTRO_ETIQUETAS = List.of(
            "Activos",
            "Inactivos",
            "Todos"
    );

    public static String mapearEstadoApi(String etiqueta) {
        if (etiqueta == null) return "TODOS";
        return switch (etiqueta.trim().toUpperCase()) {
            case "ACTIVOS", "ACTIVO" -> "ACTIVOS";
            case "INACTIVOS", "INACTIVO" -> "INACTIVOS";
            default -> "TODOS";
        };
    }

    public static String mapearEstadoEtiqueta(String claveApi) {
        if (claveApi == null) return "Todos";
        return switch (claveApi.trim().toUpperCase()) {
            case "ACTIVOS", "ACTIVO" -> "Activos";
            case "INACTIVOS", "INACTIVO" -> "Inactivos";
            default -> "Todos";
        };
    }

    public static String formatearColaboradorCombo(EmpleadoDto emp) {
        if (emp == null || emp.idEmpleado() == null || emp.idEmpleado().isBlank()) {
            return "Todos los colaboradores";
        }
        String nombre = emp.nombreCompleto().trim();
        if (emp.dni() != null && !emp.dni().isBlank()) {
            return nombre + " (" + emp.dni().trim() + ")";
        }
        return nombre;
    }

    public static String formatearSucursalCombo(SucursalDto s) {
        if (s == null || s.id() == null || s.id().isBlank()) {
            return "Todas las agencias";
        }
        String nombre = s.nombreAgencia() != null ? s.nombreAgencia().trim() : "";
        String ciudad = s.ciudad() != null ? s.ciudad().trim() : "";
        if (!ciudad.isEmpty()) {
            return nombre + " (" + ciudad + ")";
        }
        return nombre;
    }

    public static String formatearRolCombo(RolComboDto r) {
        if (r == null || r.id() == null || r.id().isBlank()) {
            return "Todos los roles";
        }
        return r.nombre() != null ? r.nombre().trim() : "";
    }

    private EmpleadosViewModel() {
    }

    // =========================================================================
    // VALIDACIONES LOCALES DE FORMULARIO (RF-03)
    // =========================================================================

    public static List<String> validarFormularioNuevo(String dni, String nombres, String apellidos, String correo,
                                                      String idRol, String idSucursal, String password, String confirmacion) {
        List<String> errores = new ArrayList<>();

        if (dni == null || !PATRON_DNI.matcher(dni.trim()).matches()) {
            errores.add("El DNI debe tener exactamente 8 dígitos numéricos");
        }
        validarCamposComunes(nombres, apellidos, correo, idRol, idSucursal, errores);

        if (password == null || !PATRON_PASSWORD.matcher(password).matches()) {
            errores.add("La contraseña debe tener al menos 8 caracteres y contener al menos una letra y un número");
        } else if (!password.equals(confirmacion)) {
            errores.add("Las contraseñas no coinciden");
        }

        return errores;
    }

    public static List<String> validarFormularioEdicion(String nombres, String apellidos, String correo,
                                                        String idRol, String idSucursal) {
        List<String> errores = new ArrayList<>();
        validarCamposComunes(nombres, apellidos, correo, idRol, idSucursal, errores);
        return errores;
    }

    public static List<String> validarPasswordRestablecimiento(String nuevaPassword, String confirmacion) {
        List<String> errores = new ArrayList<>();
        if (nuevaPassword == null || !PATRON_PASSWORD.matcher(nuevaPassword).matches()) {
            errores.add("La contraseña debe tener al menos 8 caracteres y contener al menos una letra y un número");
        } else if (!nuevaPassword.equals(confirmacion)) {
            errores.add("Las contraseñas no coinciden");
        }
        return errores;
    }

    private static void validarCamposComunes(String nombres, String apellidos, String correo,
                                             String idRol, String idSucursal, List<String> errores) {
        if (nombres == null || nombres.trim().length() < 2 || nombres.trim().length() > 80) {
            errores.add("Los nombres deben tener entre 2 y 80 caracteres");
        }
        if (apellidos == null || apellidos.trim().length() < 2 || apellidos.trim().length() > 80) {
            errores.add("Los apellidos deben tener entre 2 y 80 caracteres");
        }
        if (correo == null || correo.trim().length() > 120 || !PATRON_CORREO.matcher(correo.trim()).matches()) {
            errores.add("El formato de correo no es válido");
        }
        if (idRol == null || idRol.isBlank()) {
            errores.add("Debe seleccionar un rol para el colaborador");
        }
        if (idSucursal == null || idSucursal.isBlank()) {
            errores.add("Debe seleccionar una agencia para el colaborador");
        }
    }

    public static Optional<String> primerError(List<String> errores) {
        if (errores == null || errores.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(errores.get(0));
    }

    // =========================================================================
    // TRADUCCIÓN DE CÓDIGOS DE ERROR DE LA API
    // =========================================================================

    public static String traducirCodigoError(String codigo, String detallePorDefecto) {
        if (codigo == null) {
            return (detallePorDefecto != null && !detallePorDefecto.isBlank()) ? detallePorDefecto : "Error en la operación";
        }
        return switch (codigo.trim().toUpperCase()) {
            case "DNI_DUPLICADO" -> "Ya existe un empleado con ese DNI";
            case "CORREO_DUPLICADO" -> "Ya existe un empleado con ese correo";
            case "ACCION_NO_PERMITIDA" -> "No puedes realizar esta acción sobre tu propia cuenta";
            case "ULTIMO_ADMINISTRADOR" -> "Debe quedar al menos un administrador activo";
            case "ACCESO_DENEGADO", "HTTP_403" -> "No tienes permiso para esta acción";
            case "NO_AUTORIZADO", "HTTP_401" -> "Sesión expirada o no autorizada";
            case "VALIDACION_FALLIDA" -> (detallePorDefecto != null && !detallePorDefecto.isBlank()) ? detallePorDefecto : "Datos de entrada inválidos";
            case "SIN_CONEXION" -> "Requiere conexión";
            default -> (detallePorDefecto != null && !detallePorDefecto.isBlank()) ? detallePorDefecto : "Error en el servidor central: " + codigo;
        };
    }

    public static String limpiarMensajeError(Throwable error) {
        if (error == null) {
            return "Error inesperado en el módulo de personal";
        }
        if (error instanceof ApiExcepcion ae) {
            if (ae.getStatusCode() == 403) {
                return "No tienes permiso para esta acción";
            }
            if (ae.getStatusCode() == 401) {
                return "Sesión expirada o no autorizada";
            }
            if (ae.getCodigoError() != null && !ae.getCodigoError().isBlank()) {
                String traducido = traducirCodigoError(ae.getCodigoError(), ae.getMessage());
                return traducido.replace("null", "").trim();
            }
            String msg = ae.getMessage();
            if (msg != null && !msg.isBlank() && !msg.equalsIgnoreCase("null")) {
                return msg.replace("null", "").trim();
            }
        }
        String msg = error.getMessage();
        if (msg == null || msg.isBlank() || msg.equalsIgnoreCase("null")) {
            return "Error de comunicación con el servidor central";
        }
        return msg.replace("null", "").trim();
    }

    // =========================================================================
    // CONVERSIÓN Y FORMATEO DE FECHAS
    // =========================================================================

    public static OffsetDateTime convertirAInicioDiaLima(LocalDate fecha) {
        if (fecha == null) return null;
        return fecha.atTime(LocalTime.MIN).atZone(ZONA_LIMA).toOffsetDateTime();
    }

    public static OffsetDateTime convertirAFinDiaLima(LocalDate fecha) {
        if (fecha == null) return null;
        return fecha.atTime(LocalTime.MAX).atZone(ZONA_LIMA).toOffsetDateTime();
    }

    public static String formatearFechaHoraLocal(OffsetDateTime fechaHora) {
        if (fechaHora == null) {
            return "--/--/---- --:--:--";
        }
        return fechaHora.atZoneSameInstant(ZoneId.systemDefault()).format(FORMATO_FECHA_HORA);
    }

    // =========================================================================
    // REGLAS DE PRESENTACIÓN Y ACCIONES EN TABLA
    // =========================================================================

    public static String etiquetaEstado(Boolean estado) {
        return Boolean.TRUE.equals(estado) ? "Activo" : "Baja";
    }

    public static String etiquetaBotonCambiarEstado(EmpleadoDto seleccionado) {
        if (seleccionado == null || Boolean.TRUE.equals(seleccionado.estado())) {
            return "Dar de baja";
        }
        return "Reactivar";
    }

    public static boolean puedeEditar(EmpleadoDto seleccionado) {
        return seleccionado != null;
    }

    public static boolean puedeCambiarEstado(EmpleadoDto seleccionado) {
        return seleccionado != null;
    }

    public static boolean puedeRestablecerPassword(EmpleadoDto seleccionado) {
        return seleccionado != null;
    }

    public static String textoPaginacion(int paginaActualCero, int totalPaginas, long totalElementos) {
        int paginaHumana = totalElementos == 0 ? 0 : paginaActualCero + 1;
        return String.format("Página %d de %d (Total: %d)", paginaHumana, totalPaginas, totalElementos);
    }
}
