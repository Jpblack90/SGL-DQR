package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.seguridad.ServicioAutenticacion;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;
import pe.marvisur.sgldqr.api.seguridad.dto.LoginRequest;
import pe.marvisur.sgldqr.api.seguridad.dto.LoginResponse;

import java.util.Objects;

/**
 * Controlador REST para autenticación y consulta del perfil del colaborador autenticado.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación y Sesión", description = "Endpoints de login con JWT y consulta de perfil")
public class AuthController {

    private final ServicioAutenticacion servicioAutenticacion;

    public AuthController(ServicioAutenticacion servicioAutenticacion) {
        this.servicioAutenticacion = Objects.requireNonNull(servicioAutenticacion, "El servicioAutenticacion no puede ser nulo");
    }

    @PostMapping("/login")
    @Operation(summary = "Inicio de sesión", description = "Autentica al colaborador con correo y contraseña, emitiendo un token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario deshabilitado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "Validación de campos fallida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest peticion,
                                               HttpServletRequest request) {
        String ipOrigen = obtenerIpCliente(request);
        LoginResponse respuesta = servicioAutenticacion.autenticar(peticion, ipOrigen);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del colaborador autenticado", description = "Devuelve los datos del empleado asociado al token JWT actual.")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos del empleado autenticado",
                    content = @Content(schema = @Schema(implementation = EmpleadoAutenticadoDto.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, expirado o manipulado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<EmpleadoAutenticadoDto> me(@AuthenticationPrincipal UsuarioAutenticado principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        EmpleadoAutenticadoDto empleado = servicioAutenticacion.obtenerEmpleadoPorId(principal.getIdEmpleado());
        return ResponseEntity.ok(empleado);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }
}
