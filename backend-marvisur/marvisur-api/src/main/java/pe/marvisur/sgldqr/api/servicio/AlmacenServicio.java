package pe.marvisur.sgldqr.api.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import pe.marvisur.sgldqr.api.dto.AccionLoteEscaneo;
import pe.marvisur.sgldqr.api.dto.EncomiendaBusquedaDto;
import pe.marvisur.sgldqr.api.dto.IncidenciaDetalleDto;
import pe.marvisur.sgldqr.api.dto.ItemResultadoLoteDto;
import pe.marvisur.sgldqr.api.dto.LoteEscaneoRequest;
import pe.marvisur.sgldqr.api.dto.MotivoErrorLote;
import pe.marvisur.sgldqr.api.dto.RegistrarIncidenciaRequest;
import pe.marvisur.sgldqr.api.dto.RespuestaIncidenciaDto;
import pe.marvisur.sgldqr.api.dto.RespuestaLoteEscaneoDto;
import pe.marvisur.sgldqr.api.dto.ResultadoItemLote;
import pe.marvisur.sgldqr.api.dto.ViajeDto;
import pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.DespachoLoteDetalleJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.HistorialTrackingJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.IncidenciaEmergenciaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.RutaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.VehiculoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.FechaMapperHelper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.DespachoLoteDetalleSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.HistorialTrackingSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.IncidenciaEmergenciaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ParDobleQrSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ProgramacionViajeSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RutaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.VehiculoSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;
import pe.marvisur.sgldqr.dominio.modelo.MapeadorEventoTracking;
import pe.marvisur.sgldqr.dominio.modelo.TipoIncidencia;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para las operaciones de almacén: listado de viajes para despacho,
 * escaneo masivo por lote con transacciones atómicas individuales, búsqueda de emergencia
 * por DNI/RUC y registro/reimpresión de incidencias.
 */
@Service
public class AlmacenServicio {

    private final ProgramacionViajeSpringDataRepository programacionViajeRepo;
    private final RutaSpringDataRepository rutaRepo;
    private final VehiculoSpringDataRepository vehiculoRepo;
    private final EmpleadoSpringDataRepository empleadoRepo;
    private final ParDobleQrSpringDataRepository parDobleQrRepo;
    private final EncomiendaSpringDataRepository encomiendaRepo;
    private final ClienteSpringDataRepository clienteRepo;
    private final DespachoLoteDetalleSpringDataRepository despachoRepo;
    private final IncidenciaEmergenciaSpringDataRepository incidenciaRepo;
    private final HistorialTrackingSpringDataRepository trackingRepo;
    private final AuditoriaPort auditoriaPort;
    private final ImagenQRPort imagenQRPort;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.almacen.lote-maximo:200}")
    private int loteMaximo;

    public AlmacenServicio(ProgramacionViajeSpringDataRepository programacionViajeRepo,
                           RutaSpringDataRepository rutaRepo,
                           VehiculoSpringDataRepository vehiculoRepo,
                           EmpleadoSpringDataRepository empleadoRepo,
                           ParDobleQrSpringDataRepository parDobleQrRepo,
                           EncomiendaSpringDataRepository encomiendaRepo,
                           ClienteSpringDataRepository clienteRepo,
                           DespachoLoteDetalleSpringDataRepository despachoRepo,
                           IncidenciaEmergenciaSpringDataRepository incidenciaRepo,
                           HistorialTrackingSpringDataRepository trackingRepo,
                           AuditoriaPort auditoriaPort,
                           ImagenQRPort imagenQRPort,
                           PlatformTransactionManager transactionManager) {
        this.programacionViajeRepo = Objects.requireNonNull(programacionViajeRepo);
        this.rutaRepo = Objects.requireNonNull(rutaRepo);
        this.vehiculoRepo = Objects.requireNonNull(vehiculoRepo);
        this.empleadoRepo = Objects.requireNonNull(empleadoRepo);
        this.parDobleQrRepo = Objects.requireNonNull(parDobleQrRepo);
        this.encomiendaRepo = Objects.requireNonNull(encomiendaRepo);
        this.clienteRepo = Objects.requireNonNull(clienteRepo);
        this.despachoRepo = Objects.requireNonNull(despachoRepo);
        this.incidenciaRepo = Objects.requireNonNull(incidenciaRepo);
        this.trackingRepo = Objects.requireNonNull(trackingRepo);
        this.auditoriaPort = Objects.requireNonNull(auditoriaPort);
        this.imagenQRPort = Objects.requireNonNull(imagenQRPort);
        this.transactionManager = Objects.requireNonNull(transactionManager);
    }

    // ------------------------------------------------------------------------
    // 1. Listar Viajes (Solo lectura, filtros por estado e idSucursalOrigen)
    // ------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ViajeDto> listarViajes(EstadoViaje estado, String idSucursalOrigen) {
        String estadoStr = estado != null ? estado.name() : null;
        List<ProgramacionViajeJpaEntity> viajes = programacionViajeRepo.listarPorFiltros(estadoStr, idSucursalOrigen);

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

        return viajes.stream().map(v -> {
            RutaJpaEntity ruta = rutasMap.get(v.getIdRuta());
            VehiculoJpaEntity vehiculo = vehiculosMap.get(v.getIdVehiculo());
            EmpleadoJpaEntity chofer = choferesMap.get(v.getIdChofer());

            String nombreChofer = chofer != null ? chofer.getNombres() + " " + chofer.getApellidos() : "Sin chofer";
            String placa = vehiculo != null ? vehiculo.getPlaca() : "";

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
                    EstadoViaje.valueOf(v.getEstadoViaje())
            );
        }).toList();
    }

    // ------------------------------------------------------------------------
    // 2. Procesar Lote de Escaneo Masivo (Transacciones Atómicas por Ítem)
    // ------------------------------------------------------------------------
    public RespuestaLoteEscaneoDto procesarLote(LoteEscaneoRequest peticion, UsuarioAutenticado usuario, String ipOrigen) {
        if (peticion.getQr1List() == null || peticion.getQr1List().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La lista de QR1 no puede estar vacía");
        }
        if (peticion.getQr1List().size() > loteMaximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El tamaño del lote (" + peticion.getQr1List().size() + ") excede el límite configurado de " + loteMaximo + " ítems");
        }
        if (peticion.getAccion() == AccionLoteEscaneo.DESPACHAR_A_VIAJE
                && (peticion.getIdViaje() == null || peticion.getIdViaje().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El idViaje es obligatorio para la acción DESPACHAR_A_VIAJE");
        }

        long inicioMs = System.currentTimeMillis();
        List<ItemResultadoLoteDto> resultados = new ArrayList<>(peticion.getQr1List().size());

        // Template con REQUIRES_NEW para garantizar que cada ítem se ejecute en su propia transacción independiente
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());

        int exitosos = 0;
        int yaRegistrados = 0;
        int errores = 0;

        for (String qr1 : peticion.getQr1List()) {
            ItemResultadoLoteDto res = txTemplate.execute(status -> procesarItemIndividual(qr1, peticion, usuario, ipOrigen));
            if (res == null) {
                res = ItemResultadoLoteDto.error(qr1, null, MotivoErrorLote.QR1_INEXISTENTE);
            }
            resultados.add(res);

            if (res.getResultado() == ResultadoItemLote.OK) {
                exitosos++;
            } else if (res.getResultado() == ResultadoItemLote.YA_REGISTRADA) {
                yaRegistrados++;
            } else {
                errores++;
            }
        }

        long duracionMs = System.currentTimeMillis() - inicioMs;
        return new RespuestaLoteEscaneoDto(
                peticion.getQr1List().size(),
                exitosos,
                yaRegistrados,
                errores,
                duracionMs,
                resultados
        );
    }

    private ItemResultadoLoteDto procesarItemIndividual(String qr1, LoteEscaneoRequest peticion, UsuarioAutenticado usuario, String ipOrigen) {
        if (qr1 == null || qr1.isBlank()) {
            return ItemResultadoLoteDto.error(qr1, null, MotivoErrorLote.QR1_INEXISTENTE);
        }

        // Búsqueda del par Doble QR por el payload o UUID visible QR1
        Optional<ParDobleQrJpaEntity> parOpt = parDobleQrRepo.findByUuidQr1Operativo(qr1.trim());
        if (parOpt.isEmpty()) {
            return ItemResultadoLoteDto.error(qr1, null, MotivoErrorLote.QR1_INEXISTENTE);
        }

        ParDobleQrJpaEntity par = parOpt.get();
        String idEncomienda = par.getIdEncomienda();

        Optional<EncomiendaJpaEntity> encomiendaOpt = encomiendaRepo.findById(idEncomienda);
        if (encomiendaOpt.isEmpty()) {
            return ItemResultadoLoteDto.error(qr1, idEncomienda, MotivoErrorLote.QR1_INEXISTENTE);
        }

        EncomiendaJpaEntity encomienda = encomiendaOpt.get();

        return switch (peticion.getAccion()) {
            case RECEPCIONAR_ORIGEN -> procesarRecepcionOrigen(qr1, encomienda, usuario, ipOrigen);
            case DESPACHAR_A_VIAJE -> procesarDespachoAViaje(qr1, encomienda, peticion.getIdViaje(), usuario, ipOrigen);
            case RECEPCIONAR_DESTINO -> procesarRecepcionDestino(qr1, encomienda, usuario, ipOrigen);
        };
    }

    private ItemResultadoLoteDto procesarRecepcionOrigen(String qr1, EncomiendaJpaEntity encomienda, UsuarioAutenticado usuario, String ipOrigen) {
        // Idempotencia: Si ya está en EN_ALMACEN_ORIGEN, no repetir tracking ni auditoría
        if (EstadoLogistico.EN_ALMACEN_ORIGEN.name().equals(encomienda.getEstadoLogistico())) {
            return ItemResultadoLoteDto.yaRegistrada(qr1, encomienda.getIdEncomienda());
        }

        // Precondición de estado: Solo de REGISTRADO a EN_ALMACEN_ORIGEN
        if (!EstadoLogistico.REGISTRADO.name().equals(encomienda.getEstadoLogistico())) {
            return ItemResultadoLoteDto.error(qr1, encomienda.getIdEncomienda(), MotivoErrorLote.ESTADO_INVALIDO);
        }

        // Precondición de sucursal: La sucursal de origen debe coincidir con la del empleado
        if (!Objects.equals(encomienda.getIdSucursalOrigen(), usuario.getIdSucursal())) {
            return ItemResultadoLoteDto.error(qr1, encomienda.getIdEncomienda(), MotivoErrorLote.SUCURSAL_NO_CORRESPONDE);
        }

        // Transición atómica
        encomienda.setEstadoLogistico(EstadoLogistico.EN_ALMACEN_ORIGEN.name());
        encomiendaRepo.save(encomienda);

        // Registro de hito de tracking
        String evento = MapeadorEventoTracking.aEventoTracking(EstadoLogistico.EN_ALMACEN_ORIGEN);
        HistorialTrackingJpaEntity tracking = new HistorialTrackingJpaEntity(
                UUID.randomUUID().toString(),
                encomienda.getIdEncomienda(),
                usuario.getIdSucursal(),
                evento,
                "Recepcionado y clasificado en almacén de origen",
                OffsetDateTime.now()
        );
        trackingRepo.save(tracking);

        // Auditoría
        auditoriaPort.registrar(usuario.getIdEmpleado(), "LOTE_RECEPCION_ORIGEN", "encomiendas", encomienda.getIdEncomienda(), ipOrigen);

        return ItemResultadoLoteDto.ok(qr1, encomienda.getIdEncomienda());
    }

    private ItemResultadoLoteDto procesarDespachoAViaje(String qr1, EncomiendaJpaEntity encomienda, String idViaje, UsuarioAutenticado usuario, String ipOrigen) {
        // Validar existencia de la programación de viaje
        Optional<ProgramacionViajeJpaEntity> viajeOpt = programacionViajeRepo.findById(idViaje);
        if (viajeOpt.isEmpty()) {
            return ItemResultadoLoteDto.error(qr1, encomienda.getIdEncomienda(), MotivoErrorLote.VIAJE_INEXISTENTE);
        }

        ProgramacionViajeJpaEntity viaje = viajeOpt.get();
        Optional<RutaJpaEntity> rutaOpt = rutaRepo.findById(viaje.getIdRuta());
        if (rutaOpt.isEmpty()) {
            return ItemResultadoLoteDto.error(qr1, encomienda.getIdEncomienda(), MotivoErrorLote.VIAJE_INEXISTENTE);
        }

        RutaJpaEntity ruta = rutaOpt.get();

        // Idempotencia: Si ya está en EN_RUTA y asociada al mismo viaje
        if (EstadoLogistico.EN_RUTA.name().equals(encomienda.getEstadoLogistico())
                && despachoRepo.existsByIdViajeAndIdEncomienda(idViaje, encomienda.getIdEncomienda())) {
            return ItemResultadoLoteDto.yaRegistrada(qr1, encomienda.getIdEncomienda());
        }

        // Precondición de estado: Solo desde EN_ALMACEN_ORIGEN puede pasar a EN_RUTA
        if (!EstadoLogistico.EN_ALMACEN_ORIGEN.name().equals(encomienda.getEstadoLogistico())) {
            return ItemResultadoLoteDto.error(qr1, encomienda.getIdEncomienda(), MotivoErrorLote.ESTADO_INVALIDO);
        }

        // Precondición de ruta de origen: La ruta debe partir de la sucursal del empleado
        if (!Objects.equals(ruta.getIdSucursalOrigen(), usuario.getIdSucursal())) {
            return ItemResultadoLoteDto.error(qr1, encomienda.getIdEncomienda(), MotivoErrorLote.SUCURSAL_NO_CORRESPONDE);
        }

        // Precondición de destino: La ruta debe llegar al destino de la encomienda
        if (!Objects.equals(ruta.getIdSucursalDestino(), encomienda.getIdSucursalDestino())) {
            return ItemResultadoLoteDto.error(qr1, encomienda.getIdEncomienda(), MotivoErrorLote.DESTINO_NO_COINCIDE);
        }

        // Precondición: No puede estar en dos viajes activos/no finalizados
        if (despachoRepo.existeEncomiendaEnViajeActivo(encomienda.getIdEncomienda())) {
            return ItemResultadoLoteDto.error(qr1, encomienda.getIdEncomienda(), MotivoErrorLote.YA_DESPACHADA);
        }

        // Transición de estado a EN_RUTA
        encomienda.setEstadoLogistico(EstadoLogistico.EN_RUTA.name());
        encomiendaRepo.save(encomienda);

        // Registro de detalle de despacho por lote
        DespachoLoteDetalleJpaEntity detalle = new DespachoLoteDetalleJpaEntity(
                UUID.randomUUID().toString(),
                idViaje,
                encomienda.getIdEncomienda(),
                OffsetDateTime.now()
        );
        despachoRepo.save(detalle);

        // Registro de tracking
        String evento = MapeadorEventoTracking.aEventoTracking(EstadoLogistico.EN_RUTA);
        HistorialTrackingJpaEntity tracking = new HistorialTrackingJpaEntity(
                UUID.randomUUID().toString(),
                encomienda.getIdEncomienda(),
                usuario.getIdSucursal(),
                evento,
                "Despachado en lote para viaje " + idViaje,
                OffsetDateTime.now()
        );
        trackingRepo.save(tracking);

        // Auditoría
        auditoriaPort.registrar(usuario.getIdEmpleado(), "LOTE_DESPACHO", "encomiendas", encomienda.getIdEncomienda(), ipOrigen);

        return ItemResultadoLoteDto.ok(qr1, encomienda.getIdEncomienda());
    }

    private ItemResultadoLoteDto procesarRecepcionDestino(String qr1, EncomiendaJpaEntity encomienda, UsuarioAutenticado usuario, String ipOrigen) {
        // Idempotencia: Si ya está en EN_ALMACEN_DESTINO
        if (EstadoLogistico.EN_ALMACEN_DESTINO.name().equals(encomienda.getEstadoLogistico())) {
            return ItemResultadoLoteDto.yaRegistrada(qr1, encomienda.getIdEncomienda());
        }

        // Precondición de estado: Solo desde EN_RUTA pasa a EN_ALMACEN_DESTINO
        if (!EstadoLogistico.EN_RUTA.name().equals(encomienda.getEstadoLogistico())) {
            return ItemResultadoLoteDto.error(qr1, encomienda.getIdEncomienda(), MotivoErrorLote.ESTADO_INVALIDO);
        }

        // Precondición de sucursal: La sucursal de destino debe coincidir con la del empleado
        if (!Objects.equals(encomienda.getIdSucursalDestino(), usuario.getIdSucursal())) {
            return ItemResultadoLoteDto.error(qr1, encomienda.getIdEncomienda(), MotivoErrorLote.SUCURSAL_NO_CORRESPONDE);
        }

        // Transición de estado
        encomienda.setEstadoLogistico(EstadoLogistico.EN_ALMACEN_DESTINO.name());
        encomiendaRepo.save(encomienda);

        // Registro de tracking
        String evento = MapeadorEventoTracking.aEventoTracking(EstadoLogistico.EN_ALMACEN_DESTINO);
        HistorialTrackingJpaEntity tracking = new HistorialTrackingJpaEntity(
                UUID.randomUUID().toString(),
                encomienda.getIdEncomienda(),
                usuario.getIdSucursal(),
                evento,
                "Arribado y recepcionado en almacén de destino",
                OffsetDateTime.now()
        );
        trackingRepo.save(tracking);

        // Auditoría
        auditoriaPort.registrar(usuario.getIdEmpleado(), "LOTE_RECEPCION_DESTINO", "encomiendas", encomienda.getIdEncomienda(), ipOrigen);

        return ItemResultadoLoteDto.ok(qr1, encomienda.getIdEncomienda());
    }

    // ------------------------------------------------------------------------
    // 3. Búsqueda de Emergencia por DNI/RUC
    // ------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<EncomiendaBusquedaDto> buscarEncomiendasPorDocumento(String documento, UsuarioAutenticado usuario) {
        if (documento == null || documento.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de documento de búsqueda es obligatorio");
        }

        String docLimpio = documento.trim();
        boolean esAdmin = usuario.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"));
        String idSucursalFiltro = esAdmin ? null : usuario.getIdSucursal();

        List<EncomiendaJpaEntity> entidades = encomiendaRepo.buscarPorDocumentoYAlcanceSucursal(docLimpio, idSucursalFiltro);
        if (entidades.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> idClientes = new HashSet<>();
        for (EncomiendaJpaEntity e : entidades) {
            idClientes.add(e.getIdRemitente());
            idClientes.add(e.getIdDestinatario());
        }

        Map<String, ClienteJpaEntity> clientesMap = clienteRepo.findAllById(idClientes).stream()
                .collect(Collectors.toMap(ClienteJpaEntity::getIdCliente, Function.identity()));

        return entidades.stream().map(e -> {
            ClienteJpaEntity rem = clientesMap.get(e.getIdRemitente());
            ClienteJpaEntity dest = clientesMap.get(e.getIdDestinatario());
            return new EncomiendaBusquedaDto(
                    e.getIdEncomienda(),
                    e.getCodigoComprobante(),
                    rem != null ? rem.getNumeroDocumento() : "",
                    rem != null ? rem.getNombresRazonSocial() : "",
                    dest != null ? dest.getNumeroDocumento() : "",
                    dest != null ? dest.getNombresRazonSocial() : "",
                    e.getIdSucursalOrigen(),
                    e.getIdSucursalDestino(),
                    e.getPesoKg(),
                    e.getMontoTotal(),
                    e.getEstadoPago(),
                    e.getEstadoLogistico(),
                    FechaMapperHelper.aLocalDateTime(e.getFechaRegistro())
            );
        }).toList();
    }

    // ------------------------------------------------------------------------
    // 4. Registro de Incidencia de Emergencia y Reimpresión de QR1
    // ------------------------------------------------------------------------
    @Transactional
    public RespuestaIncidenciaDto registrarIncidencia(RegistrarIncidenciaRequest peticion, UsuarioAutenticado usuario, String ipOrigen) {
        EncomiendaJpaEntity encomienda = encomiendaRepo.findById(peticion.getIdEncomienda())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomienda no encontrada con ID: " + peticion.getIdEncomienda()));

        if (EstadoLogistico.ENTREGADO.name().equals(encomienda.getEstadoLogistico())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede registrar incidencia en una encomienda que ya ha sido entregada");
        }

        ParDobleQrJpaEntity par = parDobleQrRepo.findByIdEncomienda(encomienda.getIdEncomienda())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el par Doble QR de la encomienda"));

        String idIncidencia = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();

        IncidenciaEmergenciaJpaEntity incidencia = new IncidenciaEmergenciaJpaEntity(
                idIncidencia,
                encomienda.getIdEncomienda(),
                usuario.getIdEmpleado(),
                peticion.getTipoIncidencia().name(),
                peticion.getMotivoReimpresion(),
                now
        );
        incidenciaRepo.save(incidencia);

        // Auditoría
        auditoriaPort.registrar(usuario.getIdEmpleado(), "INCIDENCIA_REGISTRADA", "incidencias_emergencia", idIncidencia, ipOrigen);

        // Regenerar imagen PNG del QR1 operativo desde uuid_qr1_operativo
        byte[] qr1Png = imagenQRPort.generarPNG(par.getUuidQr1Operativo(), 300);
        String qr1Base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(qr1Png);

        return new RespuestaIncidenciaDto(
                idIncidencia,
                encomienda.getIdEncomienda(),
                usuario.getIdEmpleado(),
                peticion.getTipoIncidencia(),
                peticion.getMotivoReimpresion(),
                FechaMapperHelper.aLocalDateTime(now),
                qr1Base64
        );
    }

    // ------------------------------------------------------------------------
    // 5. Listado de Incidencias Reportadas
    // ------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<IncidenciaDetalleDto> listarIncidencias(String idEncomienda) {
        List<IncidenciaEmergenciaJpaEntity> lista;
        if (idEncomienda != null && !idEncomienda.isBlank()) {
            lista = incidenciaRepo.findByIdEncomiendaOrderByFechaIncidenciaDesc(idEncomienda);
        } else {
            lista = incidenciaRepo.findAll();
        }

        return lista.stream().map(i -> new IncidenciaDetalleDto(
                i.getIdIncidencia(),
                i.getIdEncomienda(),
                i.getIdEmpleadoReporta(),
                TipoIncidencia.valueOf(i.getTipoIncidencia()),
                i.getMotivoReimpresion(),
                FechaMapperHelper.aLocalDateTime(i.getFechaIncidencia())
        )).toList();
    }
}
