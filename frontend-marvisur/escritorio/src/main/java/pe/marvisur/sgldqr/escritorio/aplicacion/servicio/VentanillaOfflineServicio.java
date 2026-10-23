package pe.marvisur.sgldqr.escritorio.aplicacion.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import pe.marvisur.sgldqr.aplicacion.TarifarioPorPeso;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.servicio.MotorQR;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ApiExcepcion;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CotizacionComando;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CotizacionResultado;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EncomiendaRegistradaResultado;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PreRegistroDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.Qr1Data;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RegistrarEncomiendaComando;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.SucursalDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.UsuarioSesion;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.ColaLocalPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.VentanillaPort;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;
import pe.marvisur.sgldqr.escritorio.infraestructura.sqlite.SQLiteColaAdapter;
import pe.marvisur.sgldqr.infraestructura.qr.ZXingAdapter;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio orquestador de ventanilla con soporte transparente para modo sin conexión (offline-first, RNF-02, O1).
 *
 * <p><strong>Lógica de Conmutación:</strong>
 * <ul>
 *   <li>Si hay conexión activa, las operaciones se delegan a la API central {@link MarvisurHttpClient}.</li>
 *   <li>Si no hay conexión o si la llamada HTTP falla por error de red/timeout, el registro NO se pierde
 *       ni se solicita de nuevo: se genera localmente el código de comprobante, la tarifa oficial y el
 *       par criptográfico con {@link MotorQR}, encolando los hashes en SQLite mediante {@link ColaLocalPort}.</li>
 *   <li>La caché local SQLite {@code cache_sucursales} abastece el combo de agencias ante desconexión.</li>
 * </ul>
 * </p>
 */
public class VentanillaOfflineServicio implements VentanillaPort {

    private static final Logger LOGGER = Logger.getLogger(VentanillaOfflineServicio.class.getName());

    private final MarvisurHttpClient httpClient;
    private final ColaLocalPort colaLocalPort;
    private final ServicioConexion servicioConexion;
    private final MotorQR motorQR;
    private final ImagenQRPort imagenQRPort;
    private final TarifarioPorPeso tarifarioLocal;
    private final GeneradorCodigoComprobanteLocal generadorCodigo;
    private final ObjectMapper objectMapper;
    private final String portalUrlBase;

    public VentanillaOfflineServicio() {
        this(
                new MarvisurHttpClient(),
                new SQLiteColaAdapter(),
                ServicioConexion.getInstancia(),
                new MotorQR(),
                new ZXingAdapter(),
                obtenerTarifarioConfigurado(),
                new GeneradorCodigoComprobanteLocal(),
                obtenerPortalUrlConfigurado()
        );
    }

    public VentanillaOfflineServicio(MarvisurHttpClient httpClient,
                                     ColaLocalPort colaLocalPort,
                                     ServicioConexion servicioConexion,
                                     MotorQR motorQR,
                                     ImagenQRPort imagenQRPort,
                                     TarifarioPorPeso tarifarioLocal,
                                     GeneradorCodigoComprobanteLocal generadorCodigo,
                                     String portalUrlBase) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient no puede ser nulo");
        this.colaLocalPort = Objects.requireNonNull(colaLocalPort, "colaLocalPort no puede ser nulo");
        this.servicioConexion = Objects.requireNonNull(servicioConexion, "servicioConexion no puede ser nulo");
        this.motorQR = Objects.requireNonNull(motorQR, "motorQR no puede ser nulo");
        this.imagenQRPort = Objects.requireNonNull(imagenQRPort, "imagenQRPort no puede ser nulo");
        this.tarifarioLocal = Objects.requireNonNull(tarifarioLocal, "tarifarioLocal no puede ser nulo");
        this.generadorCodigo = Objects.requireNonNull(generadorCodigo, "generadorCodigo no puede ser nulo");
        this.portalUrlBase = portalUrlBase != null ? portalUrlBase : "http://localhost:5173";
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private static TarifarioPorPeso obtenerTarifarioConfigurado() {
        try (InputStream is = VentanillaOfflineServicio.class.getResourceAsStream("/escritorio.properties")) {
            if (is != null) {
                Properties prop = new Properties();
                prop.load(is);
                String baseStr = prop.getProperty("tarifa.base");
                String kgStr = prop.getProperty("tarifa.costo-por-kg");
                if (baseStr != null && kgStr != null) {
                    return new TarifarioPorPeso(new BigDecimal(baseStr.trim()), new BigDecimal(kgStr.trim()));
                }
            }
        } catch (Exception ignored) {
        }
        return TarifarioPorPeso.conValoresPorDefecto();
    }

    private static String obtenerPortalUrlConfigurado() {
        try (InputStream is = VentanillaOfflineServicio.class.getResourceAsStream("/escritorio.properties")) {
            if (is != null) {
                Properties prop = new Properties();
                prop.load(is);
                String url = prop.getProperty("portal.url-base");
                if (url != null && !url.isBlank()) {
                    return url.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "http://localhost:5173";
    }

    @Override
    public List<SucursalDto> listarSucursales() {
        if (servicioConexion.estaEnLinea()) {
            try {
                List<SucursalDto> remotas = httpClient.listarSucursales();
                if (!remotas.isEmpty()) {
                    colaLocalPort.guardarCacheSucursales(remotas);
                }
                return remotas;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al listar sucursales online, recurriendo a caché local SQLite: " + e.getMessage());
                servicioConexion.reportarFalloConexion("Fallo al listar sucursales: " + e.getMessage());
            }
        }

        List<SucursalDto> locales = colaLocalPort.obtenerCacheSucursales();
        if (!locales.isEmpty()) {
            return locales;
        }

        // Catálogo de respaldo mínimo si la base SQLite estuviese vacía
        return List.of(
                new SucursalDto("SUC-LIM-01", "Agencia Lima Central", "Lima", "Av. Grau 450"),
                new SucursalDto("SUC-HYO-01", "Agencia Huancayo", "Huancayo", "Av. Ferrocarril 123"),
                new SucursalDto("SUC-AQP-01", "Agencia Arequipa", "Arequipa", "Av. Ejército 100")
        );
    }

    @Override
    public CotizacionResultado cotizar(CotizacionComando comando) {
        if (servicioConexion.estaEnLinea()) {
            try {
                return httpClient.cotizar(comando);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al cotizar online, recurriendo a tarifario local: " + e.getMessage());
                servicioConexion.reportarFalloConexion("Fallo al cotizar: " + e.getMessage());
            }
        }

        if (comando.pesoKg() == null || comando.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            return new CotizacionResultado(BigDecimal.ZERO, "PEN");
        }

        BigDecimal tarifa = tarifarioLocal.calcularTarifa(comando.pesoKg(), comando.volumenM3());
        return new CotizacionResultado(tarifa, "PEN");
    }

    @Override
    public PreRegistroDto consultarPreRegistro(String codigoExpress) {
        if (!servicioConexion.estaEnLinea()) {
            throw new ApiExcepcion(503, "REQUIERE_CONEXION", "La precarga de Código Express requiere conexión activa con el servidor central.", null);
        }

        try {
            return httpClient.consultarPreRegistro(codigoExpress);
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            servicioConexion.reportarFalloConexion("Fallo al consultar pre-registro: " + e.getMessage());
            throw new ApiExcepcion("Error de comunicación al consultar Código Express: " + e.getMessage(), e);
        }
    }

    @Override
    public EncomiendaRegistradaResultado registrarEncomienda(RegistrarEncomiendaComando comando) {
        Objects.requireNonNull(comando, "El comando de registro no puede ser nulo");

        if (servicioConexion.estaEnLinea()) {
            try {
                return httpClient.registrarEncomienda(comando);
            } catch (ApiExcepcion ae) {
                // Errores de validación 400 o credenciales 401/403 no deben encolarse como offline
                if (ae.getStatusCode() == 400 || ae.getStatusCode() == 401 || ae.getStatusCode() == 403) {
                    throw ae;
                }
                LOGGER.log(Level.WARNING, "Fallo de red en registro online (" + ae.getMessage() + "), conmutando a registro offline...");
                servicioConexion.reportarFalloConexion("Fallo en registro HTTP: " + ae.getMessage());
                return registrarLocalmente(comando);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Fallo inesperado de comunicación, conmutando a registro offline...");
                servicioConexion.reportarFalloConexion("Error de red: " + e.getMessage());
                return registrarLocalmente(comando);
            }
        }

        LOGGER.info("Registrando encomienda directamente en modo sin conexión...");
        return registrarLocalmente(comando);
    }

    private EncomiendaRegistradaResultado registrarLocalmente(RegistrarEncomiendaComando comando) {
        String idEncomienda = UUID.randomUUID().toString();
        String codigoComprobante = generadorCodigo.generarCodigo();

        UsuarioSesion sesion = SesionContexto.obtenerSesion();
        String idSucursalOrigen = (sesion != null && sesion.idSucursal() != null && !sesion.idSucursal().isBlank())
                ? sesion.idSucursal()
                : "SUC-LIM-01";

        BigDecimal montoTotal = tarifarioLocal.calcularTarifa(comando.pesoKg(), comando.volumenM3());
        String modalidad = comando.modalidadPago() != null ? comando.modalidadPago() : "CONTADO";
        String estadoPago = "CONTADO".equalsIgnoreCase(modalidad) ? "PAGADO" : "PENDIENTE";
        EstadoPago estadoPagoDominio = "CONTADO".equalsIgnoreCase(modalidad) ? EstadoPago.PAGADO : EstadoPago.PENDIENTE;

        LocalDateTime ahora = LocalDateTime.now();

        // 1. Entidad de dominio pura para alimentar el Motor Criptográfico del Doble QR
        Encomienda encomiendaDominio = new Encomienda(
                idEncomienda,
                codigoComprobante,
                comando.remitente().numeroDocumento(),
                comando.destinatario().numeroDocumento(),
                idSucursalOrigen,
                comando.idSucursalDestino(),
                comando.pesoKg(),
                comando.volumenM3(),
                comando.descripcionContenido(),
                montoTotal,
                estadoPagoDominio,
                EstadoLogistico.REGISTRADO,
                ahora
        );

        ParDobleQR par = motorQR.generarDobleQR(encomiendaDominio);
        String idPareja = par.getIdParejaQr();
        String uuidQr1 = par.getUuidQr1Operativo();
        String hashQr2 = par.getHashQr2LlaveDigital();
        String qr2TokenEnClaro = par.getQr2TokenEnClaro();

        // 2. Construir payload JSON compatible con POST /api/v1/sync/encomiendas (B4)
        // NOTA DE SEGURIDAD CRÍTICA: qr2TokenEnClaro jamás se incluye en el payload ni se persiste en SQLite.
        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("idEncomienda", idEncomienda);
        payloadMap.put("codigoComprobante", codigoComprobante);
        payloadMap.put("remitente", Map.of(
                "tipoDocumento", comando.remitente().tipoDocumento() != null ? comando.remitente().tipoDocumento() : "DNI",
                "numeroDocumento", comando.remitente().numeroDocumento() != null ? comando.remitente().numeroDocumento().trim() : "",
                "nombresRazonSocial", comando.remitente().nombresRazonSocial() != null ? comando.remitente().nombresRazonSocial().trim() : "",
                "telefono", comando.remitente().telefono() != null ? comando.remitente().telefono().trim() : "",
                "correo", comando.remitente().correo() != null ? comando.remitente().correo().trim() : ""
        ));
        payloadMap.put("destinatario", Map.of(
                "tipoDocumento", comando.destinatario().tipoDocumento() != null ? comando.destinatario().tipoDocumento() : "RUC",
                "numeroDocumento", comando.destinatario().numeroDocumento() != null ? comando.destinatario().numeroDocumento().trim() : "",
                "nombresRazonSocial", comando.destinatario().nombresRazonSocial() != null ? comando.destinatario().nombresRazonSocial().trim() : "",
                "telefono", comando.destinatario().telefono() != null ? comando.destinatario().telefono().trim() : "",
                "correo", comando.destinatario().correo() != null ? comando.destinatario().correo().trim() : ""
        ));
        payloadMap.put("idSucursalOrigen", idSucursalOrigen);
        payloadMap.put("idSucursalDestino", comando.idSucursalDestino());
        payloadMap.put("pesoKg", comando.pesoKg());
        payloadMap.put("volumenM3", comando.volumenM3());
        payloadMap.put("descripcionContenido", comando.descripcionContenido());
        payloadMap.put("modalidadPago", modalidad);
        payloadMap.put("montoTotal", montoTotal);
        payloadMap.put("fechaRegistro", ahora.toString());
        payloadMap.put("qr", Map.of(
                "idPareja", idPareja,
                "uuidQr1", uuidQr1,
                "hashQr2", hashQr2
        ));

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al serializar payload de encomienda para cola SQLite: " + e.getMessage(), e);
            throw new RuntimeException("Error al serializar registro offline: " + e.getMessage(), e);
        }

        // 3. Persistir en la cola SQLite
        colaLocalPort.guardarEnCola(idEncomienda, jsonPayload);

        // 4. Renderizado visual del QR1 físico para impresión y previsualización
        String qr1Base64 = null;
        try {
            byte[] png = imagenQRPort.generarPNG(uuidQr1, 300);
            qr1Base64 = Base64.getEncoder().encodeToString(png);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al generar imagen PNG de QR1: " + e.getMessage());
        }

        // 5. Enlace a la Billetera Digital con QR2 en el fragmento (#)
        String enlaceBilletera = portalUrlBase + "/billetera#" + qr2TokenEnClaro;

        return new EncomiendaRegistradaResultado(
                idEncomienda,
                codigoComprobante,
                "LOCAL-REM-" + comando.remitente().numeroDocumento(),
                "LOCAL-DEST-" + comando.destinatario().numeroDocumento(),
                idSucursalOrigen,
                comando.idSucursalDestino(),
                comando.pesoKg(),
                comando.volumenM3(),
                comando.descripcionContenido(),
                montoTotal,
                estadoPago,
                "REGISTRADO",
                ahora.toString(),
                new Qr1Data(uuidQr1, qr1Base64),
                qr2TokenEnClaro,
                enlaceBilletera,
                "REGISTRADO_OFFLINE"
        );
    }
}
