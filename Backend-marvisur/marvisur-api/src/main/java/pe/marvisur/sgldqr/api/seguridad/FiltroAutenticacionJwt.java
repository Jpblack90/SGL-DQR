package pe.marvisur.sgldqr.api.seguridad;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro interceptor que extrae y valida el token JWT de la cabecera 'Authorization: Bearer <token>'.
 */
@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private static final String CABECERA_AUTHORIZATION = "Authorization";
    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtServicio jwtServicio;

    public FiltroAutenticacionJwt(JwtServicio jwtServicio) {
        this.jwtServicio = jwtServicio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String cabecera = request.getHeader(CABECERA_AUTHORIZATION);

        if (cabecera != null && cabecera.startsWith(PREFIJO_BEARER)) {
            String token = cabecera.substring(PREFIJO_BEARER.length()).trim();
            Optional<Claims> claimsOpt = jwtServicio.obtenerClaims(token);

            if (claimsOpt.isPresent()) {
                Claims claims = claimsOpt.get();
                String idEmpleado = claims.getSubject();
                String correo = claims.get("correo", String.class);
                String nombres = claims.get("nombres", String.class);
                String apellidos = claims.get("apellidos", String.class);
                String rol = claims.get("rol", String.class);
                String idSucursal = claims.get("idSucursal", String.class);

                if (idEmpleado != null && rol != null) {
                    UsuarioAutenticado principal = new UsuarioAutenticado(
                            idEmpleado,
                            nombres != null ? nombres : "",
                            apellidos != null ? apellidos : "",
                            correo != null ? correo : "",
                            rol,
                            idSucursal != null ? idSucursal : ""
                    );

                    UsernamePasswordAuthenticationToken autenticacion =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            } else {
                // Token inválido o manipulado: nos aseguramos de limpiar el contexto
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
