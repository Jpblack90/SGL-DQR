package pe.marvisur.sgldqr.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import pe.marvisur.sgldqr.api.dto.ClienteInputDto;
import pe.marvisur.sgldqr.api.dto.ItemResultadoSyncDto;
import pe.marvisur.sgldqr.api.dto.MotivoErrorSync;
import pe.marvisur.sgldqr.api.dto.QrSyncDto;
import pe.marvisur.sgldqr.api.dto.RespuestaSyncLoteDto;
import pe.marvisur.sgldqr.api.dto.ResultadoSync;
import pe.marvisur.sgldqr.api.dto.SyncEncomiendaItemDto;
import pe.marvisur.sgldqr.api.dto.SyncLoteRequest;
import pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.FechaMapperHelper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.aplicacion.ModalidadPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.dominio.modelo.HistorialTracking;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.TokenQR2Estatus;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;
import pe.marvisur.sgldqr.puertos.HistorialTrackingPort;
import pe.marvisur.sgldqr.puertos.RepositorioParDobleQRPort;
import pe.marvisur.sgldqr.puertos.TarifarioPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Servicio de sincronización de encomiendas creadas en ventanilla sin conexión (offline-first).
 *
 * <p><strong>Principios de Arquitectura:</strong>
 * <ul>
 *   <li>Cada ítem del lote se procesa en una transacción independiente mediante {@link TransactionTemplate}
 *       con propagación {@code REQUIRES_NEW}. Si un ítem falla, no afecta a los demás.</li>
 *   <li>Idempotencia estricta: si el {@code idEncomienda} ya existe en BD, no se duplican tracking,
 *       auditoría ni tokens, respondiendo {@code YA_SINCRONIZADA}.</li>
 *   <li>El servidor no regenera los QR: recibe los hashes y registra el token digital como ACTIVO.
 *       Nunca se loguean ni persisten valores en claro ni hashes sensibles.</li>
 *   <li>Recálculo de tarifa mediante {@link TarifarioPort}: si la tarifa local difiere de la oficial,
 *       se guarda la oficial y se informa la advertencia {@code MONTO_AJUSTADO}.</li>
 *   <li>Fecha efectiva acotada al reloj del servidor: nunca se admiten fechas de registro a futuro.</li>
 * </ul>
 * </p>
 */
@Service
public class SyncServicio {

    private static final Logger log = LoggerFactory.getLogger(SyncServicio.class);
    private static final Pattern PATRON_HEX_64 = Pattern.compile("^[0-9a-fA-F]{64}$");

    private final EncomiendaSpringDataRepository encomiendaRepo;
    private final ClienteSpringDataRepository clienteRepo;
    private final SucursalSpringDataRepository sucursalRepo;
    private final RepositorioParDobleQRPort parDobleQRPort;
    private final HistorialTrackingPort historialTrackingPort;
    private final AuditoriaPort auditoriaPort;
    private final TarifarioPort tarifarioPort;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.sync.lote-maximo:100}")
    private int loteMaximo;

    public SyncServicio(EncomiendaSpringDataRepository encomiendaRepo,
                        ClienteSpringDataRepository clienteRepo,
                        SucursalSpringDataRepository sucursalRepo,
                        RepositorioParDobleQRPort parDobleQRPort,
                        HistorialTrackingPort historialTrackingPort,
                        AuditoriaPort auditoriaPort,
                        TarifarioPort tarifarioPort,
                        PlatformTransactionManager transactionManager) {
        this.encomiendaRepo = Objects.requireNonNull(encomiendaRepo, "encomiendaRepo no puede ser nulo");
        this.clienteRepo = Objects.requireNonNull(clienteRepo, "clienteRepo no puede ser nulo");
        this.sucursalRepo = Objects.requireNonNull(sucursalRepo, "sucursalRepo no puede ser nulo");
        this.parDobleQRPort = Objects.requireNonNull(parDobleQRPort, "parDobleQRPort no puede ser nulo");
        this.historialTrackingPort = Objects.requireNonNull(historialTrackingPort, "historialTrackingPort no puede ser nulo");
        this.auditoriaPort = Objects.requireNonNull(auditoriaPort, "auditoriaPort no puede ser nulo");
        this.tarifarioPort = Objects.requireNonNull(tarifarioPort, "tarifarioPort no puede ser nulo");
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager no puede ser nulo");
    }

    /**
     * Sincroniza un lote de encomiendas registradas localmente en modo sin conexión.
     * Este método NO lleva @Transactional a nivel global para preservar el aislamiento
     * transaccional de cada ítem con REQUIRES_NEW.
     */
    public RespuestaSyncLoteDto sincronizarLote(SyncLoteRequest peticion,
                                                UsuarioAutenticado usuario,
                                                String ipOrigen) {
        Objects.requireNonNull(usuario, "El usuario autenticado no puede ser nulo");

        List<SyncEncomiendaItemDto> items = peticion != null ? peticion.getItems() : null;
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El lote de sincronización no puede estar vacío");
        }

        if (items.size() > loteMaximo) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El tamaño del lote (" + items.size() + ") supera el máximo permitido (" + loteMaximo + ")"
            );
        }

        boolean esAdmin = usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(ConstantesSeguridad.AUTH_ADMINISTRADOR));

        long inicioMs = System.currentTimeMillis();
        List<ItemResultadoSyncDto> resultados = new ArrayList<>(items.size());

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        int exitosos = 0;
        int yaSincronizados = 0;
        int errores = 0;

        for (SyncEncomiendaItemDto item : items) {
            ItemResultadoSyncDto res;
            try {
                res = txTemplate.execute(status -> procesarItemIndividual(item, usuario, ipOrigen, esAdmin, status));
            } catch (Exception e) {
                log.error("Excepción inesperada al procesar encomienda offline {}: {}",
                        item != null ? item.getIdEncomienda() : "null", e.getMessage());
                res = ItemResultadoSyncDto.error(
                        item != null ? item.getIdEncomienda() : null,
                        item != null ? item.getCodigoComprobante() : null,
                        MotivoErrorSync.ERROR_INTERNO,
                        "Error interno al procesar el ítem: " + e.getMessage()
                );
            }

            if (res == null) {
                res = ItemResultadoSyncDto.error(
                        item != null ? item.getIdEncomienda() : null,
                        item != null ? item.getCodigoComprobante() : null,
                        MotivoErrorSync.ERROR_INTERNO,
                        "Resultado de procesamiento nulo"
                );
            }

            resultados.add(res);
            if (res.getResultado() == ResultadoSync.OK) {
                exitosos++;
            } else if (res.getResultado() == ResultadoSync.YA_SINCRONIZADA) {
                yaSincronizados++;
            } else {
                errores++;
            }
        }

        long duracionMs = System.currentTimeMillis() - inicioMs;
        return new RespuestaSyncLoteDto(items.size(), exitosos, yaSincronizados, errores, duracionMs, resultados);
    }

    private ItemResultadoSyncDto procesarItemIndividual(SyncEncomiendaItemDto item,
                                                        UsuarioAutenticado usuario,
                                                        String ipOrigen,
                                                        boolean esAdmin,
                                                        TransactionStatus status) {
        if (item == null) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(null, null, MotivoErrorSync.VALIDACION, "El ítem no puede ser nulo");
        }

        String idEncomienda = item.getIdEncomienda() != null ? item.getIdEncomienda().trim() : "";
        String codigoComprobante = item.getCodigoComprobante() != null ? item.getCodigoComprobante().trim() : "";

        // 1. Idempotencia estricta: si ya existe en BD central, reportar como YA_SINCRONIZADA sin duplicar registros
        if (!idEncomienda.isBlank() && encomiendaRepo.existsById(idEncomienda)) {
            log.info("Encomienda {} ya se encuentra sincronizada en el servidor central (idempotencia)", idEncomienda);
            return ItemResultadoSyncDto.yaSincronizada(idEncomienda, codigoComprobante);
        }

        // 2. Validaciones estructurales y de formato
        if (idEncomienda.isBlank()) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.VALIDACION,
                    "El idEncomienda es obligatorio");
        }

        try {
            UUID.fromString(idEncomienda);
        } catch (IllegalArgumentException e) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.VALIDACION,
                    "El idEncomienda debe tener formato UUID válido");
        }

        if (codigoComprobante.isBlank()) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.VALIDACION,
                    "El código de comprobante es obligatorio");
        }

        if (!esClienteValido(item.getRemitente())) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.VALIDACION,
                    "Datos del remitente inválidos (DNI de 8 dígitos o RUC de 11 dígitos requeridos)");
        }

        if (!esClienteValido(item.getDestinatario())) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.VALIDACION,
                    "Datos del destinatario inválidos (DNI de 8 dígitos o RUC de 11 dígitos requeridos)");
        }

        if (item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.VALIDACION,
                    "El pesoKg debe ser mayor a 0");
        }

        if (item.getModalidadPago() == null) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.VALIDACION,
                    "La modalidad de pago es obligatoria (CONTADO o CONTRA_ENTREGA)");
        }

        QrSyncDto qr = item.getQr();
        if (qr == null) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.VALIDACION,
                    "El objeto qr con la pareja criptográfica es obligatorio");
        }

        if (qr.uuidQr1() == null || qr.uuidQr1().isBlank()) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.VALIDACION,
                    "El uuidQr1 es obligatorio");
        }

        if (qr.hashQr2() == null || !PATRON_HEX_64.matcher(qr.hashQr2().trim()).matches()) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.VALIDACION,
                    "El hashQr2 debe ser un hash SHA-256 hexadecimal válido de 64 caracteres");
        }

        // 3. Validación de correspondencia de sucursal de origen con la del empleado autenticado (salvo ADMIN)
        String sucursalOrigen = item.getIdSucursalOrigen() != null ? item.getIdSucursalOrigen().trim() : "";
        if (!esAdmin && !sucursalOrigen.equalsIgnoreCase(usuario.getIdSucursal())) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.SUCURSAL_NO_CORRESPONDE,
                    "La sucursal de origen (" + sucursalOrigen + ") no corresponde a la asignada al empleado (" + usuario.getIdSucursal() + ")");
        }

        // 4. Validación de existencia de sucursales en el catálogo central
        String sucursalDestino = item.getIdSucursalDestino() != null ? item.getIdSucursalDestino().trim() : "";
        if (sucursalOrigen.isBlank() || !sucursalRepo.existsById(sucursalOrigen)) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.SUCURSAL_INEXISTENTE,
                    "La sucursal de origen no existe: " + sucursalOrigen);
        }

        if (sucursalDestino.isBlank() || !sucursalRepo.existsById(sucursalDestino)) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.SUCURSAL_INEXISTENTE,
                    "La sucursal de destino no existe: " + sucursalDestino);
        }

        // 5. Validación de no duplicidad de código de comprobante para otra encomienda distinta
        Optional<EncomiendaJpaEntity> comprobanteExistente = encomiendaRepo.findByCodigoComprobante(codigoComprobante);
        if (comprobanteExistente.isPresent() && !comprobanteExistente.get().getIdEncomienda().equals(idEncomienda)) {
            status.setRollbackOnly();
            return ItemResultadoSyncDto.error(idEncomienda, codigoComprobante, MotivoErrorSync.CODIGO_COMPROBANTE_DUPLICADO,
                    "El código de comprobante ya se encuentra asignado a otra encomienda en el servidor central");
        }

        // 6. Clientes: reutilizar por número de documento o crear si no existe
        ClienteJpaEntity remitente = obtenerOCrearCliente(item.getRemitente());
        ClienteJpaEntity destinatario = obtenerOCrearCliente(item.getDestinatario());

        // 7. Recálculo oficial de tarifa con TarifarioPort
        BigDecimal tarifaOficial = tarifarioPort.calcularTarifa(item.getPesoKg(), item.getVolumenM3());
        BigDecimal montoFinal = tarifaOficial;
        String advertencia = null;

        if (item.getMontoTotal() == null || tarifaOficial.compareTo(item.getMontoTotal()) != 0) {
            advertencia = "MONTO_AJUSTADO";
            log.info("Tarifa ajustada para encomienda {}: recibido={}, oficial={}",
                    idEncomienda, item.getMontoTotal(), tarifaOficial);
        }

        // 8. Estado de pago y fecha efectiva
        String estadoPago = (item.getModalidadPago() == ModalidadPago.CONTADO)
                ? EstadoPago.PAGADO.name()
                : EstadoPago.PENDIENTE.name();

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaEfectiva = item.getFechaRegistro();
        if (fechaEfectiva == null || fechaEfectiva.isAfter(ahora)) {
            fechaEfectiva = ahora;
        }
        OffsetDateTime fechaRegistroOdt = FechaMapperHelper.aOffsetDateTime(fechaEfectiva);

        // 9. Persistir encomienda central (estado logístico REGISTRADO)
        EncomiendaJpaEntity encomiendaEntidad = new EncomiendaJpaEntity(
                idEncomienda,
                codigoComprobante,
                remitente.getIdCliente(),
                destinatario.getIdCliente(),
                sucursalOrigen,
                sucursalDestino,
                item.getPesoKg(),
                item.getVolumenM3(),
                item.getDescripcionContenido(),
                montoFinal,
                estadoPago,
                EstadoLogistico.REGISTRADO.name(),
                fechaRegistroOdt
        );
        encomiendaRepo.save(encomiendaEntidad);

        // 10. Persistir pareja Doble QR y estado ACTIVO del token QR2
        String idPareja = (qr.idPareja() != null && !qr.idPareja().isBlank())
                ? qr.idPareja().trim()
                : UUID.randomUUID().toString();

        TokenQR2Estatus tokenEstatus = TokenQR2Estatus.crearActivo(idPareja);
        ParDobleQR par = new ParDobleQR(
                idPareja,
                idEncomienda,
                qr.uuidQr1().trim(),
                qr.hashQr2().trim().toLowerCase(),
                fechaEfectiva,
                tokenEstatus
        );
        parDobleQRPort.guardar(par);

        // 11. Registrar hito de tracking con fecha efectiva
        HistorialTracking tracking = HistorialTracking.crear(
                idEncomienda,
                sucursalOrigen,
                "RECEPCIONADO",
                "Registrado sin conexión",
                fechaEfectiva
        );
        historialTrackingPort.registrarEvento(tracking);

        // 12. Registrar auditoría operativa en la MISMA transacción
        auditoriaPort.registrar(
                usuario.getIdEmpleado(),
                "REGISTRO_ENCOMIENDA_OFFLINE",
                "encomiendas",
                idEncomienda,
                ipOrigen
        );

        log.info("Encomienda {} sincronizada exitosamente con código {}", idEncomienda, codigoComprobante);
        return ItemResultadoSyncDto.ok(idEncomienda, codigoComprobante, advertencia);
    }

    private boolean esClienteValido(ClienteInputDto c) {
        if (c == null) return false;
        if (c.tipoDocumento() == null) return false;
        if (c.numeroDocumento() == null || c.numeroDocumento().isBlank()) return false;
        if (c.nombresRazonSocial() == null || c.nombresRazonSocial().isBlank()) return false;
        return c.isDocumentoValido();
    }

    private ClienteJpaEntity obtenerOCrearCliente(ClienteInputDto input) {
        String numDoc = input.numeroDocumento().trim();
        return clienteRepo.findByNumeroDocumento(numDoc)
                .orElseGet(() -> {
                    ClienteJpaEntity nuevo = new ClienteJpaEntity(
                            UUID.randomUUID().toString(),
                            input.tipoDocumento().name(),
                            numDoc,
                            input.nombresRazonSocial().trim(),
                            input.telefono(),
                            input.correo()
                    );
                    return clienteRepo.save(nuevo);
                });
    }
}
