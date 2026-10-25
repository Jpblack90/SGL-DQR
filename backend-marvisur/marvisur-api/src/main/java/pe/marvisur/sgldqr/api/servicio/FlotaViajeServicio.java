package pe.marvisur.sgldqr.api.servicio;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.dto.AgenciaResumenDto;
import pe.marvisur.sgldqr.api.dto.CambiarEstadoViajeRequest;
import pe.marvisur.sgldqr.api.dto.CrearVehiculoRequest;
import pe.marvisur.sgldqr.api.dto.CrearViajeRequest;
import pe.marvisur.sgldqr.api.dto.ManifiestoEncomiendaDto;
import pe.marvisur.sgldqr.api.dto.RutaDto;
import pe.marvisur.sgldqr.api.dto.VehiculoDto;
import pe.marvisur.sgldqr.api.dto.ViajeDto;
import pe.marvisur.sgldqr.api.error.ExcepcionOperacionInvalida;
import pe.marvisur.sgldqr.api.persistencia.entidad.DespachoLoteDetalleJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.RutaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.VehiculoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.especificacion.ViajeFiltroEspecificacion;
import pe.marvisur.sgldqr.api.persistencia.mapeador.FechaMapperHelper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.DespachoLoteDetalleSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ProgramacionViajeSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RolSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RutaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.VehiculoSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para la gestión de flota de vehículos, rutas y programación de viajes (RF-05).
 * Integra reglas de control de capacidad, asignación de choferes, transiciones de estado de ruta y auditoría.
 */
@Service
public class FlotaViajeServicio {

    private static final ZoneId ZONA_OFICIAL = ZoneId.of("America/Lima");

    private final VehiculoSpringDataRepository vehiculoRepo;
    private final RutaSpringDataRepository rutaRepo;
    private final ProgramacionViajeSpringDataRepository programacionViajeRepo;
    private final EmpleadoSpringDataRepository empleadoRepo;
    private final RolSpringDataRepository rolRepo;
    private final SucursalSpringDataRepository sucursalRepo;
    private final DespachoLoteDetalleSpringDataRepository despachoRepo;
    private final EncomiendaSpringDataRepository encomiendaRepo;
    private final AuditoriaPort auditoriaPort;

    public FlotaViajeServicio(VehiculoSpringDataRepository vehiculoRepo,
                              RutaSpringDataRepository rutaRepo,
                              ProgramacionViajeSpringDataRepository programacionViajeRepo,
                              EmpleadoSpringDataRepository empleadoRepo,
                              RolSpringDataRepository rolRepo,
                              SucursalSpringDataRepository sucursalRepo,
                              DespachoLoteDetalleSpringDataRepository despachoRepo,
                              EncomiendaSpringDataRepository encomiendaRepo,
                              AuditoriaPort auditoriaPort) {
        this.vehiculoRepo = Objects.requireNonNull(vehiculoRepo, "El vehiculoRepo no puede ser nulo");
        this.rutaRepo = Objects.requireNonNull(rutaRepo, "El rutaRepo no puede ser nulo");
        this.programacionViajeRepo = Objects.requireNonNull(programacionViajeRepo, "El programacionViajeRepo no puede ser nulo");
        this.empleadoRepo = Objects.requireNonNull(empleadoRepo, "El empleadoRepo no puede ser nulo");
        this.rolRepo = Objects.requireNonNull(rolRepo, "El rolRepo no puede ser nulo");
        this.sucursalRepo = Objects.requireNonNull(sucursalRepo, "El sucursalRepo no puede ser nulo");
        this.despachoRepo = Objects.requireNonNull(despachoRepo, "El despachoRepo no puede ser nulo");
        this.encomiendaRepo = Objects.requireNonNull(encomiendaRepo, "El encomiendaRepo no puede ser nulo");
        this.auditoriaPort = Objects.requireNonNull(auditoriaPort, "El auditoriaPort no puede ser nulo");
    }

    // =========================================================================
    // 1. GESTIÓN DE VEHÍCULOS (TN3)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<VehiculoDto> listarVehiculos() {
        return vehiculoRepo.findAll(Sort.by(Sort.Direction.ASC, "placa")).stream()
                .map(v -> new VehiculoDto(v.getIdVehiculo(), v.getPlaca(), v.getModelo(), v.getCapacidadCargaKg()))
                .toList();
    }

    @Transactional
    public VehiculoDto crearVehiculo(CrearVehiculoRequest request, String idEmpleado, String ipOrigen) {
        Objects.requireNonNull(request, "La petición de creación de vehículo no puede ser nula");

        if (request.placa() == null || request.placa().isBlank()) {
            throw new ExcepcionOperacionInvalida(HttpStatus.BAD_REQUEST, "VALIDACION_FALLIDA", "Validación Fallida", "La placa es obligatoria");
        }

        String placaNormalizada = request.placa().trim().toUpperCase();
        if (request.placa().contains(" ") || !placaNormalizada.matches("^[A-Z0-9-]{5,10}$")) {
            throw new ExcepcionOperacionInvalida(HttpStatus.BAD_REQUEST, "VALIDACION_FALLIDA", "Validación Fallida",
                    "La placa debe contener entre 5 y 10 caracteres en mayúsculas, sin espacios");
        }

        if (request.capacidadCargaKg() == null || request.capacidadCargaKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExcepcionOperacionInvalida(HttpStatus.BAD_REQUEST, "VALIDACION_FALLIDA", "Validación Fallida",
                    "La capacidad de carga debe ser mayor a cero");
        }

        if (request.modelo() == null || request.modelo().isBlank()) {
            throw new ExcepcionOperacionInvalida(HttpStatus.BAD_REQUEST, "VALIDACION_FALLIDA", "Validación Fallida",
                    "El modelo es obligatorio");
        }

        if (vehiculoRepo.existsByPlacaIgnoreCase(placaNormalizada)) {
            throw new ExcepcionOperacionInvalida(HttpStatus.CONFLICT, "PLACA_DUPLICADA", "Placa Duplicada",
                    "Ya existe un vehículo registrado con la placa: " + placaNormalizada);
        }

        String idVehiculo = UUID.randomUUID().toString();
        VehiculoJpaEntity entidad = new VehiculoJpaEntity(
                idVehiculo,
                placaNormalizada,
                request.modelo().trim(),
                request.capacidadCargaKg()
        );
        vehiculoRepo.save(entidad);

        // Auditoría en la misma transacción (Propagation.REQUIRED)
        auditoriaPort.registrar(idEmpleado, "VEHICULO_REGISTRADO", "vehiculos", idVehiculo, ipOrigen);

        return new VehiculoDto(idVehiculo, placaNormalizada, entidad.getModelo(), entidad.getCapacidadCargaKg());
    }

    // =========================================================================
    // 2. CONSULTA DE RUTAS (TN2, TN3, TN4)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<RutaDto> listarRutas() {
        List<RutaJpaEntity> rutas = rutaRepo.findAll();
        if (rutas.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> idSucursales = new HashSet<>();
        for (RutaJpaEntity r : rutas) {
            if (r.getIdSucursalOrigen() != null) idSucursales.add(r.getIdSucursalOrigen());
            if (r.getIdSucursalDestino() != null) idSucursales.add(r.getIdSucursalDestino());
        }

        Map<String, SucursalJpaEntity> sucursalesMap = sucursalRepo.findAllById(idSucursales).stream()
                .collect(Collectors.toMap(SucursalJpaEntity::getIdSucursal, Function.identity()));

        return rutas.stream().map(r -> {
            SucursalJpaEntity origen = sucursalesMap.get(r.getIdSucursalOrigen());
            SucursalJpaEntity destino = sucursalesMap.get(r.getIdSucursalDestino());
            return new RutaDto(
                    r.getIdRuta(),
                    new AgenciaResumenDto(r.getIdSucursalOrigen(), origen != null ? origen.getNombreAgencia() : ""),
                    new AgenciaResumenDto(r.getIdSucursalDestino(), destino != null ? destino.getNombreAgencia() : ""),
                    r.getDistanciaKm()
            );
        }).toList();
    }

    // =========================================================================
    // 3. CONSULTA DE VIAJES (TN2, TN3, TN4)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ViajeDto> listarViajes(EstadoViaje estado, String idSucursalOrigen, String idChofer, UsuarioAutenticado usuario) {
        boolean esChofer = esUsuarioChofer(usuario);
        // Regla RBAC: TN4 Chofer SOLO ve sus propios viajes, ignorando cualquier idChofer enviado
        String choferEfectivo = esChofer ? usuario.getIdEmpleado() : idChofer;

        String estadoStr = estado != null ? estado.name() : null;
        Specification<ProgramacionViajeJpaEntity> spec = ViajeFiltroEspecificacion.conFiltros(estadoStr, idSucursalOrigen, choferEfectivo);

        List<ProgramacionViajeJpaEntity> viajes = programacionViajeRepo.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaSalida"));
        if (viajes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> idRutas = viajes.stream().map(ProgramacionViajeJpaEntity::getIdRuta).collect(Collectors.toSet());
        Set<String> idVehiculos = viajes.stream().map(ProgramacionViajeJpaEntity::getIdVehiculo).collect(Collectors.toSet());
        Set<String> idChoferes = viajes.stream().map(ProgramacionViajeJpaEntity::getIdChofer).collect(Collectors.toSet());

        Map<String, RutaJpaEntity> rutasMap = rutaRepo.findAllById(idRutas).stream()
                .collect(Collectors.toMap(RutaJpaEntity::getIdRuta, Function.identity()));
        Map<String, VehiculoJpaEntity> vehiculosMap = vehiculoRepo.findAllById(idVehiculos).stream()
                .collect(Collectors.toMap(VehiculoJpaEntity::getIdVehiculo, Function.identity()));
        Map<String, EmpleadoJpaEntity> choferesMap = empleadoRepo.findAllById(idChoferes).stream()
                .collect(Collectors.toMap(EmpleadoJpaEntity::getIdEmpleado, Function.identity()));

        Set<String> idSucursales = new HashSet<>();
        rutasMap.values().forEach(r -> {
            if (r.getIdSucursalOrigen() != null) idSucursales.add(r.getIdSucursalOrigen());
            if (r.getIdSucursalDestino() != null) idSucursales.add(r.getIdSucursalDestino());
        });
        Map<String, SucursalJpaEntity> sucursalesMap = sucursalRepo.findAllById(idSucursales).stream()
                .collect(Collectors.toMap(SucursalJpaEntity::getIdSucursal, Function.identity()));

        return viajes.stream().map(v -> {
            RutaJpaEntity ruta = rutasMap.get(v.getIdRuta());
            VehiculoJpaEntity vehiculo = vehiculosMap.get(v.getIdVehiculo());
            EmpleadoJpaEntity chofer = choferesMap.get(v.getIdChofer());

            String nombreChofer = chofer != null ? chofer.getNombres() + " " + chofer.getApellidos() : "Sin chofer";
            String placa = vehiculo != null ? vehiculo.getPlaca() : "";
            String modelo = vehiculo != null ? vehiculo.getModelo() : "";

            RutaDto rutaDto = null;
            if (ruta != null) {
                SucursalJpaEntity orig = sucursalesMap.get(ruta.getIdSucursalOrigen());
                SucursalJpaEntity dest = sucursalesMap.get(ruta.getIdSucursalDestino());
                rutaDto = new RutaDto(
                        ruta.getIdRuta(),
                        new AgenciaResumenDto(ruta.getIdSucursalOrigen(), orig != null ? orig.getNombreAgencia() : ""),
                        new AgenciaResumenDto(ruta.getIdSucursalDestino(), dest != null ? dest.getNombreAgencia() : ""),
                        ruta.getDistanciaKm()
                );
            }

            return new ViajeDto(
                    v.getIdViaje(),
                    v.getIdRuta(),
                    ruta != null ? ruta.getIdSucursalOrigen() : "",
                    ruta != null ? ruta.getIdSucursalDestino() : "",
                    ruta != null ? ruta.getDistanciaKm() : null,
                    v.getIdVehiculo(),
                    placa,
                    v.getIdChofer(),
                    nombreChofer,
                    FechaMapperHelper.aLocalDateTime(v.getFechaSalida()),
                    FechaMapperHelper.aLocalDateTime(v.getFechaLlegadaEstimada()),
                    EstadoViaje.valueOf(v.getEstadoViaje()),
                    rutaDto,
                    placa,
                    modelo
            );
        }).toList();
    }

    // =========================================================================
    // 4. PROGRAMACIÓN DE VIAJES (TN3)
    // =========================================================================

    @Transactional
    public ViajeDto crearViaje(CrearViajeRequest request, UsuarioAutenticado usuario, String ipOrigen) {
        Objects.requireNonNull(request, "La petición de viaje no puede ser nula");

        // 1. Ruta y vehículo deben existir
        RutaJpaEntity ruta = rutaRepo.findById(request.idRuta())
                .orElseThrow(() -> new EntityNotFoundException("Ruta no encontrada con ID: " + request.idRuta()));
        VehiculoJpaEntity vehiculo = vehiculoRepo.findById(request.idVehiculo())
                .orElseThrow(() -> new EntityNotFoundException("Vehículo no encontrado con ID: " + request.idVehiculo()));

        // 2. Chofer debe existir, estar activo y tener rol TN4_CHOFER
        EmpleadoJpaEntity chofer = empleadoRepo.findById(request.idChofer())
                .orElseThrow(() -> new ExcepcionOperacionInvalida(HttpStatus.CONFLICT, "CHOFER_INVALIDO", "Chofer Inválido",
                        "El empleado especificado como chofer no existe"));

        if (!Boolean.TRUE.equals(chofer.getEstado())) {
            throw new ExcepcionOperacionInvalida(HttpStatus.CONFLICT, "CHOFER_INVALIDO", "Chofer Inválido",
                    "El chofer asignado no se encuentra activo");
        }

        if (!esRolChofer(chofer.getIdRol())) {
            throw new ExcepcionOperacionInvalida(HttpStatus.CONFLICT, "CHOFER_INVALIDO", "Chofer Inválido",
                    "El empleado asignado no tiene el rol de Chofer (TN4)");
        }

        // 3. Validar fechas: llegada > salida y salida no anterior a 5 minutos en el pasado
        if (!request.fechaLlegadaEstimada().isAfter(request.fechaSalida())) {
            throw new ExcepcionOperacionInvalida(HttpStatus.BAD_REQUEST, "FECHA_INVALIDA", "Fecha Inválida",
                    "La fecha de llegada estimada debe ser posterior a la fecha de salida");
        }

        OffsetDateTime ahoraLima = OffsetDateTime.now(ZONA_OFICIAL);
        OffsetDateTime limitePasado = ahoraLima.minusMinutes(5);
        if (request.fechaSalida().isBefore(limitePasado)) {
            throw new ExcepcionOperacionInvalida(HttpStatus.BAD_REQUEST, "FECHA_INVALIDA", "Fecha Inválida",
                    "La fecha de salida no puede ser anterior a más de 5 minutos en el pasado");
        }

        // 4. Chofer y vehículo no deben tener viajes activos en ruta
        if (programacionViajeRepo.existsByIdChoferAndEstadoViaje(request.idChofer(), EstadoViaje.EN_RUTA.name())) {
            throw new ExcepcionOperacionInvalida(HttpStatus.CONFLICT, "CHOFER_OCUPADO", "Chofer Ocupado",
                    "El chofer ya tiene un viaje asignado en estado EN_RUTA");
        }

        if (programacionViajeRepo.existsByIdVehiculoAndEstadoViaje(request.idVehiculo(), EstadoViaje.EN_RUTA.name())) {
            throw new ExcepcionOperacionInvalida(HttpStatus.CONFLICT, "VEHICULO_OCUPADO", "Vehículo Ocupado",
                    "El vehículo ya se encuentra asignado a un viaje en estado EN_RUTA");
        }

        // 5. Persistencia del viaje con estado PROGRAMADO
        String idViaje = UUID.randomUUID().toString();
        ProgramacionViajeJpaEntity entidad = new ProgramacionViajeJpaEntity(
                idViaje,
                request.idRuta(),
                request.idVehiculo(),
                request.idChofer(),
                request.fechaSalida(),
                request.fechaLlegadaEstimada(),
                EstadoViaje.PROGRAMADO.name()
        );
        programacionViajeRepo.save(entidad);

        // 6. Auditoría en la misma transacción (Propagation.REQUIRED)
        auditoriaPort.registrar(usuario.getIdEmpleado(), "VIAJE_CREADO", "programacion_viajes", idViaje, ipOrigen);

        SucursalJpaEntity orig = sucursalRepo.findById(ruta.getIdSucursalOrigen()).orElse(null);
        SucursalJpaEntity dest = sucursalRepo.findById(ruta.getIdSucursalDestino()).orElse(null);
        RutaDto rutaDto = new RutaDto(
                ruta.getIdRuta(),
                new AgenciaResumenDto(ruta.getIdSucursalOrigen(), orig != null ? orig.getNombreAgencia() : ""),
                new AgenciaResumenDto(ruta.getIdSucursalDestino(), dest != null ? dest.getNombreAgencia() : ""),
                ruta.getDistanciaKm()
        );

        return new ViajeDto(
                idViaje,
                ruta.getIdRuta(),
                ruta.getIdSucursalOrigen(),
                ruta.getIdSucursalDestino(),
                ruta.getDistanciaKm(),
                vehiculo.getIdVehiculo(),
                vehiculo.getPlaca(),
                chofer.getIdEmpleado(),
                chofer.getNombres() + " " + chofer.getApellidos(),
                FechaMapperHelper.aLocalDateTime(request.fechaSalida()),
                FechaMapperHelper.aLocalDateTime(request.fechaLlegadaEstimada()),
                EstadoViaje.PROGRAMADO,
                rutaDto,
                vehiculo.getPlaca(),
                vehiculo.getModelo()
        );
    }

    // =========================================================================
    // 5. ACTUALIZACIÓN DE ESTADO DEL VIAJE (TN3 o TN4 Chofer asignado)
    // =========================================================================

    @Transactional
    public ViajeDto cambiarEstadoViaje(String idViaje, CambiarEstadoViajeRequest request, UsuarioAutenticado usuario, String ipOrigen) {
        Objects.requireNonNull(request, "La petición de cambio de estado no puede ser nula");

        ProgramacionViajeJpaEntity viaje = programacionViajeRepo.findById(idViaje)
                .orElseThrow(() -> new EntityNotFoundException("Viaje no encontrado con ID: " + idViaje));

        // Regla RBAC: TN4 Chofer solo puede alterar sus propios viajes asignados
        if (esUsuarioChofer(usuario) && !Objects.equals(viaje.getIdChofer(), usuario.getIdEmpleado())) {
            throw new AccessDeniedException("El chofer solo puede modificar el estado de sus propios viajes asignados");
        }

        EstadoViaje actual = EstadoViaje.valueOf(viaje.getEstadoViaje());
        EstadoViaje nuevo = request.estado();

        String accionAuditoria;
        if (actual == EstadoViaje.PROGRAMADO && nuevo == EstadoViaje.EN_RUTA) {
            accionAuditoria = "VIAJE_INICIADO";
        } else if (actual == EstadoViaje.EN_RUTA && nuevo == EstadoViaje.FINALIZADO) {
            accionAuditoria = "VIAJE_FINALIZADO";
        } else {
            throw new ExcepcionOperacionInvalida(HttpStatus.CONFLICT, "TRANSICION_INVALIDA", "Transición Inválida",
                    "No se puede transicionar el viaje del estado " + actual + " al estado " + nuevo);
        }

        viaje.setEstadoViaje(nuevo.name());
        programacionViajeRepo.save(viaje);

        // Auditoría en la misma transacción (Propagation.REQUIRED)
        auditoriaPort.registrar(usuario.getIdEmpleado(), accionAuditoria, "programacion_viajes", idViaje, ipOrigen);

        // Mapeo completo de respuesta
        RutaJpaEntity ruta = rutaRepo.findById(viaje.getIdRuta()).orElse(null);
        VehiculoJpaEntity vehiculo = vehiculoRepo.findById(viaje.getIdVehiculo()).orElse(null);
        EmpleadoJpaEntity chofer = empleadoRepo.findById(viaje.getIdChofer()).orElse(null);

        RutaDto rutaDto = null;
        if (ruta != null) {
            SucursalJpaEntity orig = sucursalRepo.findById(ruta.getIdSucursalOrigen()).orElse(null);
            SucursalJpaEntity dest = sucursalRepo.findById(ruta.getIdSucursalDestino()).orElse(null);
            rutaDto = new RutaDto(
                    ruta.getIdRuta(),
                    new AgenciaResumenDto(ruta.getIdSucursalOrigen(), orig != null ? orig.getNombreAgencia() : ""),
                    new AgenciaResumenDto(ruta.getIdSucursalDestino(), dest != null ? dest.getNombreAgencia() : ""),
                    ruta.getDistanciaKm()
            );
        }

        String placa = vehiculo != null ? vehiculo.getPlaca() : "";
        String modelo = vehiculo != null ? vehiculo.getModelo() : "";
        String nombreChofer = chofer != null ? chofer.getNombres() + " " + chofer.getApellidos() : "";

        return new ViajeDto(
                viaje.getIdViaje(),
                viaje.getIdRuta(),
                ruta != null ? ruta.getIdSucursalOrigen() : "",
                ruta != null ? ruta.getIdSucursalDestino() : "",
                ruta != null ? ruta.getDistanciaKm() : null,
                viaje.getIdVehiculo(),
                placa,
                viaje.getIdChofer(),
                nombreChofer,
                FechaMapperHelper.aLocalDateTime(viaje.getFechaSalida()),
                FechaMapperHelper.aLocalDateTime(viaje.getFechaLlegadaEstimada()),
                nuevo,
                rutaDto,
                placa,
                modelo
        );
    }

    // =========================================================================
    // 6. MANIFIESTO DE CARGA (TN2, TN3, TN4 Chofer asignado)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ManifiestoEncomiendaDto> obtenerManifiesto(String idViaje, UsuarioAutenticado usuario) {
        ProgramacionViajeJpaEntity viaje = programacionViajeRepo.findById(idViaje)
                .orElseThrow(() -> new EntityNotFoundException("Viaje no encontrado con ID: " + idViaje));

        // Regla RBAC: TN4 Chofer solo puede consultar manifiesto de sus propios viajes
        if (esUsuarioChofer(usuario) && !Objects.equals(viaje.getIdChofer(), usuario.getIdEmpleado())) {
            throw new AccessDeniedException("El chofer solo puede consultar el manifiesto de sus propios viajes asignados");
        }

        List<DespachoLoteDetalleJpaEntity> detalles = despachoRepo.findByIdViaje(idViaje);
        if (detalles.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> idEncomiendas = detalles.stream()
                .map(DespachoLoteDetalleJpaEntity::getIdEncomienda)
                .collect(Collectors.toSet());

        List<EncomiendaJpaEntity> encomiendas = encomiendaRepo.findAllById(idEncomiendas);
        if (encomiendas.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> idSucursales = encomiendas.stream()
                .map(EncomiendaJpaEntity::getIdSucursalDestino)
                .collect(Collectors.toSet());

        Map<String, SucursalJpaEntity> sucursalesMap = sucursalRepo.findAllById(idSucursales).stream()
                .collect(Collectors.toMap(SucursalJpaEntity::getIdSucursal, Function.identity()));

        // Directiva estricta de CERO datos personales
        return encomiendas.stream().map(e -> {
            SucursalJpaEntity dest = sucursalesMap.get(e.getIdSucursalDestino());
            String nombreDestino = dest != null ? dest.getNombreAgencia() : "";
            return new ManifiestoEncomiendaDto(
                    e.getCodigoComprobante(),
                    e.getEstadoLogistico(),
                    nombreDestino,
                    e.getPesoKg()
            );
        }).toList();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    private boolean esUsuarioChofer(UsuarioAutenticado usuario) {
        if (usuario == null) {
            return false;
        }
        return usuario.getAuthorities().stream().anyMatch(a ->
                ConstantesSeguridad.AUTH_CHOFER.equals(a.getAuthority())
                        || "ROLE_TN4_CHOFER".equals(a.getAuthority())
                        || "CHOFER".equalsIgnoreCase(usuario.getRol()));
    }

    private boolean esRolChofer(String idRol) {
        if (idRol == null) {
            return false;
        }
        if (ConstantesSeguridad.ID_ROL_CHOFER.equals(idRol)) {
            return true;
        }
        return rolRepo.findById(idRol).map(r ->
                r.getNombreRol().toUpperCase().contains("CHOFER") || r.getIdRol().toUpperCase().contains("CHOFER")
        ).orElse(false);
    }
}
