package pe.marvisur.sgldqr.api.servicio;

import org.springframework.http.HttpStatus;
import pe.marvisur.sgldqr.api.error.ExcepcionOperacionInvalida;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validador de reglas de negocio y seguridad para la gestión de personal (RF-03).
 */
public final class ValidadorEmpleado {

    private static final Pattern PATRON_DNI = Pattern.compile("^\\d{8}$");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private ValidadorEmpleado() {
    }

    /**
     * Valida la política de contraseñas de personal operativo:
     * Al menos 8 caracteres, al menos una letra y al menos un dígito.
     */
    public static void validarPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.BAD_REQUEST,
                    "VALIDACION_FALLIDA",
                    "Validación Fallida",
                    "La contraseña no puede estar vacía"
            );
        }
        if (password.length() < 8) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.BAD_REQUEST,
                    "VALIDACION_FALLIDA",
                    "Validación Fallida",
                    "La contraseña debe tener al menos 8 caracteres"
            );
        }
        boolean tieneLetra = false;
        boolean tieneNumero = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                tieneLetra = true;
            } else if (Character.isDigit(c)) {
                tieneNumero = true;
            }
        }
        if (!tieneLetra || !tieneNumero) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.BAD_REQUEST,
                    "VALIDACION_FALLIDA",
                    "Validación Fallida",
                    "La contraseña debe contener al menos una letra y un número"
            );
        }
    }

    /**
     * Normaliza un correo electrónico: recorta espacios y lo convierte a minúsculas.
     * Valida sintácticamente el formato del correo.
     */
    public static String normalizarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.BAD_REQUEST,
                    "VALIDACION_FALLIDA",
                    "Validación Fallida",
                    "El correo electrónico no puede estar vacío"
            );
        }
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        if (!PATRON_CORREO.matcher(normalizado).matches()) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.BAD_REQUEST,
                    "VALIDACION_FALLIDA",
                    "Validación Fallida",
                    "El formato del correo electrónico es inválido"
            );
        }
        return normalizado;
    }

    /**
     * Valida el formato del DNI (exactamente 8 dígitos numéricos).
     */
    public static String normalizarDni(String dni) {
        if (dni == null || dni.isBlank()) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.BAD_REQUEST,
                    "VALIDACION_FALLIDA",
                    "Validación Fallida",
                    "El DNI no puede estar vacío"
            );
        }
        String normalizado = dni.trim();
        if (!PATRON_DNI.matcher(normalizado).matches()) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.BAD_REQUEST,
                    "VALIDACION_FALLIDA",
                    "Validación Fallida",
                    "El DNI debe contener exactamente 8 dígitos numéricos"
            );
        }
        return normalizado;
    }

    /**
     * Valida longitud de nombres o apellidos (entre 2 y 80 caracteres).
     */
    public static String normalizarTexto(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.BAD_REQUEST,
                    "VALIDACION_FALLIDA",
                    "Validación Fallida",
                    "El campo '" + campo + "' no puede estar vacío"
            );
        }
        String normalizado = texto.trim();
        if (normalizado.length() < 2 || normalizado.length() > 80) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.BAD_REQUEST,
                    "VALIDACION_FALLIDA",
                    "Validación Fallida",
                    "El campo '" + campo + "' debe tener entre 2 y 80 caracteres"
            );
        }
        return normalizado;
    }

    /**
     * Regla de seguridad: un administrador no puede darse de baja a sí mismo.
     */
    public static void validarAutoBaja(String idEmpleadoActual, String idEmpleadoDestino) {
        if (idEmpleadoActual != null && idEmpleadoActual.equals(idEmpleadoDestino)) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.CONFLICT,
                    "ACCION_NO_PERMITIDA",
                    "Acción No Permitida",
                    "Un administrador no puede darse de baja a sí mismo"
            );
        }
    }

    /**
     * Regla de seguridad: un administrador no puede cambiar su propio rol.
     */
    public static void validarAutoCambioRol(String idEmpleadoActual, String idEmpleadoDestino, String idRolActual, String nuevoIdRol) {
        if (idEmpleadoActual != null && idEmpleadoActual.equals(idEmpleadoDestino) && idRolActual != null && !idRolActual.equals(nuevoIdRol)) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.CONFLICT,
                    "ACCION_NO_PERMITIDA",
                    "Acción No Permitida",
                    "Un administrador no puede cambiar su propio rol"
            );
        }
    }

    /**
     * Regla de seguridad: no se puede dar de baja ni quitar el rol Administrador al último administrador activo.
     */
    public static void validarReglaUltimoAdministrador(long totalAdministradoresActivos) {
        if (totalAdministradoresActivos <= 1) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.CONFLICT,
                    "ULTIMO_ADMINISTRADOR",
                    "Último Administrador",
                    "No se puede dar de baja ni quitar el rol Administrador al último administrador activo"
            );
        }
    }
}
