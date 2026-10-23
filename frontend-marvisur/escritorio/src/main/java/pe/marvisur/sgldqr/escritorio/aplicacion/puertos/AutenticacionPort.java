package pe.marvisur.sgldqr.escritorio.aplicacion.puertos;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.UsuarioSesion;

/**
 * Puerto para la autenticación de empleados y manejo de credenciales en memoria.
 */
public interface AutenticacionPort {

    /**
     * Autentica al empleado contra la API REST y devuelve su sesión activa con el token JWT.
     *
     * @param correo correo electrónico del empleado
     * @param password contraseña en claro
     * @return sesión activa con token JWT en memoria
     */
    UsuarioSesion autenticar(String correo, String password);

    /**
     * Cierra la sesión activa y purga el token de memoria.
     */
    void cerrarSesion();

    /**
     * Devuelve la sesión en memoria si existe.
     */
    UsuarioSesion obtenerSesionActual();
}
