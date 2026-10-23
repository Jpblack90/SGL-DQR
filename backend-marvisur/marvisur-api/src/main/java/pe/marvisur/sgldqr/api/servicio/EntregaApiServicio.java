package pe.marvisur.sgldqr.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.dto.RespuestaCobroDto;
import pe.marvisur.sgldqr.api.dto.RespuestaValidacionEntregaDto;
import pe.marvisur.sgldqr.api.dto.ValidarEntregaRequest;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.TokenQr2EstatusJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.EncomiendaMapper;
import pe.marvisur.sgldqr.api.persistencia.mapeador.FechaMapperHelper;
import pe.marvisur.sgldqr.api.persistencia.mapeador.ParDobleQrMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ParDobleQrSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.TokenQr2EstatusSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.aplicacion.CambiarEstadoService;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoTokenQR2;
import pe.marvisur.sgldqr.dominio.modelo.HistorialTracking;
import pe.marvisur.sgldqr.dominio.modelo.MapeadorEventoTracking;
import pe.marvisur.sgldqr.dominio.modelo.MotivoInactivacion;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.ResultadoValidacion;
import pe.marvisur.sgldqr.dominio.modelo.ResultadoValidacionCruzada;
import pe.marvisur.sgldqr.dominio.modelo.TokenQR2Estatus;
import pe.marvisur.sgldqr.dominio.modelo.ValidacionCruzadaLog;
import pe.marvisur.sgldqr.dominio.servicio.MotorQR;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;
import pe.marvisur.sgldqr.puertos.HistorialTrackingPort;
import pe.marvisur.sgldqr.puertos.RepositorioParDobleQRPort;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio transaccional orquestador de validación cruzada Doble QR, cobro Contra Entrega
 * y consumación física de entrega (RF-04, RF-07, RNF-01, RNF-03).
 */
@Service
public class EntregaApiServicio {

    private static final Logger log = LoggerFactory.getLogger(EntregaApiServicio.class);

    private static final String ACCION_VALIDACION_ENTREGA = "VALIDACION_ENTREGA";
    private static final String ACCION_VALIDACION_RECHAZADA = "VALIDACION_ENTREGA_RECHAZADA";
    private static final String ACCION_COBRO = "COBRO_CONTRA_ENTREGA";
    private static final String TABLA_ENCOMIENDAS = "encomiendas";

    private final ParDobleQrSpringDataRepository parRepository;
    private final TokenQr2EstatusSpringDataRepository tokenRepository;
    private final EncomiendaSpringDataRepository encomiendaRepository;
    private final RepositorioParDobleQRPort parDobleQRPort;
    private final HistorialTrackingPort historialTrackingPort;
    private final AuditoriaPort auditoriaPort;
    private final MotorQR motorQR;
    private final CambiarEstadoService cambiarEstadoService;

    public EntregaApiServicio(ParDobleQrSpringDataRepository parRepository,
                              TokenQr2EstatusSpringDataRepository tokenRepository,
                              EncomiendaSpringDataRepository encomiendaRepository,
                              RepositorioParDobleQRPort parDobleQRPort,
                              HistorialTrackingPort historialTrackingPort,
                              AuditoriaPort auditoriaPort,
                              MotorQR motorQR,
                              CambiarEstadoService cambiarEstadoService) {
        this.parRepository = Objects.requireNonNull(parRepository, "El parRepository no puede ser nulo");
        this.tokenRepository = Objects.requireNonNull(tokenRepository, "El tokenRepository no puede ser nulo");
        this.encomiendaRepository = Objects.requireNonNull(encomiendaRepository, "El encomiendaRepository no puede ser nulo");
        this.parDobleQRPort = Objects.requireNonNull(parDobleQRPort, "El parDobleQRPort no puede ser nulo");
        this.historialTrackingPort = Objects.requireNonNull(historialTrackingPort, "El historialTrackingPort no puede ser nulo");
        this.auditoriaPort = Objects.requireNonNull(auditoriaPort, "El auditoriaPort no puede ser nulo");
        this.motorQR = Objects.requireNonNull(motorQR, "El motorQR no puede ser nulo");
        this.cambiarEstadoService = Objects.requireNonNull(cambiarEstadoService, "El cambiarEstadoService no puede ser nulo");
    }

    /**
     * Ejecuta la validación cruzada del par Doble QR y consuma la entrega física.
     * Devuelve un resultado tipado sin lanzar excepciones por fallos de validación,
     * garantizando que el log de auditoría en validaciones_cruzadas_logs quede persistido.
     */
    @Transactional
    public ResultadoProcesoValidacion validarEntrega(ValidarEntregaRequest request, UsuarioAutenticado usuario, String ipOrigen) {
        Objects.requireNonNull(request, "La petición no puede ser nula");
        Objects.requireNonNull(usuario, "El usuario autenticado no puede ser nulo");

        String qr1 = request.qr1().trim();

        // 1. Buscar la pareja por QR1 (uuid_qr1_operativo o id_encomienda)
        ParDobleQrJpaEntity parEntidad = parRepository.findByUuidQr1Operativo(qr1)
                .or(() -> {
                    if (!qr1.startsWith("MARVISUR:QR1:")) {
                        return parRepository.findByUuidQr1Operativo("MARVISUR:QR1:" + qr1);
                    }
                    return Optional.empty();
                })
                .or(() -> parRepository.findByIdEncomienda(qr1))
                .orElse(null);

        if (parEntidad == null) {
            log.warn("Validación rechazada: QR1 no encontrado en el sistema.");
            return ResultadoProcesoValidacion.noEncontrado("No se encontró encomienda asociada al código QR1: " + qr1);
        }

        // 2. Buscar la encomienda asociada
        EncomiendaJpaEntity encomiendaEntidad = encomiendaRepository.findById(parEntidad.getIdEncomienda())
                .orElse(null);

        if (encomiendaEntidad == null) {
            log.warn("Validación rechazada: Encomienda no encontrada para la pareja QR: {}", parEntidad.getIdParejaQr());
            return ResultadoProcesoValidacion.noEncontrado("No se encontró la encomienda asociada al par QR.");
        }

        // 3. Bloqueo pesimista del token para serializar validaciones concurrentes sobre el mismo QR2
        TokenQr2EstatusJpaEntity tokenEntidad = tokenRepository.findByIdParejaQrConBloqueo(parEntidad.getIdParejaQr())
                .orElseThrow(() -> new IllegalStateException("No se encontró estatus de token para pareja: " + parEntidad.getIdParejaQr()));

        // Si el token ya fue invalidado previamente (ej. reintento o carrera concurrente en que otro hilo ya entregó)
        if (!EstadoTokenQR2.ACTIVO.name().equals(tokenEntidad.getEstadoToken())) {
            log.warn("Validación rechazada: El token QR2 de la pareja {} ya no está ACTIVO (estado actual: {})",
                    parEntidad.getIdParejaQr(), tokenEntidad.getEstadoToken());
            long latencia = 5L;
            LocalDateTime ahora = LocalDateTime.now();
            ValidacionCruzadaLog logValidacion = ValidacionCruzadaLog.crear(
                    parEntidad.getIdParejaQr(),
                    usuario.getIdEmpleado(),
                    ResultadoValidacion.FALLIDO_TOKEN_INVALIDO,
                    (int) latencia,
                    ahora
            );
            parDobleQRPort.guardarValidacionLog(logValidacion);
            return ResultadoProcesoValidacion.tokenInvalido(latencia);
        }

        // 4. Precondición: la encomienda debe estar EN_ALMACEN_DESTINO (si el token aún está ACTIVO)
        if (!EstadoLogistico.EN_ALMACEN_DESTINO.name().equals(encomiendaEntidad.getEstadoLogistico())) {
            log.warn("Validación rechazada por transición inválida: Encomienda {} en estado '{}' (requiere 'EN_ALMACEN_DESTINO')",
                    encomiendaEntidad.getIdEncomienda(), encomiendaEntidad.getEstadoLogistico());

            // Registrar auditoría de rechazo sin tocar el token ni registrar en validaciones_cruzadas_logs
            auditoriaPort.registrar(
                    usuario.getIdEmpleado(),
                    ACCION_VALIDACION_RECHAZADA,
                    TABLA_ENCOMIENDAS,
                    encomiendaEntidad.getIdEncomienda(),
                    ipOrigen
            );

            return ResultadoProcesoValidacion.transicionInvalida(
                    encomiendaEntidad.getEstadoLogistico(),
                    "La encomienda no se encuentra en estado 'EN_ALMACEN_DESTINO'. Estado actual: " + encomiendaEntidad.getEstadoLogistico()
            );
        }

        // Mapear entidades a modelos de dominio para ejecutar MotorQR
        TokenQR2Estatus tokenDominio = ParDobleQrMapper.aDominio(tokenEntidad);
        ParDobleQR parDominio = ParDobleQrMapper.aDominio(parEntidad, tokenEntidad);
        Encomienda encomiendaDominio = EncomiendaMapper.aDominio(encomiendaEntidad);

        // 5. Ejecutar validación cruzada criptográfica en MotorQR
        ResultadoValidacionCruzada resultadoCruzado = motorQR.validarCruzado(
                request.qr1(),
                request.qr2(),
                encomiendaDominio,
                parDominio
        );

        // 6. Registrar SIEMPRE una fila en validaciones_cruzadas_logs
        ValidacionCruzadaLog logValidacion = ValidacionCruzadaLog.crear(
                parEntidad.getIdParejaQr(),
                usuario.getIdEmpleado(),
                resultadoCruzado.getResultado(),
                (int) resultadoCruzado.getLatenciaProcesamientoMs(),
                resultadoCruzado.getFechaValidacion()
        );
        parDobleQRPort.guardarValidacionLog(logValidacion);

        if (resultadoCruzado.esExitoso()) {
            // Actualizar token a INVALIDADO con motivo ENTREGA_EXITOSA y fecha_invalidacion
            tokenEntidad.setEstadoToken(EstadoTokenQR2.INVALIDADO.name());
            tokenEntidad.setFechaInvalidacion(FechaMapperHelper.aOffsetDateTime(resultadoCruzado.getFechaValidacion()));
            tokenEntidad.setMotivoInactivacion(MotivoInactivacion.ENTREGA_EXITOSA.name());
            tokenRepository.save(tokenEntidad);

            // Actualizar encomienda a ENTREGADO vía el core
            cambiarEstadoService.cambiarEstado(encomiendaEntidad.getIdEncomienda(), EstadoLogistico.ENTREGADO);

            // Registrar evento en historial_tracking
            HistorialTracking tracking = HistorialTracking.crear(
                    encomiendaEntidad.getIdEncomienda(),
                    usuario.getIdSucursal(),
                    MapeadorEventoTracking.aEventoTracking(EstadoLogistico.ENTREGADO),
                    "Entrega física consumada mediante validación cruzada Doble QR",
                    resultadoCruzado.getFechaValidacion()
            );
            historialTrackingPort.registrarEvento(tracking);

            // Registrar auditoría operativa VALIDACION_ENTREGA
            auditoriaPort.registrar(
                    usuario.getIdEmpleado(),
                    ACCION_VALIDACION_ENTREGA,
                    TABLA_ENCOMIENDAS,
                    encomiendaEntidad.getIdEncomienda(),
                    ipOrigen
            );

            RespuestaValidacionEntregaDto respuestaDto = new RespuestaValidacionEntregaDto(
                    encomiendaEntidad.getIdEncomienda(),
                    encomiendaEntidad.getCodigoComprobante(),
                    ResultadoValidacion.EXITOSO.name(),
                    EstadoLogistico.ENTREGADO.name(),
                    "Entrega física autorizada y token QR2 invalidado de forma irreversible.",
                    resultadoCruzado.getLatenciaProcesamientoMs(),
                    resultadoCruzado.getFechaValidacion().toString()
            );
            return ResultadoProcesoValidacion.exitoso(respuestaDto, resultadoCruzado.getLatenciaProcesamientoMs());

        } else if (resultadoCruzado.getResultado() == ResultadoValidacion.FALLIDO_PAGO_PENDIENTE) {
            log.warn("Validación cruzada bloqueada por pago pendiente: Encomienda {}", encomiendaEntidad.getIdEncomienda());
            return ResultadoProcesoValidacion.pagoPendiente(
                    encomiendaEntidad.getIdEncomienda(),
                    encomiendaEntidad.getCodigoComprobante(),
                    encomiendaEntidad.getMontoTotal(),
                    resultadoCruzado.getLatenciaProcesamientoMs()
            );
        } else {
            log.warn("Validación cruzada fallida: Token QR2 inválido o ya utilizado para pareja {}", parEntidad.getIdParejaQr());
            return ResultadoProcesoValidacion.tokenInvalido(resultadoCruzado.getLatenciaProcesamientoMs());
        }
    }

    /**
     * Registra el cobro de una encomienda en modalidad Contra Entrega.
     * Desbloquea la encomienda pasando estado_pago a PAGADO.
     */
    @Transactional
    public ResultadoCobro registrarCobro(String idEncomienda, UsuarioAutenticado usuario, String ipOrigen) {
        Objects.requireNonNull(idEncomienda, "El ID de la encomienda no puede ser nulo");
        Objects.requireNonNull(usuario, "El usuario no puede ser nulo");

        EncomiendaJpaEntity encomiendaEntidad = encomiendaRepository.findById(idEncomienda)
                .orElse(null);

        if (encomiendaEntidad == null) {
            return ResultadoCobro.noEncontrado("No se encontró la encomienda con ID: " + idEncomienda);
        }

        // Si ya está PAGADO -> YA_PAGADA sin duplicar auditoría
        if (EstadoPago.PAGADO.name().equals(encomiendaEntidad.getEstadoPago())) {
            log.info("Intento de cobro sobre encomienda {} ya pagada.", idEncomienda);
            return ResultadoCobro.yaPagada("La encomienda con código " + encomiendaEntidad.getCodigoComprobante() + " ya se encuentra pagada.");
        }

        // Registrar cobro vía servicio de aplicación del core
        cambiarEstadoService.registrarCobro(idEncomienda);

        LocalDateTime ahora = LocalDateTime.now();

        // Registrar evento en historial_tracking con observacion "Cobro registrado"
        HistorialTracking tracking = HistorialTracking.crear(
                encomiendaEntidad.getIdEncomienda(),
                usuario.getIdSucursal(),
                MapeadorEventoTracking.aEventoTracking(EstadoLogistico.valueOf(encomiendaEntidad.getEstadoLogistico())),
                "Cobro registrado",
                ahora
        );
        historialTrackingPort.registrarEvento(tracking);

        // Registrar auditoría COBRO_CONTRA_ENTREGA
        auditoriaPort.registrar(
                usuario.getIdEmpleado(),
                ACCION_COBRO,
                TABLA_ENCOMIENDAS,
                encomiendaEntidad.getIdEncomienda(),
                ipOrigen
        );

        RespuestaCobroDto respuestaDto = new RespuestaCobroDto(
                encomiendaEntidad.getIdEncomienda(),
                encomiendaEntidad.getCodigoComprobante(),
                encomiendaEntidad.getMontoTotal(),
                EstadoPago.PAGADO.name(),
                encomiendaEntidad.getEstadoLogistico(),
                "Cobro registrado exitosamente. La encomienda ha sido desbloqueada para su validación cruzada y entrega.",
                ahora.toString()
        );

        return ResultadoCobro.exitoso(respuestaDto);
    }
}
