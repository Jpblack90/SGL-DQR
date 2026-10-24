package pe.marvisur.sgldqr.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.marvisur.sgldqr.api.persistencia.entidad.ArchivoPdfComprobanteJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ComprobantePagoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ArchivoPdfComprobanteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ComprobantePagoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ParDobleQrSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.dominio.modelo.ComprobantePdfDatos;
import pe.marvisur.sgldqr.dominio.modelo.Sucursal;
import pe.marvisur.sgldqr.puertos.GeneradorComprobantePort;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;
import pe.marvisur.sgldqr.puertos.RepositorioSucursalPort;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de aplicación para la gestión, emisión perezosa, almacenamiento e idempotencia
 * de comprobantes de pago en formato PDF (RF-11).
 * <p>
 * <strong>Reglas de negocio:</strong>
 * <ul>
 *   <li>El comprobante es informativo de control interno del sistema SGL-DQR y NO es comprobante electrónico SUNAT.</li>
 *   <li>Emisión perezosa: el registro en {@code comprobantes_pago} y el PDF se crean la primera vez que se solicitan.</li>
 *   <li>Idempotencia: descargas sucesivas devuelven el mismo hash SHA-256 sin duplicar registros.</li>
 *   <li>Verificación de integridad: si el archivo en disco no existe o su hash SHA-256 no coincide, se regenera automáticamente.</li>
 *   <li>Privacidad: documentos de identidad enmascarados (últimos 3 dígitos visibles), sin correos ni teléfonos.</li>
 *   <li>Seguridad: solo incluye el QR1 físico; nunca el QR2 llave digital.</li>
 * </ul>
 * </p>
 */
@Service
public class ComprobanteServicio {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteServicio.class);
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final String ERROR_PUBLICO_NO_ENCONTRADO = "No se encontró el comprobante con los datos proporcionados";

    private final EncomiendaSpringDataRepository encomiendaRepository;
    private final ClienteSpringDataRepository clienteRepository;
    private final RepositorioSucursalPort sucursalPort;
    private final ParDobleQrSpringDataRepository parDobleQrRepository;
    private final ComprobantePagoSpringDataRepository comprobantePagoRepository;
    private final ArchivoPdfComprobanteSpringDataRepository archivoPdfRepository;
    private final GeneradorComprobantePort generadorComprobantePort;
    private final ImagenQRPort imagenQRPort;

    @Value("${app.comprobantes.directorio:./comprobantes}")
    private String directorioComprobantes;

    public record ResultadoPdfComprobante(String nombreArchivo, byte[] contenidoPdf) {}

    public ComprobanteServicio(EncomiendaSpringDataRepository encomiendaRepository,
                               ClienteSpringDataRepository clienteRepository,
                               RepositorioSucursalPort sucursalPort,
                               ParDobleQrSpringDataRepository parDobleQrRepository,
                               ComprobantePagoSpringDataRepository comprobantePagoRepository,
                               ArchivoPdfComprobanteSpringDataRepository archivoPdfRepository,
                               GeneradorComprobantePort generadorComprobantePort,
                               ImagenQRPort imagenQRPort) {
        this.encomiendaRepository = Objects.requireNonNull(encomiendaRepository, "El encomiendaRepository no puede ser nulo");
        this.clienteRepository = Objects.requireNonNull(clienteRepository, "El clienteRepository no puede ser nulo");
        this.sucursalPort = Objects.requireNonNull(sucursalPort, "El sucursalPort no puede ser nulo");
        this.parDobleQrRepository = Objects.requireNonNull(parDobleQrRepository, "El parDobleQrRepository no puede ser nulo");
        this.comprobantePagoRepository = Objects.requireNonNull(comprobantePagoRepository, "El comprobantePagoRepository no puede ser nulo");
        this.archivoPdfRepository = Objects.requireNonNull(archivoPdfRepository, "El archivoPdfRepository no puede ser nulo");
        this.generadorComprobantePort = Objects.requireNonNull(generadorComprobantePort, "El generadorComprobantePort no puede ser nulo");
        this.imagenQRPort = Objects.requireNonNull(imagenQRPort, "El imagenQRPort no puede ser nulo");
    }

    /**
     * Obtiene o genera perezosamente el PDF del comprobante para el acceso público (portal web).
     * Requiere que el comprobante y el número de documento de identidad (DNI o RUC) coincidan con el remitente o destinatario.
     * Ante cualquier discrepancia responde 404 genérico idéntico.
     */
    @Transactional
    public ResultadoPdfComprobante obtenerOgenerarPdfPublico(String comprobante, String documento) {
        if (comprobante == null || comprobante.isBlank() || documento == null || documento.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los parámetros comprobante y documento son obligatorios.");
        }

        String comprobanteLimpio = comprobante.trim();
        String docLimpio = documento.trim();

        // 1. Buscar encomienda por código de comprobante
        EncomiendaJpaEntity enc = encomiendaRepository.findByCodigoComprobante(comprobanteLimpio)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ERROR_PUBLICO_NO_ENCONTRADO));

        // 2. Verificar autorización por documento del remitente o destinatario
        ClienteJpaEntity remitente = clienteRepository.findById(enc.getIdRemitente()).orElse(null);
        ClienteJpaEntity destinatario = clienteRepository.findById(enc.getIdDestinatario()).orElse(null);

        boolean autorizado = (remitente != null && docLimpio.equalsIgnoreCase(remitente.getNumeroDocumento())) ||
                             (destinatario != null && docLimpio.equalsIgnoreCase(destinatario.getNumeroDocumento()));

        if (!autorizado) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ERROR_PUBLICO_NO_ENCONTRADO);
        }

        return obtenerOGenerarComprobante(enc, remitente, destinatario);
    }

    /**
     * Obtiene o genera perezosamente el PDF del comprobante para el personal interno (TN1, TN2, TN3).
     * Los choferes (TN4) tienen acceso denegado (403 Forbidden).
     * Los operadores (TN1, TN2) solo pueden acceder a encomiendas de su propia sucursal (origen o destino).
     * El Administrador (TN3) puede acceder a encomiendas de cualquier sucursal.
     */
    @Transactional
    public ResultadoPdfComprobante obtenerOgenerarPdfInterno(String idEncomienda, UsuarioAutenticado usuario) {
        if (idEncomienda == null || idEncomienda.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador de encomienda es obligatorio.");
        }
        Objects.requireNonNull(usuario, "El usuario autenticado no puede ser nulo");

        // 1. Validar restricción para Chofer (TN4) -> 403 Forbidden
        if (esChofer(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los choferes no tienen acceso a comprobantes de pago.");
        }

        // 2. Buscar encomienda
        EncomiendaJpaEntity enc = encomiendaRepository.findById(idEncomienda.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomienda no encontrada"));

        // 3. Validar alcance por sucursal para TN1 y TN2 (solo sucursal origen o destino)
        if (!esAdministrador(usuario)) {
            String sucursalUsuario = usuario.getIdSucursal();
            boolean perteneceSucursal = sucursalUsuario != null &&
                    (sucursalUsuario.equalsIgnoreCase(enc.getIdSucursalOrigen()) ||
                     sucursalUsuario.equalsIgnoreCase(enc.getIdSucursalDestino()));
            if (!perteneceSucursal) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para acceder a comprobantes de otra sucursal.");
            }
        }

        ClienteJpaEntity remitente = clienteRepository.findById(enc.getIdRemitente()).orElse(null);
        ClienteJpaEntity destinatario = clienteRepository.findById(enc.getIdDestinatario()).orElse(null);

        return obtenerOGenerarComprobante(enc, remitente, destinatario);
    }

    /**
     * Lógica central de obtención, idempotencia y regeneración del comprobante.
     */
    private ResultadoPdfComprobante obtenerOGenerarComprobante(EncomiendaJpaEntity enc,
                                                              ClienteJpaEntity remitente,
                                                              ClienteJpaEntity destinatario) {
        // 1. Obtener o crear ComprobantePagoJpaEntity
        ComprobantePagoJpaEntity comprobante = comprobantePagoRepository.findByIdEncomienda(enc.getIdEncomienda())
                .orElseGet(() -> crearComprobantePago(enc, remitente));

        String nombreArchivo = "comprobante-" + comprobante.getSerieCorrelativo() + ".pdf";
        Path rutaDirectorio = Paths.get(directorioComprobantes).toAbsolutePath().normalize();
        Path rutaArchivo = rutaDirectorio.resolve(nombreArchivo);

        // 2. Comprobar si ya existe archivo registrado e íntegro en disco
        Optional<ArchivoPdfComprobanteJpaEntity> archivoOpt = archivoPdfRepository.findByIdComprobante(comprobante.getIdComprobante());
        if (archivoOpt.isPresent()) {
            ArchivoPdfComprobanteJpaEntity archivoEntity = archivoOpt.get();
            Path pathExistente = Paths.get(archivoEntity.getRutaAlmacenamientoVps());
            if (Files.isRegularFile(pathExistente)) {
                try {
                    byte[] bytesExistentes = Files.readAllBytes(pathExistente);
                    String hashActual = calcularSha256(bytesExistentes);
                    if (hashActual.equalsIgnoreCase(archivoEntity.getHashSha256Archivo())) {
                        log.debug("Comprobante {} recuperado de disco con hash íntegro", comprobante.getSerieCorrelativo());
                        return new ResultadoPdfComprobante(nombreArchivo, bytesExistentes);
                    }
                    log.warn("Comprobante {} en disco tiene hash discrepante; se regenerará", comprobante.getSerieCorrelativo());
                } catch (IOException e) {
                    log.warn("Error leyendo archivo de comprobante en disco: {}; se regenerará", e.getMessage());
                }
            } else {
                log.info("Archivo de comprobante {} no encontrado en disco; se regenerará", pathExistente);
            }
        }

        // 3. Generar el PDF físicamente
        byte[] pdfBytes = generarBytesPdf(enc, comprobante, remitente, destinatario);
        String hashSha256 = calcularSha256(pdfBytes);

        // 4. Guardar archivo en disco
        try {
            Files.createDirectories(rutaDirectorio);
            Files.write(rutaArchivo, pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Error al persistir comprobante PDF en disco: {}", rutaArchivo, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo almacenar el archivo de comprobante en disco.");
        }

        // 5. Guardar o actualizar registro en 'archivos_pdf_comprobante'
        OffsetDateTime ahora = OffsetDateTime.now();
        if (archivoOpt.isPresent()) {
            ArchivoPdfComprobanteJpaEntity entity = archivoOpt.get();
            entity.setNombreArchivo(nombreArchivo);
            entity.setRutaAlmacenamientoVps(rutaArchivo.toString());
            entity.setHashSha256Archivo(hashSha256);
            entity.setFechaGeneracion(ahora);
            archivoPdfRepository.save(entity);
        } else {
            ArchivoPdfComprobanteJpaEntity nuevo = new ArchivoPdfComprobanteJpaEntity(
                    UUID.randomUUID().toString(),
                    comprobante.getIdComprobante(),
                    nombreArchivo,
                    rutaArchivo.toString(),
                    hashSha256,
                    ahora
            );
            archivoPdfRepository.save(nuevo);
        }

        return new ResultadoPdfComprobante(nombreArchivo, pdfBytes);
    }

    private ComprobantePagoJpaEntity crearComprobantePago(EncomiendaJpaEntity enc, ClienteJpaEntity remitente) {
        BigDecimal montoTotal = enc.getMontoTotal();
        BigDecimal subtotal = montoTotal.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        BigDecimal igv = montoTotal.subtract(subtotal);

        // Determinación del tipo de comprobante: FACTURA si es RUC (11 dígitos), BOLETA si es DNI u otro
        String tipoComprobante = "BOLETA";
        if (remitente != null) {
            String tipoDoc = remitente.getTipoDocumento();
            String numDoc = remitente.getNumeroDocumento();
            if ("RUC".equalsIgnoreCase(tipoDoc) || (numDoc != null && numDoc.trim().length() == 11)) {
                tipoComprobante = "FACTURA";
            }
        }

        OffsetDateTime fechaEmision = enc.getFechaRegistro() != null
                ? enc.getFechaRegistro()
                : OffsetDateTime.now();

        ComprobantePagoJpaEntity comprobante = new ComprobantePagoJpaEntity(
                UUID.randomUUID().toString(),
                enc.getIdEncomienda(),
                tipoComprobante,
                enc.getCodigoComprobante(),
                subtotal,
                igv,
                montoTotal,
                fechaEmision
        );
        return comprobantePagoRepository.save(comprobante);
    }

    private byte[] generarBytesPdf(EncomiendaJpaEntity enc, ComprobantePagoJpaEntity comprobante,
                                   ClienteJpaEntity remitente, ClienteJpaEntity destinatario) {
        // Nombres de agencias
        Sucursal origen = sucursalPort.buscarPorId(enc.getIdSucursalOrigen()).orElse(null);
        Sucursal destino = sucursalPort.buscarPorId(enc.getIdSucursalDestino()).orElse(null);
        String agenciaOrigen = origen != null ? origen.getNombreAgencia() : "Agencia Origen";
        String agenciaDestino = destino != null ? destino.getNombreAgencia() : "Agencia Destino";

        // Nombres completos
        String nombreRemitente = remitente != null ? remitente.getNombresRazonSocial() : "CLIENTE REMITENTE";
        String nombreDestinatario = destinatario != null ? destinatario.getNombresRazonSocial() : "CLIENTE DESTINATARIO";

        // Documentos enmascarados (solo últimos 3 dígitos visibles)
        String docRemitente = enmascararDocumento(remitente != null ? remitente.getNumeroDocumento() : "");
        String docDestinatario = enmascararDocumento(destinatario != null ? destinatario.getNumeroDocumento() : "");

        // Obtener QR1 operativo en PNG
        String qr1Payload = "MARVISUR:QR1:" + enc.getIdEncomienda();
        Optional<ParDobleQrJpaEntity> parOpt = parDobleQrRepository.findByIdEncomienda(enc.getIdEncomienda());
        if (parOpt.isPresent()) {
            qr1Payload = parOpt.get().getUuidQr1Operativo();
        }
        byte[] qr1PngBytes = imagenQRPort.generarPNG(qr1Payload, 150);

        LocalDateTime fechaEmisionLocal = comprobante.getFechaEmision()
                .atZoneSameInstant(ZONA_LIMA)
                .toLocalDateTime();

        // Modalidad y estado de pago
        String estadoPago = enc.getEstadoPago() != null ? enc.getEstadoPago() : "PAGADO";
        String modalidad = "PENDIENTE".equalsIgnoreCase(estadoPago) ? "CONTRA_ENTREGA" : "CONTADO";

        ComprobantePdfDatos datos = new ComprobantePdfDatos(
                comprobante.getTipoComprobante(),
                comprobante.getSerieCorrelativo(),
                fechaEmisionLocal,
                agenciaOrigen,
                agenciaDestino,
                nombreRemitente,
                docRemitente,
                nombreDestinatario,
                docDestinatario,
                enc.getPesoKg(),
                enc.getDescripcionContenido(),
                modalidad,
                estadoPago,
                comprobante.getMontoSubtotal(),
                comprobante.getMontoIgv(),
                comprobante.getMontoTotal(),
                qr1PngBytes
        );

        return generadorComprobantePort.generarComprobantePdf(datos);
    }

    public static String enmascararDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            return "***";
        }
        String doc = documento.trim();
        int len = doc.length();
        if (len <= 3) {
            return "***";
        }
        return "*".repeat(len - 3) + doc.substring(len - 3);
    }

    private String calcularSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible", e);
        }
    }

    private boolean esAdministrador(UsuarioAutenticado usuario) {
        String rol = usuario.getRol();
        return ConstantesSeguridad.AUTH_ADMINISTRADOR.equalsIgnoreCase(rol) ||
               ConstantesSeguridad.ROL_ADMINISTRADOR.equalsIgnoreCase(rol) ||
               "ADMINISTRADOR".equalsIgnoreCase(rol);
    }

    private boolean esChofer(UsuarioAutenticado usuario) {
        String rol = usuario.getRol();
        return ConstantesSeguridad.AUTH_CHOFER.equalsIgnoreCase(rol) ||
               ConstantesSeguridad.ROL_CHOFER.equalsIgnoreCase(rol) ||
               "CHOFER".equalsIgnoreCase(rol);
    }
}
