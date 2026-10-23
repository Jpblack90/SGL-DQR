package pe.marvisur.sgldqr.escritorio.infraestructura.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.*;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.AlmacenPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.AutenticacionPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.EstadoConexionPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.VentanillaPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.SyncPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente HTTP REST para comunicación con la API central de Expreso Marvisur.
 * Implementa los puertos {@link AutenticacionPort}, {@link VentanillaPort},
 * {@link AlmacenPort} y {@link EstadoConexionPort}.
 */
public class MarvisurHttpClient implements AutenticacionPort, VentanillaPort, AlmacenPort, EstadoConexionPort, SyncPort {

    private static final Logger LOGGER = Logger.getLogger(MarvisurHttpClient.class.getName());
    private static final Duration TIMEOUT_DEFAULT = Duration.ofSeconds(2);
    private static final Duration TIMEOUT_SALUD = Duration.ofSeconds(2);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MarvisurHttpClient() {
        this(new ConfiguracionApi().getBaseUrl(), HttpClient.newBuilder()
                .connectTimeout(TIMEOUT_DEFAULT)
                .build());
    }

    public MarvisurHttpClient(String baseUrl) {
        this(baseUrl, HttpClient.newBuilder()
                .connectTimeout(TIMEOUT_DEFAULT)
                .build());
    }

    public MarvisurHttpClient(String baseUrl, HttpClient httpClient) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // =========================================================================
    // AUTENTICACIÓN
    // =========================================================================

    @Override
    public UsuarioSesion autenticar(String correo, String password) {
        if (correo == null || correo.isBlank() || password == null || password.isBlank()) {
            throw new ApiExcepcion(400, "VALIDACION_FALLIDA", "Debe ingresar correo y contraseña.", null);
        }

        Map<String, String> credenciales = Map.of("correo", correo.trim(), "password", password);
        try {
            String jsonPeticion = objectMapper.writeValueAsString(credenciales);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/auth/login"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPeticion))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String token = root.path("token").asText(null);
                JsonNode empleadoNode = root.path("empleado");

                String id = empleadoNode.path("id").asText(null);
                String nombres = empleadoNode.path("nombres").asText("");
                String apellidos = empleadoNode.path("apellidos").asText("");
                String mail = empleadoNode.path("correo").asText(correo);
                String rol = empleadoNode.path("rol").asText("");
                String idSucursal = empleadoNode.path("idSucursal").asText("");

                UsuarioSesion sesion = new UsuarioSesion(id, nombres, apellidos, mail, rol, idSucursal, token);
                SesionContexto.establecerSesion(sesion);
                return sesion;
            } else if (response.statusCode() == 401) {
                throw new ApiExcepcion(401, "CREDENCIALES_INVALIDAS", "Credenciales inválidas o usuario deshabilitado.", null);
            } else {
                procesarErrorRespuesta(response);
                throw new ApiExcepcion(response.statusCode(), "Error al autenticar: HTTP " + response.statusCode());
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.log(Level.SEVERE, "Error de red al intentar autenticar: " + e.getMessage());
            throw new ApiExcepcion("No se pudo conectar con el servidor central de Marvisur (" + baseUrl + ").", e);
        }
    }

    @Override
    public void cerrarSesion() {
        SesionContexto.limpiar();
    }

    @Override
    public UsuarioSesion obtenerSesionActual() {
        return SesionContexto.obtenerSesion();
    }

    // =========================================================================
    // VENTANILLA: SUCURSALES, COTIZACIÓN, PRE-REGISTRO, REGISTRO
    // =========================================================================

    @Override
    public List<SucursalDto> listarSucursales() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/publico/sucursales"))
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<SucursalDto>>() {});
            } else {
                procesarErrorRespuesta(response);
                return List.of();
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al listar agencias: " + e.getMessage());
            throw new ApiExcepcion("Error al obtener la lista de sucursales: " + e.getMessage(), e);
        }
    }

    @Override
    public CotizacionResultado cotizar(CotizacionComando comando) {
        try {
            Map<String, Object> cuerpo = new HashMap<>();
            cuerpo.put("idSucursalOrigen", comando.idSucursalOrigen());
            cuerpo.put("idSucursalDestino", comando.idSucursalDestino());
            cuerpo.put("pesoKg", comando.pesoKg());
            if (comando.volumenM3() != null) {
                cuerpo.put("volumenM3", comando.volumenM3());
            }

            String jsonPeticion = objectMapper.writeValueAsString(cuerpo);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/publico/cotizacion"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPeticion))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                BigDecimal monto = new BigDecimal(root.path("montoEstimado").asText("0.00"));
                String moneda = root.path("moneda").asText("PEN");
                return new CotizacionResultado(monto, moneda);
            } else {
                procesarErrorRespuesta(response);
                throw new ApiExcepcion(response.statusCode(), "Error al cotizar tarifa.");
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al calcular cotización: " + e.getMessage());
            throw new ApiExcepcion("Error al calcular la cotización: " + e.getMessage(), e);
        }
    }

    @Override
    public PreRegistroDto consultarPreRegistro(String codigoExpress) {
        if (codigoExpress == null || codigoExpress.isBlank()) {
            throw new ApiExcepcion(400, "CODIGO_INVALIDO", "Debe ingresar el código express.", null);
        }

        try {
            String codigoLimpio = codigoExpress.trim();
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/preregistros/" + codigoLimpio))
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .GET();

            adjuntarAutorizacionSiExiste(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String idPre = root.path("idPreRegistro").asText(null);
                String cod = root.path("codigoExpress").asText(codigoLimpio);

                JsonNode remNode = root.path("remitente");
                ClienteDto rem = remNode.isMissingNode() ? null : objectMapper.treeToValue(remNode, ClienteDto.class);

                JsonNode desNode = root.path("destinatario");
                ClienteDto des = desNode.isMissingNode() ? null : objectMapper.treeToValue(desNode, ClienteDto.class);

                String idSucDestino = root.path("idSucursalDestino").asText(null);
                BigDecimal peso = root.hasNonNull("pesoEstimado") ? new BigDecimal(root.path("pesoEstimado").asText()) : null;
                BigDecimal monto = root.hasNonNull("montoCotizado") ? new BigDecimal(root.path("montoCotizado").asText()) : null;
                String fecha = root.path("fechaCreacion").asText(null);

                return new PreRegistroDto(idPre, cod, rem, des, idSucDestino, peso, monto, fecha);
            } else if (response.statusCode() == 404) {
                throw new ApiExcepcion(404, "PRE_REGISTRO_NO_ENCONTRADO", "No se encontró ningún pre-registro activo con el código " + codigoLimpio, null);
            } else {
                procesarErrorRespuesta(response);
                throw new ApiExcepcion(response.statusCode(), "Error al consultar pre-registro.");
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al consultar pre-registro " + codigoExpress + ": " + e.getMessage());
            throw new ApiExcepcion("Error al consultar el código express: " + e.getMessage(), e);
        }
    }

    @Override
    public EncomiendaRegistradaResultado registrarEncomienda(RegistrarEncomiendaComando comando) {
        if (!SesionContexto.estaAutenticado()) {
            throw new ApiExcepcion(401, "NO_AUTENTICADO", "Debe iniciar sesión para registrar encomiendas.", null);
        }

        try {
            String jsonPeticion = objectMapper.writeValueAsString(comando);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/encomiendas"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPeticion));

            adjuntarAutorizacionSiExiste(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());

                String idEnc = root.path("idEncomienda").asText(null);
                String codComprobante = root.path("codigoComprobante").asText(null);
                String idRem = root.path("idRemitente").asText(null);
                String idDes = root.path("idDestinatario").asText(null);
                String idSucOri = root.path("idSucursalOrigen").asText(null);
                String idSucDes = root.path("idSucursalDestino").asText(null);
                BigDecimal peso = root.hasNonNull("pesoKg") ? new BigDecimal(root.path("pesoKg").asText()) : null;
                BigDecimal vol = root.hasNonNull("volumenM3") ? new BigDecimal(root.path("volumenM3").asText()) : null;
                String desc = root.path("descripcionContenido").asText(null);
                BigDecimal total = root.hasNonNull("montoTotal") ? new BigDecimal(root.path("montoTotal").asText()) : BigDecimal.ZERO;
                String estPago = root.path("estadoPago").asText(null);
                String estLog = root.path("estadoLogistico").asText(null);
                String fecha = root.path("fechaRegistro").asText(null);

                Qr1Data qr1 = null;
                if (root.hasNonNull("qr1")) {
                    JsonNode qr1Node = root.path("qr1");
                    qr1 = new Qr1Data(
                            qr1Node.path("payload").asText(null),
                            qr1Node.path("imagenPngBase64").asText(null)
                    );
                }

                String qr2Token = root.path("qr2Token").asText(null);
                String enlaceBilletera = root.path("enlaceBilletera").asText(null);
                String adv = root.path("advertencia").asText(null);

                return new EncomiendaRegistradaResultado(
                        idEnc, codComprobante, idRem, idDes, idSucOri, idSucDes,
                        peso, vol, desc, total, estPago, estLog, fecha,
                        qr1, qr2Token, enlaceBilletera, adv
                );
            } else {
                procesarErrorRespuesta(response);
                throw new ApiExcepcion(response.statusCode(), "Error al registrar encomienda.");
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al registrar encomienda: " + e.getMessage());
            throw new ApiExcepcion("Error al registrar la encomienda en el servidor: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // ALMACÉN: VALIDACIÓN CRUZADA, COBRO, VIAJES, LOTES, BÚSQUEDA, INCIDENCIAS
    // =========================================================================

    @Override
    public ValidacionEntregaResultado validarEntrega(String qr1, String qr2) {
        if (!SesionContexto.estaAutenticado()) {
            throw new ApiExcepcion(401, "NO_AUTENTICADO", "Debe iniciar sesión para validar entregas.", null);
        }

        try {
            Map<String, String> cuerpo = Map.of("qr1", qr1.trim(), "qr2", qr2.trim());
            String jsonPeticion = objectMapper.writeValueAsString(cuerpo);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/entregas/validar"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPeticion));

            adjuntarAutorizacionSiExiste(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String idEnc = root.path("idEncomienda").asText(null);
                String codComprobante = root.path("codigoComprobante").asText(null);
                String estadoLogistico = root.path("estadoLogistico").asText("ENTREGADO");
                long latencia = root.path("latenciaMs").asLong(0);
                String mensaje = root.path("mensaje").asText("Entrega física autorizada.");

                return new ValidacionEntregaResultado(
                        ValidacionEntregaResultado.TipoValidacion.EXITOSO,
                        idEnc, codComprobante, null, estadoLogistico, latencia, mensaje
                );
            } else if (response.statusCode() == 409) {
                JsonNode root = objectMapper.readTree(response.body());
                String codigo = root.path("codigo").asText("");
                if (codigo.isBlank()) {
                    codigo = root.path("title").asText("");
                }
                String codigoUpper = codigo.toUpperCase();
                long latencia = root.path("latenciaMs").asLong(0);

                if (codigoUpper.contains("PAGO_PENDIENTE") || codigoUpper.contains("BLOQUEADA")) {
                    String idEnc = root.path("idEncomienda").asText(null);
                    String codComprobante = root.path("codigoComprobante").asText(null);
                    BigDecimal monto = root.hasNonNull("montoTotal")
                            ? new BigDecimal(root.path("montoTotal").asText())
                            : BigDecimal.ZERO;
                    String mensaje = root.path("detail").asText("Entrega bloqueada: modalidad Contra Entrega con pago pendiente.");

                    return new ValidacionEntregaResultado(
                            ValidacionEntregaResultado.TipoValidacion.PAGO_PENDIENTE,
                            idEnc, codComprobante, monto, "EN_ALMACEN_DESTINO", latencia, mensaje
                    );
                } else if (codigoUpper.contains("TOKEN") || codigoUpper.contains("INVALIDO_O_EXPIRADO") || "TOKEN_QR2_INVALIDO".equals(codigoUpper)) {
                    return new ValidacionEntregaResultado(
                            ValidacionEntregaResultado.TipoValidacion.TOKEN_INVALIDO,
                            null, null, null, null, latencia,
                            root.path("detail").asText("El código digital presentado es inválido, ya fue utilizado o no corresponde a esta encomienda.")
                    );
                } else if (codigoUpper.contains("TRANSICION")) {
                    String estadoActual = root.path("estadoActual").asText("");
                    String mensaje = root.path("detail").asText("La encomienda no se encuentra en estado 'EN_ALMACEN_DESTINO'.");
                    return new ValidacionEntregaResultado(
                            ValidacionEntregaResultado.TipoValidacion.TRANSICION_INVALIDA,
                            null, null, null, estadoActual, latencia, mensaje
                    );
                } else {
                    return new ValidacionEntregaResultado(
                            ValidacionEntregaResultado.TipoValidacion.ERROR_INESPERADO,
                            null, null, null, null, latencia,
                            root.path("detail").asText("Error de validación cruzada.")
                    );
                }
            } else if (response.statusCode() == 404) {
                return new ValidacionEntregaResultado(
                        ValidacionEntregaResultado.TipoValidacion.NO_ENCONTRADO,
                        null, null, null, null, 0,
                        "No se encontró encomienda asociada al código QR1 escaneado."
                );
            } else if (response.statusCode() == 403) {
                throw new ApiExcepcion(403, "ACCESO_DENEGADO", "No tienes permiso para esta acción (requiere rol Almacenero o Administrador).", null);
            } else {
                procesarErrorRespuesta(response);
                throw new ApiExcepcion(response.statusCode(), "Error al validar entrega: HTTP " + response.statusCode());
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error de red al validar entrega: " + e.getMessage());
            throw new ApiExcepcion("Error de comunicación al validar entrega: " + e.getMessage(), e);
        }
    }

    @Override
    public RespuestaCobroDto registrarCobro(String idEncomienda) {
        if (!SesionContexto.estaAutenticado()) {
            throw new ApiExcepcion(401, "NO_AUTENTICADO", "Debe iniciar sesión para registrar cobros.", null);
        }

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/encomiendas/" + idEncomienda + "/cobro"))
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .POST(HttpRequest.BodyPublishers.noBody());

            adjuntarAutorizacionSiExiste(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return new RespuestaCobroDto(
                        root.path("idEncomienda").asText(idEncomienda),
                        root.path("codigoComprobante").asText(null),
                        root.hasNonNull("montoTotal") ? new BigDecimal(root.path("montoTotal").asText()) : BigDecimal.ZERO,
                        root.path("estadoPago").asText("PAGADO"),
                        root.path("estadoLogistico").asText(null),
                        root.path("mensaje").asText("Cobro registrado exitosamente."),
                        root.path("fechaCobro").asText(null)
                );
            } else if (response.statusCode() == 409) {
                throw new ApiExcepcion(409, "YA_PAGADA", "La encomienda ya se encuentra pagada.", null);
            } else if (response.statusCode() == 403) {
                throw new ApiExcepcion(403, "ACCESO_DENEGADO", "No tienes permiso para registrar cobros.", null);
            } else {
                procesarErrorRespuesta(response);
                throw new ApiExcepcion(response.statusCode(), "Error al registrar cobro.");
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al registrar cobro de encomienda " + idEncomienda + ": " + e.getMessage());
            throw new ApiExcepcion("Error al registrar cobro: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ViajeDto> listarViajes(String estado, String idSucursalOrigen) {
        try {
            StringBuilder sb = new StringBuilder(baseUrl).append("/api/v1/viajes?");
            if (estado != null && !estado.isBlank()) {
                sb.append("estado=").append(URLEncoder.encode(estado.trim(), StandardCharsets.UTF_8)).append("&");
            }
            if (idSucursalOrigen != null && !idSucursalOrigen.isBlank()) {
                sb.append("idSucursalOrigen=").append(URLEncoder.encode(idSucursalOrigen.trim(), StandardCharsets.UTF_8));
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(sb.toString()))
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .GET();

            adjuntarAutorizacionSiExiste(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<ViajeDto>>() {});
            } else {
                procesarErrorRespuesta(response);
                return List.of();
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al listar viajes: " + e.getMessage());
            throw new ApiExcepcion("Error al consultar viajes: " + e.getMessage(), e);
        }
    }

    @Override
    public RespuestaLoteEscaneoDto procesarLote(LoteEscaneoComando comando) {
        if (!SesionContexto.estaAutenticado()) {
            throw new ApiExcepcion(401, "NO_AUTENTICADO", "Debe iniciar sesión para procesar lotes.", null);
        }

        try {
            String jsonPeticion = objectMapper.writeValueAsString(comando);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/almacen/lotes/escaneo"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPeticion));

            adjuntarAutorizacionSiExiste(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), RespuestaLoteEscaneoDto.class);
            } else if (response.statusCode() == 403) {
                throw new ApiExcepcion(403, "ACCESO_DENEGADO", "No tienes permiso para procesar lotes (requiere Almacenero o Administrador).", null);
            } else {
                procesarErrorRespuesta(response);
                throw new ApiExcepcion(response.statusCode(), "Error al procesar lote de escaneo.");
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al procesar lote de escaneo: " + e.getMessage());
            throw new ApiExcepcion("Error de red al procesar lote: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EncomiendaBusquedaDto> buscarEncomiendasPorDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            throw new ApiExcepcion(400, "DOCUMENTO_REQUERIDO", "Debe ingresar un número de DNI o RUC.", null);
        }

        try {
            String url = baseUrl + "/api/v1/almacen/encomiendas/buscar?documento=" +
                    URLEncoder.encode(documento.trim(), StandardCharsets.UTF_8);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .GET();

            adjuntarAutorizacionSiExiste(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<EncomiendaBusquedaDto>>() {});
            } else if (response.statusCode() == 403) {
                throw new ApiExcepcion(403, "ACCESO_DENEGADO", "No tienes permiso para buscar encomiendas por documento.", null);
            } else {
                procesarErrorRespuesta(response);
                return List.of();
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al buscar encomiendas por documento: " + e.getMessage());
            throw new ApiExcepcion("Error al buscar encomiendas: " + e.getMessage(), e);
        }
    }

    @Override
    public RespuestaIncidenciaDto reportarIncidencia(RegistrarIncidenciaComando comando) {
        if (!SesionContexto.estaAutenticado()) {
            throw new ApiExcepcion(401, "NO_AUTENTICADO", "Debe iniciar sesión para reportar incidencias.", null);
        }

        try {
            String jsonPeticion = objectMapper.writeValueAsString(comando);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/almacen/incidencias"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPeticion));

            adjuntarAutorizacionSiExiste(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), RespuestaIncidenciaDto.class);
            } else if (response.statusCode() == 409) {
                throw new ApiExcepcion(409, "ENCOMIENDA_ENTREGADA", "No se puede reportar incidencia sobre una encomienda ya entregada.", null);
            } else if (response.statusCode() == 403) {
                throw new ApiExcepcion(403, "ACCESO_DENEGADO", "No tienes permiso para reportar incidencias.", null);
            } else {
                procesarErrorRespuesta(response);
                throw new ApiExcepcion(response.statusCode(), "Error al reportar incidencia.");
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al reportar incidencia de encomienda: " + e.getMessage());
            throw new ApiExcepcion("Error al reportar incidencia: " + e.getMessage(), e);
        }
    }

    @Override
    public List<IncidenciaHistorialDto> listarIncidencias(String idEncomienda) {
        try {
            StringBuilder sb = new StringBuilder(baseUrl).append("/api/v1/almacen/incidencias");
            if (idEncomienda != null && !idEncomienda.isBlank()) {
                sb.append("?idEncomienda=").append(URLEncoder.encode(idEncomienda.trim(), StandardCharsets.UTF_8));
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(sb.toString()))
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .GET();

            adjuntarAutorizacionSiExiste(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<IncidenciaHistorialDto>>() {});
            } else {
                procesarErrorRespuesta(response);
                return List.of();
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al listar historial de incidencias: " + e.getMessage());
            throw new ApiExcepcion("Error al consultar historial de incidencias: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // SINCRONIZACIÓN OFFLINE (B4, O2)
    // =========================================================================

    @Override
    public RespuestaSyncLoteDto sincronizarLote(String jsonLote) {
        if (!SesionContexto.estaAutenticado()) {
            throw new ApiExcepcion(401, "NO_AUTENTICADO", "Debe iniciar sesión para sincronizar la cola local.", null);
        }

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/sync/encomiendas"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_DEFAULT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonLote));

            adjuntarAutorizacionSiExiste(builder);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            ServicioConexion.getInstancia().reportarExitoConexion();

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), RespuestaSyncLoteDto.class);
            } else if (response.statusCode() == 401) {
                SesionContexto.notificarSesionExpirada("Sesión expirada durante sincronización offline");
                throw new ApiExcepcion(401, "NO_AUTORIZADO", "Su sesión ha expirado o no tiene autorización.", null);
            } else {
                procesarErrorRespuesta(response);
                throw new ApiExcepcion(response.statusCode(), "Error en sincronización: HTTP " + response.statusCode());
            }
        } catch (ApiExcepcion ae) {
            throw ae;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error de red al sincronizar lote offline: " + e.getMessage());
            ServicioConexion.getInstancia().reportarFalloConexion("Error de red en sincronización: " + e.getMessage());
            throw new ApiExcepcion("Error de comunicación al sincronizar lote: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // ESTADO DE CONEXIÓN (POLLING /actuator/health)
    // =========================================================================

    @Override
    public boolean verificarConectividad() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/actuator/health"))
                    .header("Accept", "application/json")
                    .timeout(TIMEOUT_SALUD)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null && response.body().contains("UP")) {
                return true;
            }

            // Fallback opcional a /api/v1/publico/ping
            HttpRequest pingReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/publico/ping"))
                    .timeout(TIMEOUT_SALUD)
                    .GET()
                    .build();
            HttpResponse<String> pingResp = httpClient.send(pingReq, HttpResponse.BodyHandlers.ofString());
            return pingResp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // UTILITARIOS PRIVADOS
    // =========================================================================

    private void adjuntarAutorizacionSiExiste(HttpRequest.Builder builder) {
        String token = SesionContexto.obtenerToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
    }

    private void procesarErrorRespuesta(HttpResponse<String> response) {
        ServicioConexion.getInstancia().reportarExitoConexion();
        int status = response.statusCode();
        String body = response.body();

        if (status == 401) {
            SesionContexto.notificarSesionExpirada("Sesión expirada o credenciales inválidas");
            throw new ApiExcepcion(401, "NO_AUTORIZADO", "Su sesión ha expirado o no tiene autorización.", null);
        }

        String titulo = "Error HTTP " + status;
        String detalle = "El servidor devolvió el código de estado " + status;
        String codigo = "HTTP_" + status;
        Map<String, String> validaciones = new HashMap<>();

        if (body != null && !body.isBlank()) {
            try {
                JsonNode json = objectMapper.readTree(body);
                if (json.has("title")) {
                    titulo = json.path("title").asText(titulo);
                }
                if (json.has("detail")) {
                    detalle = json.path("detail").asText(detalle);
                }
                if (json.has("message")) {
                    detalle = json.path("message").asText(detalle);
                }
                if (json.has("codigo")) {
                    codigo = json.path("codigo").asText(codigo);
                }
                if (json.has("errores") && json.path("errores").isObject()) {
                    json.path("errores").fields().forEachRemaining(entry ->
                            validaciones.put(entry.getKey(), entry.getValue().asText()));
                }
            } catch (Exception ignored) {
                detalle = body;
            }
        }

        throw new ApiExcepcion(status, codigo, detalle != null ? detalle : titulo, validaciones);
    }
}
