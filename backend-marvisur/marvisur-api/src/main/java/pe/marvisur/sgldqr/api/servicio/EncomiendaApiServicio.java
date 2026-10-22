package pe.marvisur.sgldqr.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.marvisur.sgldqr.api.dto.ClienteInputDto;
import pe.marvisur.sgldqr.api.dto.DetalleEncomiendaDto;
import pe.marvisur.sgldqr.api.dto.Qr1DataDto;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaRequest;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaResponse;
import pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.FechaMapperHelper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.aplicacion.RegistrarEncomiendaCommand;
import pe.marvisur.sgldqr.aplicacion.RegistrarEncomiendaService;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPreRegistro;
import pe.marvisur.sgldqr.dominio.modelo.HistorialTracking;
import pe.marvisur.sgldqr.dominio.modelo.MapeadorEventoTracking;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.PreRegistroExpress;
import pe.marvisur.sgldqr.dominio.servicio.MotorQR;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;
import pe.marvisur.sgldqr.puertos.HistorialTrackingPort;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;
import pe.marvisur.sgldqr.puertos.RepositorioParDobleQRPort;
import pe.marvisur.sgldqr.puertos.RepositorioPreRegistroExpressPort;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de orquestación para la gestión de encomiendas en la API REST.
 */
@Service
public class EncomiendaApiServicio {

    private static final Logger log = LoggerFactory.getLogger(EncomiendaApiServicio.class);

    private static final String ADVERTENCIA_QR2 =
            "ADVERTENCIA: El token QR2 de la llave digital se entrega por única vez. No se puede recuperar si se pierde. Marvisur no almacena el token en claro.";
    private static final String TABLA_ENCOMIENDAS = "encomiendas";
    private static final String ACCION_REGISTRO = "REGISTRO_ENCOMIENDA";

    private final RegistrarEncomiendaService registrarEncomiendaService;
    private final MotorQR motorQR;
    private final ImagenQRPort imagenQRPort;
    private final RepositorioParDobleQRPort parDobleQRPort;
    private final HistorialTrackingPort historialTrackingPort;
    private final AuditoriaPort auditoriaPort;
    private final ClienteSpringDataRepository clienteRepository;
    private final EncomiendaSpringDataRepository encomiendaRepository;
    private final GeneradorCodigoComprobante generadorCodigoComprobante;
    private final RepositorioPreRegistroExpressPort repositorioPreRegistroPort;

    @Value("${app.portal.url-base:http://localhost:5173}")
    private String portalUrlBase;

    @Value("${app.preregistro.vigencia-horas:48}")
    private int vigenciaHoras;

    public EncomiendaApiServicio(RegistrarEncomiendaService registrarEncomiendaService,
                                 MotorQR motorQR,
                                 ImagenQRPort imagenQRPort,
                                 RepositorioParDobleQRPort parDobleQRPort,
                                 HistorialTrackingPort historialTrackingPort,
                                 AuditoriaPort auditoriaPort,
                                 ClienteSpringDataRepository clienteRepository,
                                 EncomiendaSpringDataRepository encomiendaRepository,
                                 GeneradorCodigoComprobante generadorCodigoComprobante,
                                 RepositorioPreRegistroExpressPort repositorioPreRegistroPort) {
        this.registrarEncomiendaService = Objects.requireNonNull(registrarEncomiendaService, "El registrarEncomiendaService no puede ser nulo");
        this.motorQR = Objects.requireNonNull(motorQR, "El motorQR no puede ser nulo");
        this.imagenQRPort = Objects.requireNonNull(imagenQRPort, "El imagenQRPort no puede ser nulo");
        this.parDobleQRPort = Objects.requireNonNull(parDobleQRPort, "El parDobleQRPort no puede ser nulo");
        this.historialTrackingPort = Objects.requireNonNull(historialTrackingPort, "El historialTrackingPort no puede ser nulo");
        this.auditoriaPort = Objects.requireNonNull(auditoriaPort, "El auditoriaPort no puede ser nulo");
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "El clienteRepository no puede ser nulo");
        this.encomiendaRepository = Objects.requireNonNull(encomiendaRepository, "El encomiendaRepository no puede ser nulo");
        this.generadorCodigoComprobante = Objects.requireNonNull(generadorCodigoComprobante, "El generadorCodigoComprobante no puede ser nulo");
        this.repositorioPreRegistroPort = Objects.requireNonNull(repositorioPreRegistroPort, "El repositorioPreRegistroPort no puede ser nulo");
    }

    /**
     * Registra una encomienda en una única transacción atómica.
     *
     * <p>Flujo:
     * 1. Busca o crea clientes remitente y destinatario (reutiliza sin sobrescribir).
     * 2. Genera código de comprobante único con reintentos automáticos.
     * 3. Registra encomienda con RegistrarEncomiendaService (calcula tarifa oficial).
     * 4. Genera Par Doble QR criptográfico y persiste solo hashes con token ACTIVO.
     * 5. Registra hito inicial en historial_tracking.
     * 6. Registra entrada en auditoria_logs.
     * 7. Retorna respuesta 201 con QR1 PNG base64 y QR2 en claro por única vez.
     * </p>
     */
    @Transactional
    public RegistrarEncomiendaResponse registrarEncomienda(RegistrarEncomiendaRequest request,
                                                           UsuarioAutenticado usuario,
                                                           String ipOrigen) {
        Objects.requireNonNull(request, "La solicitud no puede ser nula");
        Objects.requireNonNull(usuario, "El usuario autenticado no puede ser nulo");

        // 0. Si viene código express, validar existencia, vigencia, estado y sucursal de destino
        PreRegistroExpress preRegistro = null;
        if (request.codigoExpress() != null && !request.codigoExpress().isBlank()) {
            String codExpress = request.codigoExpress().trim();
            preRegistro = repositorioPreRegistroPort.buscarPorCodigoExpress(codExpress)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "No se encontró el pre-registro express con código: " + codExpress));

            LocalDateTime ahora = LocalDateTime.now();
            if (preRegistro.estaExpirado(ahora, vigenciaHoras)) {
                preRegistro.marcarExpirado();
                repositorioPreRegistroPort.actualizar(preRegistro);
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "El pre-registro express con código " + codExpress + " ha expirado.");
            }

            if (preRegistro.getEstado() != EstadoPreRegistro.PENDIENTE_VENTANILLA) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "El pre-registro express con código " + codExpress + " ya fue procesado o no está pendiente.");
            }

            if (!preRegistro.getIdSucursalDestino().equals(request.idSucursalDestino())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "La sucursal de destino no coincide con la del pre-registro express.");
            }
        }

        // 1. Clientes: buscar por numero_documento o crear sin sobrescribir existentes
        ClienteJpaEntity remitente = obtenerOCrearCliente(request.remitente());
        ClienteJpaEntity destinatario = obtenerOCrearCliente(request.destinatario());

        // 2. Generar código de comprobante único
        String codigoComprobante = generadorCodigoComprobante.generarCodigoUnico();
        String sucursalOrigen = usuario.getIdSucursal();

        // 3. Registrar encomienda con servicio de dominio
        RegistrarEncomiendaCommand comando = new RegistrarEncomiendaCommand(
                codigoComprobante,
                remitente.getIdCliente(),
                destinatario.getIdCliente(),
                sucursalOrigen,
                request.idSucursalDestino(),
                request.pesoKg(),
                request.volumenM3(),
                request.descripcionContenido(),
                request.modalidadPago()
        );
        Encomienda encomienda = registrarEncomiendaService.registrar(comando);

        // 4. Generar Par Doble QR con MotorQR
        ParDobleQR par = motorQR.generarDobleQR(encomienda);
        parDobleQRPort.guardar(par);

        // 4b. Si vino código express, marcarlo como PROCESADO dentro de la misma transacción
        if (preRegistro != null) {
            preRegistro.marcarProcesado(LocalDateTime.now(), vigenciaHoras);
            repositorioPreRegistroPort.actualizar(preRegistro);
        }

        // 5. Registrar evento inicial en historial_tracking
        String eventoTracking = MapeadorEventoTracking.aEventoTracking(encomienda.getEstadoLogistico());
        HistorialTracking tracking = HistorialTracking.crear(
                encomienda.getIdEncomienda(),
                sucursalOrigen,
                eventoTracking,
                "Encomienda recepcionada en ventanilla",
                LocalDateTime.now()
        );
        historialTrackingPort.registrarEvento(tracking);

        // 6. Registrar auditoría operativa (sin almacenar datos sensibles del QR2)
        auditoriaPort.registrar(
                usuario.getIdEmpleado(),
                ACCION_REGISTRO,
                TABLA_ENCOMIENDAS,
                encomienda.getIdEncomienda(),
                ipOrigen
        );

        // 7. Generar imagen PNG del QR1 operativo
        byte[] qr1PngBytes = imagenQRPort.generarPNG(par.getUuidQr1Operativo(), 300);
        String qr1Base64 = Base64.getEncoder().encodeToString(qr1PngBytes);

        // 8. Construir enlace a Billetera Digital con QR2 en fragmento (#)
        String qr2TokenEnClaro = par.getQr2TokenEnClaro();
        String enlaceBilletera = portalUrlBase + "/billetera#" + qr2TokenEnClaro;

        log.info("Encomienda {} registrada con éxito (código: {}) por empleado {}",
                encomienda.getIdEncomienda(), encomienda.getCodigoComprobante(), usuario.getIdEmpleado());

        return new RegistrarEncomiendaResponse(
                encomienda.getIdEncomienda(),
                encomienda.getCodigoComprobante(),
                encomienda.getIdRemitente(),
                encomienda.getIdDestinatario(),
                encomienda.getIdSucursalOrigen(),
                encomienda.getIdSucursalDestino(),
                encomienda.getPesoKg(),
                encomienda.getVolumenM3(),
                encomienda.getDescripcionContenido(),
                encomienda.getMontoTotal(),
                encomienda.getEstadoPago(),
                encomienda.getEstadoLogistico(),
                encomienda.getFechaRegistro().toString(),
                new Qr1DataDto(par.getUuidQr1Operativo(), qr1Base64),
                qr2TokenEnClaro,
                enlaceBilletera,
                ADVERTENCIA_QR2
        );
    }

    /**
     * Consulta una encomienda por su ID, validando el alcance de sucursal del empleado.
     */
    @Transactional(readOnly = true)
    public DetalleEncomiendaDto consultarPorId(String idEncomienda, UsuarioAutenticado usuario) {
        Objects.requireNonNull(idEncomienda, "El ID de la encomienda no puede ser nulo");
        Objects.requireNonNull(usuario, "El usuario autenticado no puede ser nulo");

        EncomiendaJpaEntity entidad = encomiendaRepository.findById(idEncomienda)
                .orElseThrow(() -> new NoSuchElementException("Encomienda no encontrada con ID: " + idEncomienda));

        validarAlcanceSucursal(entidad, usuario);

        return mapearADetalleDto(entidad);
    }

    /**
     * Lista encomiendas con filtros opcionales y paginación, aplicando visibilidad por sucursal.
     */
    @Transactional(readOnly = true)
    public Page<DetalleEncomiendaDto> listarEncomiendas(EstadoPago estadoPago,
                                                        EstadoLogistico estadoLogistico,
                                                        String idSucursalOrigen,
                                                        String idSucursalDestino,
                                                        Pageable pageable,
                                                        UsuarioAutenticado usuario) {
        Objects.requireNonNull(usuario, "El usuario autenticado no puede ser nulo");

        Specification<EncomiendaJpaEntity> spec = Specification.where(null);

        // Control de visibilidad: empleado no administrador solo ve encomiendas asociadas a su sucursal
        boolean esAdmin = usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(ConstantesSeguridad.AUTH_ADMINISTRADOR));

        if (!esAdmin) {
            String sucursalEmpleado = usuario.getIdSucursal();
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.equal(root.get("idSucursalOrigen"), sucursalEmpleado),
                    cb.equal(root.get("idSucursalDestino"), sucursalEmpleado)
            ));
        } else {
            if (idSucursalOrigen != null && !idSucursalOrigen.isBlank()) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("idSucursalOrigen"), idSucursalOrigen.trim()));
            }
            if (idSucursalDestino != null && !idSucursalDestino.isBlank()) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("idSucursalDestino"), idSucursalDestino.trim()));
            }
        }

        if (estadoPago != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estadoPago"), estadoPago.name()));
        }

        if (estadoLogistico != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estadoLogistico"), estadoLogistico.name()));
        }

        return encomiendaRepository.findAll(spec, pageable).map(this::mapearADetalleDto);
    }

    /**
     * Regenera el PNG del QR1 Operativo para reimpresión física en ventanilla o almacén (RF-06).
     */
    @Transactional(readOnly = true)
    public byte[] generarPngQr1(String idEncomienda, UsuarioAutenticado usuario) {
        Objects.requireNonNull(idEncomienda, "El ID de la encomienda no puede ser nulo");
        Objects.requireNonNull(usuario, "El usuario no puede ser nulo");

        EncomiendaJpaEntity entidad = encomiendaRepository.findById(idEncomienda)
                .orElseThrow(() -> new NoSuchElementException("Encomienda no encontrada con ID: " + idEncomienda));

        validarAlcanceSucursal(entidad, usuario);

        ParDobleQR par = parDobleQRPort.buscarPorEncomiendaId(idEncomienda)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el par Doble QR para la encomienda: " + idEncomienda));

        return imagenQRPort.generarPNG(par.getUuidQr1Operativo(), 300);
    }

    private ClienteJpaEntity obtenerOCrearCliente(ClienteInputDto input) {
        String numDoc = input.numeroDocumento().trim();
        return clienteRepository.findByNumeroDocumento(numDoc)
                .orElseGet(() -> {
                    ClienteJpaEntity nuevo = new ClienteJpaEntity(
                            UUID.randomUUID().toString(),
                            input.tipoDocumento().name(),
                            numDoc,
                            input.nombresRazonSocial().trim(),
                            input.telefono(),
                            input.correo()
                    );
                    return clienteRepository.save(nuevo);
                });
    }

    private void validarAlcanceSucursal(EncomiendaJpaEntity encomienda, UsuarioAutenticado usuario) {
        boolean esAdmin = usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(ConstantesSeguridad.AUTH_ADMINISTRADOR));

        if (!esAdmin) {
            String sucursalUsuario = usuario.getIdSucursal();
            boolean pertenece = sucursalUsuario.equals(encomienda.getIdSucursalOrigen())
                    || sucursalUsuario.equals(encomienda.getIdSucursalDestino());

            if (!pertenece) {
                log.warn("Acceso denegado: Empleado {} (sucursal {}) intentó acceder a encomienda {} (origen {}, destino {})",
                        usuario.getIdEmpleado(), sucursalUsuario, encomienda.getIdEncomienda(),
                        encomienda.getIdSucursalOrigen(), encomienda.getIdSucursalDestino());
                throw new AccessDeniedException("No tiene permisos para acceder a encomiendas de otra sucursal.");
            }
        }
    }

    private DetalleEncomiendaDto mapearADetalleDto(EncomiendaJpaEntity entidad) {
        return new DetalleEncomiendaDto(
                entidad.getIdEncomienda(),
                entidad.getCodigoComprobante(),
                entidad.getIdRemitente(),
                entidad.getIdDestinatario(),
                entidad.getIdSucursalOrigen(),
                entidad.getIdSucursalDestino(),
                entidad.getPesoKg(),
                entidad.getVolumenM3(),
                entidad.getDescripcionContenido(),
                entidad.getMontoTotal(),
                EstadoPago.valueOf(entidad.getEstadoPago()),
                EstadoLogistico.valueOf(entidad.getEstadoLogistico()),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaRegistro()).toString()
        );
    }
}
