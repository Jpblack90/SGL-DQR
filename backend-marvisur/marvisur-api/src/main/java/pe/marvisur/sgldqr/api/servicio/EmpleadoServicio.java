package pe.marvisur.sgldqr.api.servicio;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.dto.ActualizarEmpleadoRequest;
import pe.marvisur.sgldqr.api.dto.CambiarEstadoEmpleadoRequest;
import pe.marvisur.sgldqr.api.dto.CrearEmpleadoRequest;
import pe.marvisur.sgldqr.api.dto.EmpleadoDto;
import pe.marvisur.sgldqr.api.dto.RestablecerPasswordRequest;
import pe.marvisur.sgldqr.api.dto.RolComboDto;
import pe.marvisur.sgldqr.api.error.ExcepcionOperacionInvalida;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.RolJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RolSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para la gestión de personal operativo y control de accesos (RF-03).
 * Implementa reglas de seguridad RBAC, bajas lógicas y auditoría transaccional.
 */
@Service
public class EmpleadoServicio {

    private static final Logger log = LoggerFactory.getLogger(EmpleadoServicio.class);
    private static final String TABLA_EMPLEADOS = "empleados";

    private final EmpleadoSpringDataRepository empleadoRepository;
    private final RolSpringDataRepository rolRepository;
    private final SucursalSpringDataRepository sucursalRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaPort auditoriaPort;

    public EmpleadoServicio(EmpleadoSpringDataRepository empleadoRepository,
                            RolSpringDataRepository rolRepository,
                            SucursalSpringDataRepository sucursalRepository,
                            PasswordEncoder passwordEncoder,
                            AuditoriaPort auditoriaPort) {
        this.empleadoRepository = Objects.requireNonNull(empleadoRepository, "empleadoRepository no puede ser nulo");
        this.rolRepository = Objects.requireNonNull(rolRepository, "rolRepository no puede ser nulo");
        this.sucursalRepository = Objects.requireNonNull(sucursalRepository, "sucursalRepository no puede ser nulo");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder no puede ser nulo");
        this.auditoriaPort = Objects.requireNonNull(auditoriaPort, "auditoriaPort no puede ser nulo");
    }

    /**
     * Consulta paginada y filtrada de colaboradores.
     */
    @Transactional(readOnly = true)
    public Page<EmpleadoDto> listarEmpleados(String estado, String idRol, String idSucursal, String q, Pageable pageable) {
        int tamanioSeguro = Math.min(pageable != null ? pageable.getPageSize() : 20, 50);
        int numeroPagina = pageable != null ? pageable.getPageNumber() : 0;
        Sort orden = (pageable != null && pageable.getSort().isSorted())
                ? pageable.getSort()
                : Sort.by(Sort.Direction.ASC, "apellidos", "nombres");
        Pageable pageableAjustado = PageRequest.of(numeroPagina, tamanioSeguro, orden);

        Boolean estadoFiltro;
        if (estado == null || estado.isBlank() || "ACTIVOS".equalsIgnoreCase(estado)) {
            estadoFiltro = Boolean.TRUE;
        } else if ("INACTIVOS".equalsIgnoreCase(estado)) {
            estadoFiltro = Boolean.FALSE;
        } else if ("TODOS".equalsIgnoreCase(estado)) {
            estadoFiltro = null;
        } else {
            estadoFiltro = Boolean.TRUE;
        }

        String terminoBusqueda = (q != null && !q.isBlank()) ? q.trim() : null;
        String rolFiltro = (idRol != null && !idRol.isBlank()) ? idRol.trim() : null;
        String sucursalFiltro = (idSucursal != null && !idSucursal.isBlank()) ? idSucursal.trim() : null;

        Page<EmpleadoJpaEntity> paginaEntidades = empleadoRepository.buscarConFiltros(
                estadoFiltro, rolFiltro, sucursalFiltro, terminoBusqueda, pageableAjustado
        );

        Map<String, String> rolesMap = rolRepository.findAll().stream()
                .collect(Collectors.toMap(RolJpaEntity::getIdRol, RolJpaEntity::getNombreRol, (a, b) -> a));
        Map<String, String> sucursalesMap = sucursalRepository.findAll().stream()
                .collect(Collectors.toMap(SucursalJpaEntity::getIdSucursal, SucursalJpaEntity::getNombreAgencia, (a, b) -> a));

        return paginaEntidades.map(e -> aDto(e, rolesMap, sucursalesMap));
    }

    /**
     * Obtiene el detalle de un empleado por su identificador único.
     */
    @Transactional(readOnly = true)
    public EmpleadoDto obtenerPorId(String id) {
        EmpleadoJpaEntity entity = empleadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el empleado con ID: " + id));

        String nombreRol = rolRepository.findById(entity.getIdRol())
                .map(RolJpaEntity::getNombreRol).orElse("Rol no especificado");
        String nombreSucursal = sucursalRepository.findById(entity.getIdSucursal())
                .map(SucursalJpaEntity::getNombreAgencia).orElse("Sucursal no especificada");

        return aDto(entity, nombreRol, nombreSucursal);
    }

    /**
     * Alta de un nuevo empleado en el sistema.
     */
    @Transactional
    public EmpleadoDto crearEmpleado(CrearEmpleadoRequest request, UsuarioAutenticado usuarioActual, String ipOrigen) {
        Objects.requireNonNull(request, "request no puede ser nulo");
        String dniNormalizado = ValidadorEmpleado.normalizarDni(request.dni());
        String correoNormalizado = ValidadorEmpleado.normalizarCorreo(request.correo());
        String nombresNormalizados = ValidadorEmpleado.normalizarTexto(request.nombres(), "nombres");
        String apellidosNormalizados = ValidadorEmpleado.normalizarTexto(request.apellidos(), "apellidos");
        ValidadorEmpleado.validarPassword(request.password());

        if (empleadoRepository.findByDni(dniNormalizado).isPresent()) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.CONFLICT,
                    "DNI_DUPLICADO",
                    "DNI Duplicado",
                    "Ya existe un empleado registrado con el DNI " + dniNormalizado
            );
        }

        if (empleadoRepository.findByCorreo(correoNormalizado).isPresent()) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.CONFLICT,
                    "CORREO_DUPLICADO",
                    "Correo Duplicado",
                    "Ya existe un empleado registrado con el correo " + correoNormalizado
            );
        }

        RolJpaEntity rol = rolRepository.findById(request.idRol())
                .orElseThrow(() -> new EntityNotFoundException("El rol con ID '" + request.idRol() + "' no existe"));

        SucursalJpaEntity sucursal = sucursalRepository.findById(request.idSucursal())
                .orElseThrow(() -> new EntityNotFoundException("La sucursal con ID '" + request.idSucursal() + "' no existe"));

        String idEmpleadoNuevo = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(request.password());

        EmpleadoJpaEntity entidad = new EmpleadoJpaEntity(
                idEmpleadoNuevo,
                rol.getIdRol(),
                sucursal.getIdSucursal(),
                dniNormalizado,
                nombresNormalizados,
                apellidosNormalizados,
                correoNormalizado,
                passwordHash,
                Boolean.TRUE
        );

        empleadoRepository.save(entidad);

        auditoriaPort.registrar(
                usuarioActual != null ? usuarioActual.getIdEmpleado() : "SISTEMA",
                "EMPLEADO_CREADO",
                TABLA_EMPLEADOS,
                idEmpleadoNuevo,
                ipOrigen
        );

        log.info("Empleado creado exitosamente: id={}, dni={}, rol={}", idEmpleadoNuevo, dniNormalizado, rol.getIdRol());
        return aDto(entidad, rol.getNombreRol(), sucursal.getNombreAgencia());
    }

    /**
     * Modificación de datos generales de un empleado.
     */
    @Transactional
    public EmpleadoDto actualizarEmpleado(String id, ActualizarEmpleadoRequest request, UsuarioAutenticado usuarioActual, String ipOrigen) {
        Objects.requireNonNull(request, "request no puede ser nulo");
        EmpleadoJpaEntity empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el empleado con ID: " + id));

        String correoNormalizado = ValidadorEmpleado.normalizarCorreo(request.correo());
        String nombresNormalizados = ValidadorEmpleado.normalizarTexto(request.nombres(), "nombres");
        String apellidosNormalizados = ValidadorEmpleado.normalizarTexto(request.apellidos(), "apellidos");

        Optional<EmpleadoJpaEntity> existenteCorreo = empleadoRepository.findByCorreo(correoNormalizado);
        if (existenteCorreo.isPresent() && !existenteCorreo.get().getIdEmpleado().equals(id)) {
            throw new ExcepcionOperacionInvalida(
                    HttpStatus.CONFLICT,
                    "CORREO_DUPLICADO",
                    "Correo Duplicado",
                    "Ya existe un empleado registrado con el correo " + correoNormalizado
            );
        }

        RolJpaEntity nuevoRol = rolRepository.findById(request.idRol())
                .orElseThrow(() -> new EntityNotFoundException("El rol con ID '" + request.idRol() + "' no existe"));

        SucursalJpaEntity sucursal = sucursalRepository.findById(request.idSucursal())
                .orElseThrow(() -> new EntityNotFoundException("La sucursal con ID '" + request.idSucursal() + "' no existe"));

        // Regla: un administrador no puede cambiar su propio rol
        String idEjecutor = usuarioActual != null ? usuarioActual.getIdEmpleado() : null;
        ValidadorEmpleado.validarAutoCambioRol(idEjecutor, id, empleado.getIdRol(), request.idRol());

        // Regla: no quitar rol Administrador al último administrador activo
        if (ConstantesSeguridad.ID_ROL_ADMINISTRADOR.equals(empleado.getIdRol())
                && Boolean.TRUE.equals(empleado.getEstado())
                && !ConstantesSeguridad.ID_ROL_ADMINISTRADOR.equals(request.idRol())) {
            long totalAdmins = empleadoRepository.countByIdRolAndEstadoTrue(ConstantesSeguridad.ID_ROL_ADMINISTRADOR);
            ValidadorEmpleado.validarReglaUltimoAdministrador(totalAdmins);
        }

        empleado.setNombres(nombresNormalizados);
        empleado.setApellidos(apellidosNormalizados);
        empleado.setCorreo(correoNormalizado);
        empleado.setIdRol(nuevoRol.getIdRol());
        empleado.setIdSucursal(sucursal.getIdSucursal());

        empleadoRepository.save(empleado);

        auditoriaPort.registrar(
                usuarioActual != null ? usuarioActual.getIdEmpleado() : "SISTEMA",
                "EMPLEADO_ACTUALIZADO",
                TABLA_EMPLEADOS,
                id,
                ipOrigen
        );

        log.info("Empleado actualizado exitosamente: id={}", id);
        return aDto(empleado, nuevoRol.getNombreRol(), sucursal.getNombreAgencia());
    }

    /**
     * Cambio de estado de vigencia laboral (baja lógica o reactivación).
     */
    @Transactional
    public EmpleadoDto cambiarEstado(String id, CambiarEstadoEmpleadoRequest request, UsuarioAutenticado usuarioActual, String ipOrigen) {
        Objects.requireNonNull(request, "request no puede ser nulo");
        EmpleadoJpaEntity empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el empleado con ID: " + id));

        boolean nuevoEstado = Boolean.TRUE.equals(request.activo());

        if (!nuevoEstado) {
            String idEjecutor = usuarioActual != null ? usuarioActual.getIdEmpleado() : null;
            // Regla: un administrador no puede darse de baja a sí mismo
            ValidadorEmpleado.validarAutoBaja(idEjecutor, id);

            // Regla: no dar de baja al último administrador activo
            if (ConstantesSeguridad.ID_ROL_ADMINISTRADOR.equals(empleado.getIdRol()) && Boolean.TRUE.equals(empleado.getEstado())) {
                long totalAdmins = empleadoRepository.countByIdRolAndEstadoTrue(ConstantesSeguridad.ID_ROL_ADMINISTRADOR);
                ValidadorEmpleado.validarReglaUltimoAdministrador(totalAdmins);
            }
        }

        empleado.setEstado(nuevoEstado);
        empleadoRepository.save(empleado);

        String accionAudit = nuevoEstado ? "EMPLEADO_REACTIVADO" : "EMPLEADO_DADO_DE_BAJA";
        auditoriaPort.registrar(
                usuarioActual != null ? usuarioActual.getIdEmpleado() : "SISTEMA",
                accionAudit,
                TABLA_EMPLEADOS,
                id,
                ipOrigen
        );

        String nombreRol = rolRepository.findById(empleado.getIdRol())
                .map(RolJpaEntity::getNombreRol).orElse("Rol no especificado");
        String nombreSucursal = sucursalRepository.findById(empleado.getIdSucursal())
                .map(SucursalJpaEntity::getNombreAgencia).orElse("Sucursal no especificada");

        log.info("Estado de empleado modificado: id={}, nuevoEstado={}", id, nuevoEstado);
        return aDto(empleado, nombreRol, nombreSucursal);
    }

    /**
     * Restablecimiento de contraseña por parte del administrador.
     */
    @Transactional
    public void restablecerPassword(String id, RestablecerPasswordRequest request, UsuarioAutenticado usuarioActual, String ipOrigen) {
        Objects.requireNonNull(request, "request no puede ser nulo");
        EmpleadoJpaEntity empleado = empleadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el empleado con ID: " + id));

        ValidadorEmpleado.validarPassword(request.nuevaPassword());

        empleado.setPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        empleadoRepository.save(empleado);

        auditoriaPort.registrar(
                usuarioActual != null ? usuarioActual.getIdEmpleado() : "SISTEMA",
                "EMPLEADO_PASSWORD_RESTABLECIDA",
                TABLA_EMPLEADOS,
                id,
                ipOrigen
        );

        log.info("Contraseña de empleado restablecida exitosamente: id={}", id);
    }

    /**
     * Lista catálogo de roles disponibles para selectores y combos.
     */
    @Transactional(readOnly = true)
    public List<RolComboDto> listarRoles() {
        return rolRepository.findAll().stream()
                .map(r -> new RolComboDto(r.getIdRol(), r.getNombreRol()))
                .toList();
    }

    private EmpleadoDto aDto(EmpleadoJpaEntity e, Map<String, String> rolesMap, Map<String, String> sucursalesMap) {
        String nombreRol = rolesMap.getOrDefault(e.getIdRol(), "Rol no especificado");
        String nombreSucursal = sucursalesMap.getOrDefault(e.getIdSucursal(), "Sucursal no especificada");
        return aDto(e, nombreRol, nombreSucursal);
    }

    private EmpleadoDto aDto(EmpleadoJpaEntity e, String nombreRol, String nombreSucursal) {
        return new EmpleadoDto(
                e.getIdEmpleado(),
                e.getDni(),
                e.getNombres(),
                e.getApellidos(),
                e.getCorreo(),
                e.getIdRol(),
                nombreRol,
                e.getIdSucursal(),
                nombreSucursal,
                e.getEstado()
        );
    }
}
