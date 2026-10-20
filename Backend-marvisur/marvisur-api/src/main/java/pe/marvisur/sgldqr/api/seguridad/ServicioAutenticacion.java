package pe.marvisur.sgldqr.api.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.RolJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RolSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;
import pe.marvisur.sgldqr.api.seguridad.dto.LoginRequest;
import pe.marvisur.sgldqr.api.seguridad.dto.LoginResponse;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;

import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de aplicación encargado de la autenticación de colaboradores, validación contra BCrypt,
 * verificación de estado activo y registro de bitácora en auditoria_logs.
 */
@Service
public class ServicioAutenticacion {

    private static final Logger log = LoggerFactory.getLogger(ServicioAutenticacion.class);

    private static final String MENSAJE_ERROR_CREDENCIALES = "Credenciales inválidas. Verifique su correo o contraseña.";
    private static final String TABLA_AFECTADA_EMPLEADOS = "empleados";
    private static final String ACCION_LOGIN_EXITOSO = "LOGIN_EXITOSO";
    private static final String ACCION_LOGIN_FALLIDO = "LOGIN_FALLIDO";

    private final EmpleadoSpringDataRepository empleadoRepository;
    private final RolSpringDataRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtServicio jwtServicio;
    private final AuditoriaPort auditoriaPort;

    public ServicioAutenticacion(EmpleadoSpringDataRepository empleadoRepository,
                                RolSpringDataRepository rolRepository,
                                PasswordEncoder passwordEncoder,
                                JwtServicio jwtServicio,
                                AuditoriaPort auditoriaPort) {
        this.empleadoRepository = Objects.requireNonNull(empleadoRepository, "El empleadoRepository no puede ser nulo");
        this.rolRepository = Objects.requireNonNull(rolRepository, "El rolRepository no puede ser nulo");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "El passwordEncoder no puede ser nulo");
        this.jwtServicio = Objects.requireNonNull(jwtServicio, "El jwtServicio no puede ser nulo");
        this.auditoriaPort = Objects.requireNonNull(auditoriaPort, "El auditoriaPort no puede ser nulo");
    }

    /**
     * Autentica a un empleado con su correo y contraseña.
     *
     * <p><strong>Reglas de seguridad y auditoría:</strong></p>
     * <ul>
     *   <li>Si el correo no existe: responde error genérico sin registrar auditoría (la FK exige empleado existente).</li>
     *   <li>Si la contraseña es incorrecta o el empleado está inactivo (baja lógica): registra {@code LOGIN_FALLIDO}
     *       y responde con el mismo error genérico (no revela si el usuario existe o su estado).</li>
     *   <li>Si las credenciales son válidas y el usuario está activo: registra {@code LOGIN_EXITOSO} y retorna el token.</li>
     * </ul>
     */
    @Transactional
    public LoginResponse autenticar(LoginRequest peticion, String ipOrigen) {
        Objects.requireNonNull(peticion, "La petición de login no puede ser nula");
        String correoNormalizado = peticion.correo().trim().toLowerCase();

        Optional<EmpleadoJpaEntity> empleadoOpt = empleadoRepository.findByCorreo(correoNormalizado);

        // 1. Si no existe el correo, rechaza con mensaje genérico sin registrar auditoría (FK restrictiva)
        if (empleadoOpt.isEmpty()) {
            log.warn("Intento de login con correo no existente desde IP: {}", ipOrigen);
            throw new BadCredentialsException(MENSAJE_ERROR_CREDENCIALES);
        }

        EmpleadoJpaEntity empleado = empleadoOpt.get();

        // 2. Validar contraseña contra BCrypt y verificar estado activo (baja lógica)
        boolean passwordValido = passwordEncoder.matches(peticion.password(), empleado.getPasswordHash());
        boolean activo = Boolean.TRUE.equals(empleado.getEstado());

        if (!passwordValido || !activo) {
            log.warn("Fallo de autenticación para empleado {} (passwordValido={}, activo={}) desde IP: {}",
                    empleado.getIdEmpleado(), passwordValido, activo, ipOrigen);

            // Registra LOGIN_FALLIDO solo porque el empleado sí existe (cumple con la FK id_empleado)
            auditoriaPort.registrar(
                    empleado.getIdEmpleado(),
                    ACCION_LOGIN_FALLIDO,
                    TABLA_AFECTADA_EMPLEADOS,
                    empleado.getIdEmpleado(),
                    ipOrigen
            );

            throw new BadCredentialsException(MENSAJE_ERROR_CREDENCIALES);
        }

        // 3. Resolver authority del rol
        String authority = resolverAuthority(empleado.getIdRol());

        EmpleadoAutenticadoDto dto = new EmpleadoAutenticadoDto(
                empleado.getIdEmpleado(),
                empleado.getNombres(),
                empleado.getApellidos(),
                empleado.getCorreo(),
                authority,
                empleado.getIdSucursal()
        );

        String token = jwtServicio.generarToken(dto);

        // 4. Registrar auditoría de éxito
        auditoriaPort.registrar(
                empleado.getIdEmpleado(),
                ACCION_LOGIN_EXITOSO,
                TABLA_AFECTADA_EMPLEADOS,
                empleado.getIdEmpleado(),
                ipOrigen
        );

        log.info("Login exitoso para empleado {} ({}) desde IP: {}",
                empleado.getIdEmpleado(), authority, ipOrigen);

        return new LoginResponse(
                token,
                "Bearer",
                jwtServicio.calcularFechaExpiracion().toString(),
                jwtServicio.getExpiracionSegundos(),
                dto
        );
    }

    /**
     * Consulta los datos del empleado autenticado por su identificador único.
     */
    @Transactional(readOnly = true)
    public EmpleadoAutenticadoDto obtenerEmpleadoPorId(String idEmpleado) {
        Objects.requireNonNull(idEmpleado, "El idEmpleado no puede ser nulo");
        EmpleadoJpaEntity empleado = empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new BadCredentialsException("Empleado no encontrado o sesión inválida"));

        if (!Boolean.TRUE.equals(empleado.getEstado())) {
            throw new BadCredentialsException("El empleado se encuentra deshabilitado");
        }

        String authority = resolverAuthority(empleado.getIdRol());
        return new EmpleadoAutenticadoDto(
                empleado.getIdEmpleado(),
                empleado.getNombres(),
                empleado.getApellidos(),
                empleado.getCorreo(),
                authority,
                empleado.getIdSucursal()
        );
    }

    private String resolverAuthority(String idRol) {
        if (idRol == null) {
            return ConstantesSeguridad.AUTH_RECEPCIONISTA;
        }
        Optional<RolJpaEntity> rolOpt = rolRepository.findById(idRol);
        if (rolOpt.isPresent()) {
            return ConstantesSeguridad.mapearAAuthority(rolOpt.get().getNombreRol());
        }
        return ConstantesSeguridad.mapearAAuthority(idRol);
    }
}
