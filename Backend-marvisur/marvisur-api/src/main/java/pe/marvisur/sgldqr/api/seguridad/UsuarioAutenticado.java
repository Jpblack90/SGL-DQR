package pe.marvisur.sgldqr.api.seguridad;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Representación del principal de seguridad autenticado en el contexto de Spring Security.
 */
public class UsuarioAutenticado implements UserDetails {

    private final String idEmpleado;
    private final String nombres;
    private final String apellidos;
    private final String correo;
    private final String rol;
    private final String idSucursal;
    private final List<GrantedAuthority> authorities;

    public UsuarioAutenticado(String idEmpleado, String nombres, String apellidos,
                              String correo, String rol, String idSucursal) {
        this.idEmpleado = Objects.requireNonNull(idEmpleado, "El idEmpleado no puede ser nulo");
        this.nombres = Objects.requireNonNull(nombres, "Los nombres no pueden ser nulos");
        this.apellidos = Objects.requireNonNull(apellidos, "Los apellidos no pueden ser nulos");
        this.correo = Objects.requireNonNull(correo, "El correo no puede ser nulo");
        this.rol = Objects.requireNonNull(rol, "El rol no puede ser nulo");
        this.idSucursal = Objects.requireNonNull(idSucursal, "El idSucursal no puede ser nulo");
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(rol));
    }

    public static UsuarioAutenticado desdeDto(EmpleadoAutenticadoDto dto) {
        return new UsuarioAutenticado(
                dto.id(),
                dto.nombres(),
                dto.apellidos(),
                dto.correo(),
                dto.rol(),
                dto.idSucursal()
        );
    }

    public EmpleadoAutenticadoDto aDto() {
        return new EmpleadoAutenticadoDto(idEmpleado, nombres, apellidos, correo, rol, idSucursal);
    }

    public String getIdEmpleado() {
        return idEmpleado;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getRol() {
        return rol;
    }

    public String getIdSucursal() {
        return idSucursal;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // En autenticación basada en JWT stateless no se almacena la contraseña en el principal
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
