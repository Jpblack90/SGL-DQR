package pe.marvisur.sgldqr.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.marvisur.sgldqr.api.dto.BilleteraRequest;
import pe.marvisur.sgldqr.api.dto.ClienteInputDto;
import pe.marvisur.sgldqr.api.dto.CotizacionRequest;
import pe.marvisur.sgldqr.api.dto.CotizacionResponse;
import pe.marvisur.sgldqr.api.dto.CrearPreRegistroPublicoRequest;
import pe.marvisur.sgldqr.api.dto.DetallePreRegistroVentanillaDto;
import pe.marvisur.sgldqr.api.dto.EventoTrackingPublicoDto;
import pe.marvisur.sgldqr.api.dto.RastreoPublicoDto;
import pe.marvisur.sgldqr.api.dto.RespuestaBilleteraDto;
import pe.marvisur.sgldqr.api.dto.RespuestaPreRegistroPublicoDto;
import pe.marvisur.sgldqr.api.dto.SucursalPublicaDto;
import pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPreRegistro;
import pe.marvisur.sgldqr.dominio.modelo.HistorialTracking;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.PreRegistroExpress;
import pe.marvisur.sgldqr.dominio.modelo.Sucursal;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;
import pe.marvisur.sgldqr.dominio.modelo.TokenQR2Estatus;
import pe.marvisur.sgldqr.dominio.servicio.MotorQR;
import pe.marvisur.sgldqr.puertos.HistorialTrackingPort;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;
import pe.marvisur.sgldqr.puertos.RepositorioParDobleQRPort;
import pe.marvisur.sgldqr.puertos.RepositorioPreRegistroExpressPort;
import pe.marvisur.sgldqr.puertos.RepositorioSucursalPort;
import pe.marvisur.sgldqr.puertos.TarifarioPort;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de aplicación para las operaciones del portal web público (RF-08, RF-09, RF-10).
 *
 * <p><strong>Principios de diseño:</strong></p>
 * <ul>
 *   <li>Fricción Cero: acceso sin credenciales para el cliente final.</li>
 *   <li>Privacidad y Seguridad: nunca se exponen datos personales completos ni tokens en logs o respuestas.</li>
 *   <li>Auditoría: las consultas públicas no se insertan en 'auditoria_logs' (esta requiere idEmpleado);
 *       se registran en los logs de la aplicación usando identificadores no sensibles.</li>
 * </ul>
 */
@Service
public class PublicoServicio {

    private static final Logger log = LoggerFactory.getLogger(PublicoServicio.class);
    private static final String MONEDA_PEN = "PEN";
    private static final String ERROR_RASTREO_NO_ENCONTRADO = "No se encontró la encomienda con los datos proporcionados";

    private final RepositorioSucursalPort repositorioSucursalPort;
    private final TarifarioPort tarifarioPort;
    private final EncomiendaSpringDataRepository encomiendaRepository;
    private final ClienteSpringDataRepository clienteRepository;
    private final HistorialTrackingPort historialTrackingPort;
    private final RepositorioPreRegistroExpressPort repositorioPreRegistroPort;
    private final RepositorioParDobleQRPort parDobleQRPort;
    private final ImagenQRPort imagenQRPort;
    private final MotorQR motorQR;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.preregistro.vigencia-horas:48}")
    private int vigenciaHoras;

    public PublicoServicio(RepositorioSucursalPort repositorioSucursalPort,
                           TarifarioPort tarifarioPort,
                           EncomiendaSpringDataRepository encomiendaRepository,
                           ClienteSpringDataRepository clienteRepository,
                           HistorialTrackingPort historialTrackingPort,
                           RepositorioPreRegistroExpressPort repositorioPreRegistroPort,
                           RepositorioParDobleQRPort parDobleQRPort,
                           ImagenQRPort imagenQRPort,
                           MotorQR motorQR) {
        this.repositorioSucursalPort = Objects.requireNonNull(repositorioSucursalPort, "El repositorioSucursalPort no puede ser nulo");
        this.tarifarioPort = Objects.requireNonNull(tarifarioPort, "El tarifarioPort no puede ser nulo");
        this.encomiendaRepository = Objects.requireNonNull(encomiendaRepository, "El encomiendaRepository no puede ser nulo");
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "El clienteRepository no puede ser nulo");
        this.historialTrackingPort = Objects.requireNonNull(historialTrackingPort, "El historialTrackingPort no puede ser nulo");
        this.repositorioPreRegistroPort = Objects.requireNonNull(repositorioPreRegistroPort, "El repositorioPreRegistroPort no puede ser nulo");
        this.parDobleQRPort = Objects.requireNonNull(parDobleQRPort, "El parDobleQRPort no puede ser nulo");
        this.imagenQRPort = Objects.requireNonNull(imagenQRPort, "El imagenQRPort no puede ser nulo");
        this.motorQR = Objects.requireNonNull(motorQR, "El motorQR no puede ser nulo");
    }

    /**
     * Lista todas las sucursales activas para poblar los selectores del portal web.
     */
    @Transactional(readOnly = true)
    public List<SucursalPublicaDto> listarSucursales() {
        return repositorioSucursalPort.listarTodas()
                .stream()
                .sorted(Comparator.comparing(Sucursal::getNombreAgencia))
                .map(s -> new SucursalPublicaDto(s.getIdSucursal(), s.getNombreAgencia(), s.getCiudad(), s.getDireccion()))
                .toList();
    }

    /**
     * Cotiza el costo estimado de envío según el peso oficial.
     *
     * <p><strong>Nota:</strong> TODO: La tarifa por tipo de mercadería no está definida en Marvisur.</p>
     */
    public CotizacionResponse cotizar(CotizacionRequest request) {
        Objects.requireNonNull(request, "La solicitud de cotización no puede ser nula");
        BigDecimal montoEstimado = tarifarioPort.calcularTarifa(request.pesoKg(), request.volumenM3());
        return new CotizacionResponse(montoEstimado, MONEDA_PEN);
    }

    /**
     * Consulta pública de rastreo y tracking de encomiendas con autorización por DNI/RUC.
     * Si el número de documento no coincide con el remitente o destinatario, se retorna 404 genérico.
     */
    @Transactional(readOnly = true)
    public RastreoPublicoDto consultarRastreo(String comprobante, String documento) {
        if (comprobante == null || comprobante.isBlank() || documento == null || documento.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los parámetros comprobante y documento son obligatorios.");
        }

        String comprobanteLimpio = comprobante.trim();
        String docLimpio = documento.trim();

        // 1. Buscar encomienda por comprobante
        Optional<EncomiendaJpaEntity> encOpt = encomiendaRepository.findByCodigoComprobante(comprobanteLimpio);
        if (encOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ERROR_RASTREO_NO_ENCONTRADO);
        }
        EncomiendaJpaEntity enc = encOpt.get();

        // 2. Buscar clientes remitente y destinatario para autorizar
        ClienteJpaEntity remitente = clienteRepository.findById(enc.getIdRemitente()).orElse(null);
        ClienteJpaEntity destinatario = clienteRepository.findById(enc.getIdDestinatario()).orElse(null);

        boolean autorizado = (remitente != null && docLimpio.equalsIgnoreCase(remitente.getNumeroDocumento())) ||
                             (destinatario != null && docLimpio.equalsIgnoreCase(destinatario.getNumeroDocumento()));

        if (!autorizado) {
            // Error genérico idéntico: no revela si el comprobante existe
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ERROR_RASTREO_NO_ENCONTRADO);
        }

        // 3. Obtener nombres de sucursales
        Sucursal origen = repositorioSucursalPort.buscarPorId(enc.getIdSucursalOrigen()).orElse(null);
        Sucursal destino = repositorioSucursalPort.buscarPorId(enc.getIdSucursalDestino()).orElse(null);

        String sucursalOrigen = origen != null ? origen.getNombreAgencia() : "Agencia Origen";
        String ciudadOrigen = origen != null ? origen.getCiudad() : "";
        String sucursalDestino = destino != null ? destino.getNombreAgencia() : "Agencia Destino";
        String ciudadDestino = destino != null ? destino.getCiudad() : "";

        // 4. Línea de tiempo de eventos tracking
        List<HistorialTracking> eventos = historialTrackingPort.listarPorEncomienda(enc.getIdEncomienda());
        List<EventoTrackingPublicoDto> lineaDeTiempo = eventos.stream()
                .sorted(Comparator.comparing(HistorialTracking::getFechaHoraEvento))
                .map(ev -> new EventoTrackingPublicoDto(
                        ev.getEstadoEvento(),
                        obtenerNombreSucursal(ev.getIdSucursalActual()),
                        ev.getFechaHoraEvento()
                ))
                .toList();

        // 5. Enmascarar nombres para privacidad
        String nombreRemitenteEnmascarado = enmascararNombre(remitente != null ? remitente.getNombresRazonSocial() : "");
        String nombreDestinatarioEnmascarado = enmascararNombre(destinatario != null ? destinatario.getNombresRazonSocial() : "");

        log.info("Consulta pública de rastreo exitosa para comprobante: {}", enc.getCodigoComprobante());

        return new RastreoPublicoDto(
                enc.getCodigoComprobante(),
                enc.getEstadoLogistico(),
                enc.getEstadoPago(),
                enc.getMontoTotal(),
                sucursalOrigen,
                ciudadOrigen,
                sucursalDestino,
                ciudadDestino,
                nombreRemitenteEnmascarado,
                nombreDestinatarioEnmascarado,
                lineaDeTiempo
        );
    }

    /**
     * Registra una orden de pre-registro web ('Código Express') para agilizar la atención en ventanilla.
     */
    @Transactional
    public RespuestaPreRegistroPublicoDto crearPreRegistro(CrearPreRegistroPublicoRequest request) {
        Objects.requireNonNull(request, "La solicitud de pre-registro no puede ser nula");

        // 1. Clientes: buscar por número de documento o crear sin sobrescribir
        ClienteJpaEntity remitente = obtenerOCrearCliente(request.remitente());
        ClienteJpaEntity destinatario = obtenerOCrearCliente(request.destinatario());

        // 2. Cotizar tarifa
        BigDecimal montoCotizado = tarifarioPort.calcularTarifa(request.pesoEstimado(), BigDecimal.ZERO);

        // 3. Generar código express único ("PRE-XXXXXX-PE") con reintentos
        String codigoExpress = generarCodigoExpressUnico();

        // 4. Crear entidad de dominio y persistir
        LocalDateTime ahora = LocalDateTime.now();
        PreRegistroExpress preRegistro = PreRegistroExpress.crearNuevo(
                codigoExpress,
                remitente.getIdCliente(),
                destinatario.getIdCliente(),
                request.idSucursalDestino(),
                request.pesoEstimado(),
                montoCotizado,
                ahora
        );
        repositorioPreRegistroPort.guardar(preRegistro);

        // 5. Preparar datos de respuesta
        Sucursal destino = repositorioSucursalPort.buscarPorId(request.idSucursalDestino()).orElse(null);
        String nombreSucursalDestino = destino != null ? destino.getNombreAgencia() : request.idSucursalDestino();
        LocalDateTime fechaExpiracion = preRegistro.calcularFechaExpiracion(vigenciaHoras);

        log.info("Pre-registro web generado exitosamente: {}", codigoExpress);

        return new RespuestaPreRegistroPublicoDto(
                codigoExpress,
                fechaExpiracion,
                montoCotizado,
                request.pesoEstimado(),
                nombreSucursalDestino,
                enmascararNombre(remitente.getNombresRazonSocial()),
                enmascararNombre(destinatario.getNombresRazonSocial())
        );
    }

    /**
     * Consulta pública de Billetera Digital recibiendo el token en el cuerpo (POST).
     * Hashea el token con SHA-256 para buscar la orden y genera la imagen QR2 al vuelo con ImagenQRPort.
     */
    @Transactional(readOnly = true)
    public RespuestaBilleteraDto consultarBilletera(BilleteraRequest request) {
        Objects.requireNonNull(request, "La solicitud de billetera no puede ser nula");
        String tokenPlano = request.token().trim();

        // 1. Calcular hash SHA-256 del token para búsqueda en BD
        String hashQr2 = motorQR.calcularSha256Hex(tokenPlano);

        // 2. Buscar en parejas_doble_qr por hash_qr2_llave_digital
        Optional<ParDobleQR> parOpt = parDobleQRPort.buscarPorHashQr2(hashQr2);
        if (parOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró información para el enlace de retiro proporcionado.");
        }
        ParDobleQR par = parOpt.get();

        // 3. Buscar datos de la encomienda
        EncomiendaJpaEntity enc = encomiendaRepository.findById(par.getIdEncomienda())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró información para el enlace de retiro proporcionado."));

        // 4. Consultar estatus del token QR2
        TokenQR2Estatus tokenEstatus = parDobleQRPort.buscarTokenEstatusPorParejaId(par.getIdParejaQr()).orElse(null);
        String estadoToken = tokenEstatus != null ? tokenEstatus.getEstadoToken().name() : "DESCONOCIDO";

        // 5. Obtener agencia de destino
        Sucursal destino = repositorioSucursalPort.buscarPorId(enc.getIdSucursalDestino()).orElse(null);
        String nombreAgenciaDestino = destino != null ? destino.getNombreAgencia() : "Agencia de Destino";
        String ciudadDestino = destino != null ? destino.getCiudad() : "";

        // 6. Generar imagen PNG en base64 del QR2 a partir del token recibido
        byte[] qr2Bytes = imagenQRPort.generarPNG(tokenPlano, 300);
        String qr2Base64 = Base64.getEncoder().encodeToString(qr2Bytes);

        log.info("Consulta pública de billetera digital procesada exitosamente para comprobante: {}", enc.getCodigoComprobante());

        return new RespuestaBilleteraDto(
                enc.getCodigoComprobante(),
                enc.getEstadoLogistico(),
                enc.getEstadoPago(),
                enc.getMontoTotal(),
                nombreAgenciaDestino,
                ciudadDestino,
                estadoToken,
                qr2Base64
        );
    }

    /**
     * Consulta autenticada de un pre-registro express para precargar datos en ventanilla (TN1, TN3).
     */
    @Transactional
    public DetallePreRegistroVentanillaDto consultarPreRegistroVentanilla(String codigoExpress) {
        if (codigoExpress == null || codigoExpress.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código express es obligatorio.");
        }

        String codigoLimpio = codigoExpress.trim();
        PreRegistroExpress pre = repositorioPreRegistroPort.buscarPorCodigoExpress(codigoLimpio)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el pre-registro con código express: " + codigoLimpio));

        LocalDateTime ahora = LocalDateTime.now();
        if (pre.estaExpirado(ahora, vigenciaHoras)) {
            pre.marcarExpirado();
            repositorioPreRegistroPort.actualizar(pre);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pre-registro express con código " + codigoLimpio + " ha expirado.");
        }

        if (pre.getEstado() != EstadoPreRegistro.PENDIENTE_VENTANILLA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pre-registro express con código " + codigoLimpio + " ya fue procesado o no está pendiente.");
        }

        ClienteJpaEntity remitente = clienteRepository.findById(pre.getIdClienteRemitente())
                .orElseThrow(() -> new IllegalStateException("Cliente remitente no encontrado"));
        ClienteJpaEntity destinatario = clienteRepository.findById(pre.getIdClienteDestinatario())
                .orElseThrow(() -> new IllegalStateException("Cliente destinatario no encontrado"));

        ClienteInputDto remitenteDto = new ClienteInputDto(
                TipoDocumento.valueOf(remitente.getTipoDocumento()),
                remitente.getNumeroDocumento(),
                remitente.getNombresRazonSocial(),
                remitente.getTelefono(),
                remitente.getCorreo()
        );

        ClienteInputDto destinatarioDto = new ClienteInputDto(
                TipoDocumento.valueOf(destinatario.getTipoDocumento()),
                destinatario.getNumeroDocumento(),
                destinatario.getNombresRazonSocial(),
                destinatario.getTelefono(),
                destinatario.getCorreo()
        );

        return new DetallePreRegistroVentanillaDto(
                pre.getIdPreRegistro(),
                pre.getCodigoExpress(),
                remitenteDto,
                destinatarioDto,
                pre.getIdSucursalDestino(),
                pre.getPesoEstimado(),
                pre.getMontoCotizado(),
                pre.getFechaCreacion(),
                pre.calcularFechaExpiracion(vigenciaHoras),
                pre.getEstado().name()
        );
    }

    private ClienteJpaEntity obtenerOCrearCliente(ClienteInputDto dto) {
        return clienteRepository.findByNumeroDocumento(dto.numeroDocumento())
                .orElseGet(() -> {
                    ClienteJpaEntity nuevo = new ClienteJpaEntity(
                            UUID.randomUUID().toString(),
                            dto.tipoDocumento().name(),
                            dto.numeroDocumento(),
                            dto.nombresRazonSocial(),
                            dto.telefono(),
                            dto.correo()
                    );
                    return clienteRepository.save(nuevo);
                });
    }

    private String generarCodigoExpressUnico() {
        for (int i = 0; i < 10; i++) {
            int numero = secureRandom.nextInt(1_000_000);
            String codigo = String.format("PRE-%06d-PE", numero);
            if (!repositorioPreRegistroPort.existePorCodigoExpress(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException("No fue posible generar un código express único tras múltiples intentos.");
    }

    private String obtenerNombreSucursal(String idSucursal) {
        if (idSucursal == null) return "Agencia";
        return repositorioSucursalPort.buscarPorId(idSucursal)
                .map(Sucursal::getNombreAgencia)
                .orElse(idSucursal);
    }

    /**
     * Enmascara nombres personales o razones sociales preservando la inicial y ocultando el resto (e.g. "J*** P***").
     */
    public static String enmascararNombre(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "***";
        }
        String[] partes = nombreCompleto.trim().split("\\s+");
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            String parte = partes[i];
            if (parte.length() <= 2) {
                resultado.append(parte.charAt(0)).append("*");
            } else {
                resultado.append(parte.charAt(0)).append("***");
            }
            if (i < partes.length - 1) {
                resultado.append(" ");
            }
        }
        return resultado.toString();
    }
}
