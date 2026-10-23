package pe.marvisur.sgldqr.escritorio.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.*;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.VentanillaPort;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;
import pe.marvisur.sgldqr.escritorio.infraestructura.impresion.ImpresionSimuladaAdapter;
import pe.marvisur.sgldqr.infraestructura.qr.ZXingAdapter;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;
import pe.marvisur.sgldqr.puertos.ImpresionPort;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la pantalla de ventanilla: captura de datos, cotización,
 * precarga de Código Express, emisión de Doble QR e impresión de ticket.
 */
public class ControladorVentanilla {

    private static final Logger LOGGER = Logger.getLogger(ControladorVentanilla.class.getName());

    // Código Express
    @FXML private TextField txtCodigoExpress;
    @FXML private Button btnCargarExpress;

    // Remitente
    @FXML private ComboBox<String> cmbRemitenteTipoDoc;
    @FXML private TextField txtRemitenteNumDoc;
    @FXML private TextField txtRemitenteNombres;
    @FXML private TextField txtRemitenteTelefono;
    @FXML private TextField txtRemitenteCorreo;

    // Destinatario
    @FXML private ComboBox<String> cmbDestinatarioTipoDoc;
    @FXML private TextField txtDestinatarioNumDoc;
    @FXML private TextField txtDestinatarioNombres;
    @FXML private TextField txtDestinatarioTelefono;
    @FXML private TextField txtDestinatarioCorreo;

    // Paquete y Envío
    @FXML private ComboBox<SucursalDto> cmbSucursalDestino;
    @FXML private TextField txtPesoKg;
    @FXML private TextField txtVolumenM3;
    @FXML private TextField txtDescripcion;
    @FXML private RadioButton rdoContado;
    @FXML private RadioButton rdoContraEntrega;

    // Cotización y Acciones
    @FXML private Button btnCotizar;
    @FXML private Label lblTarifaEstimada;
    @FXML private Button btnLimpiar;
    @FXML private Button btnRegistrar;

    // Modal de Resultado Doble QR
    @FXML private VBox panelResultado;
    @FXML private Label lblBadgeEstadoPago;
    @FXML private Label lblResultadoComprobante;
    @FXML private Label lblResultadoMonto;
    @FXML private Label lblResultadoRuta;
    @FXML private Label lblResultadoEstadoLogistico;
    @FXML private TextField txtEnlaceBilletera;
    @FXML private Button btnCopiarEnlace;
    @FXML private ImageView imgQr1;
    @FXML private ImageView imgQr2Enlace;
    @FXML private Button btnImprimirTicket;
    @FXML private Button btnCerrarResultado;

    private final VentanillaPort ventanillaPort;
    private final ImpresionPort impresionPort;
    private final ImagenQRPort imagenQRPort;

    // Referencia temporal en memoria mientras el diálogo de confirmación está abierto
    private EncomiendaRegistradaResultado ultimoResultado;
    private RegistrarEncomiendaComando ultimoComandoRegistrado;

    public ControladorVentanilla() {
        this(new MarvisurHttpClient(), new ImpresionSimuladaAdapter(), new ZXingAdapter());
    }

    public ControladorVentanilla(VentanillaPort ventanillaPort, ImpresionPort impresionPort, ImagenQRPort imagenQRPort) {
        this.ventanillaPort = ventanillaPort;
        this.impresionPort = impresionPort;
        this.imagenQRPort = imagenQRPort;
    }

    @FXML
    public void initialize() {
        configurarControlesIniciales();
        cargarSucursales();
    }

    private void configurarControlesIniciales() {
        ToggleGroup grupoModalidad = new ToggleGroup();
        rdoContado.setToggleGroup(grupoModalidad);
        rdoContraEntrega.setToggleGroup(grupoModalidad);
        rdoContado.setSelected(true);

        List<String> tiposDoc = List.of("DNI", "RUC", "CE");
        cmbRemitenteTipoDoc.setItems(FXCollections.observableArrayList(tiposDoc));
        cmbRemitenteTipoDoc.setValue("DNI");

        cmbDestinatarioTipoDoc.setItems(FXCollections.observableArrayList(tiposDoc));
        cmbDestinatarioTipoDoc.setValue("DNI");

        panelResultado.setVisible(false);
        panelResultado.setManaged(false);
    }

    private void cargarSucursales() {
        Task<List<SucursalDto>> tarea = new Task<>() {
            @Override
            protected List<SucursalDto> call() {
                return ventanillaPort.listarSucursales();
            }
        };

        tarea.setOnSucceeded(e -> {
            List<SucursalDto> sucursales = tarea.getValue();
            cmbSucursalDestino.setItems(FXCollections.observableArrayList(sucursales));
        });

        tarea.setOnFailed(e -> LOGGER.log(Level.WARNING, "No se pudieron precargar las sucursales: " + tarea.getException().getMessage()));

        Thread hilo = new Thread(tarea, "hilo-sucursales");
        hilo.setDaemon(true);
        hilo.start();
    }

    // =========================================================================
    // PRECARGA CÓDIGO EXPRESS
    // =========================================================================

    @FXML
    public void manejarCargarCodigoExpress() {
        String codigo = txtCodigoExpress.getText() != null ? txtCodigoExpress.getText().trim() : "";
        if (codigo.isBlank()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Código Express", "Código Requerido", "Ingrese el código express legible (ej. CEX-481920).");
            return;
        }

        btnCargarExpress.setDisable(true);

        Task<PreRegistroDto> tarea = new Task<>() {
            @Override
            protected PreRegistroDto call() {
                return ventanillaPort.consultarPreRegistro(codigo);
            }
        };

        tarea.setOnSucceeded(e -> {
            btnCargarExpress.setDisable(false);
            PreRegistroDto pre = tarea.getValue();
            precargarFormulario(pre);
            NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Código Express", "Datos Precargados",
                    "Se cargaron exitosamente los datos del pre-registro: " + pre.codigoExpress());
        });

        tarea.setOnFailed(e -> {
            btnCargarExpress.setDisable(false);
            Throwable err = tarea.getException();
            String msg = err != null && err.getMessage() != null ? err.getMessage() : "Error al consultar código express.";
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Código Express", "No encontrado", msg);
        });

        Thread hilo = new Thread(tarea, "hilo-carga-express");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void precargarFormulario(PreRegistroDto pre) {
        if (pre.remitente() != null) {
            ClienteDto rem = pre.remitente();
            if (rem.tipoDocumento() != null) cmbRemitenteTipoDoc.setValue(rem.tipoDocumento());
            if (rem.numeroDocumento() != null) txtRemitenteNumDoc.setText(rem.numeroDocumento());
            if (rem.nombresRazonSocial() != null) txtRemitenteNombres.setText(rem.nombresRazonSocial());
            if (rem.telefono() != null) txtRemitenteTelefono.setText(rem.telefono());
            if (rem.correo() != null) txtRemitenteCorreo.setText(rem.correo());
        }

        if (pre.destinatario() != null) {
            ClienteDto des = pre.destinatario();
            if (des.tipoDocumento() != null) cmbDestinatarioTipoDoc.setValue(des.tipoDocumento());
            if (des.numeroDocumento() != null) txtDestinatarioNumDoc.setText(des.numeroDocumento());
            if (des.nombresRazonSocial() != null) txtDestinatarioNombres.setText(des.nombresRazonSocial());
            if (des.telefono() != null) txtDestinatarioTelefono.setText(des.telefono());
            if (des.correo() != null) txtDestinatarioCorreo.setText(des.correo());
        }

        if (pre.pesoEstimado() != null) {
            txtPesoKg.setText(pre.pesoEstimado().toPlainString());
        }

        if (pre.idSucursalDestino() != null) {
            for (SucursalDto suc : cmbSucursalDestino.getItems()) {
                if (suc.id().equalsIgnoreCase(pre.idSucursalDestino())) {
                    cmbSucursalDestino.setValue(suc);
                    break;
                }
            }
        }

        if (pre.montoCotizado() != null) {
            lblTarifaEstimada.setText("Tarifa: S/. " + pre.montoCotizado().setScale(2).toPlainString());
        }
    }

    // =========================================================================
    // COTIZACIÓN
    // =========================================================================

    @FXML
    public void manejarCotizar() {
        UsuarioSesion sesion = SesionContexto.obtenerSesion();
        String idOrigen = sesion != null && sesion.idSucursal() != null ? sesion.idSucursal() : "SUC-LIM-01";

        SucursalDto destino = cmbSucursalDestino.getValue();
        if (destino == null) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Cotización", "Agencia Destino Requerida", "Seleccione la agencia de destino para cotizar.");
            return;
        }

        BigDecimal peso;
        try {
            peso = new BigDecimal(txtPesoKg.getText().trim());
            if (peso.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Cotización", "Peso Inválido", "Ingrese un peso válido mayor a 0 kg.");
            return;
        }

        BigDecimal vol = null;
        if (txtVolumenM3.getText() != null && !txtVolumenM3.getText().isBlank()) {
            try {
                vol = new BigDecimal(txtVolumenM3.getText().trim());
            } catch (Exception ignored) {
            }
        }

        CotizacionComando comando = new CotizacionComando(idOrigen, destino.id(), peso, vol);
        btnCotizar.setDisable(true);

        Task<CotizacionResultado> tarea = new Task<>() {
            @Override
            protected CotizacionResultado call() {
                return ventanillaPort.cotizar(comando);
            }
        };

        tarea.setOnSucceeded(e -> {
            btnCotizar.setDisable(false);
            CotizacionResultado res = tarea.getValue();
            lblTarifaEstimada.setText("Tarifa: S/. " + res.montoEstimado().setScale(2).toPlainString());
        });

        tarea.setOnFailed(e -> {
            btnCotizar.setDisable(false);
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Cotización", "Fallo al cotizar", tarea.getException().getMessage());
        });

        Thread hilo = new Thread(tarea, "hilo-cotizar");
        hilo.setDaemon(true);
        hilo.start();
    }

    // =========================================================================
    // REGISTRO DE ENCOMIENDA (DOBLE QR)
    // =========================================================================

    @FXML
    public void manejarRegistrarEncomienda() {
        ClienteDto remitente = new ClienteDto(
                cmbRemitenteTipoDoc.getValue(),
                txtRemitenteNumDoc.getText(),
                txtRemitenteNombres.getText(),
                txtRemitenteTelefono.getText(),
                txtRemitenteCorreo.getText()
        );

        ClienteDto destinatario = new ClienteDto(
                cmbDestinatarioTipoDoc.getValue(),
                txtDestinatarioNumDoc.getText(),
                txtDestinatarioNombres.getText(),
                txtDestinatarioTelefono.getText(),
                txtDestinatarioCorreo.getText()
        );

        SucursalDto destino = cmbSucursalDestino.getValue();
        String idDestino = destino != null ? destino.id() : null;

        BigDecimal peso = null;
        try {
            if (txtPesoKg.getText() != null && !txtPesoKg.getText().isBlank()) {
                peso = new BigDecimal(txtPesoKg.getText().trim());
            }
        } catch (Exception ignored) {
        }

        BigDecimal vol = null;
        try {
            if (txtVolumenM3.getText() != null && !txtVolumenM3.getText().isBlank()) {
                vol = new BigDecimal(txtVolumenM3.getText().trim());
            }
        } catch (Exception ignored) {
        }

        String desc = txtDescripcion.getText() != null && !txtDescripcion.getText().isBlank()
                ? txtDescripcion.getText().trim()
                : "ENCOMIENDA ESTÁNDAR";

        String modalidad = rdoContado.isSelected() ? "CONTADO" : "CONTRA_ENTREGA";

        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                remitente, destinatario, idDestino, peso, vol, desc, modalidad
        );

        ValidadorVentanilla.ResultadoValidacion validacion = ValidadorVentanilla.validar(comando);
        if (!validacion.esValido()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Validación de Registro", "Complete los campos requeridos", validacion.mensajeAgrupado());
            return;
        }

        btnRegistrar.setDisable(true);

        Task<EncomiendaRegistradaResultado> tarea = new Task<>() {
            @Override
            protected EncomiendaRegistradaResultado call() {
                return ventanillaPort.registrarEncomienda(comando);
            }
        };

        tarea.setOnSucceeded(e -> {
            btnRegistrar.setDisable(false);
            this.ultimoResultado = tarea.getValue();
            this.ultimoComandoRegistrado = comando;
            mostrarModalResultado(ultimoResultado);
        });

        tarea.setOnFailed(e -> {
            btnRegistrar.setDisable(false);
            Throwable ex = tarea.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Error al registrar encomienda.";
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Registro Fallido", "No se pudo emitir la encomienda", msg);
        });

        Thread hilo = new Thread(tarea, "hilo-registro-encomienda");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarModalResultado(EncomiendaRegistradaResultado res) {
        lblResultadoComprobante.setText("Comprobante: " + res.codigoComprobante());
        lblResultadoMonto.setText("Monto Total: S/. " + res.montoTotal().setScale(2).toPlainString());
        lblResultadoRuta.setText("Ruta: " + res.idSucursalOrigen() + " ➔ " + res.idSucursalDestino());
        lblResultadoEstadoLogistico.setText("Estado Logístico: " + res.estadoLogistico());

        boolean pagado = "PAGADO".equalsIgnoreCase(res.estadoPago());
        lblBadgeEstadoPago.setText(res.estadoPago());
        lblBadgeEstadoPago.getStyleClass().setAll(pagado ? "badge-pagado" : "badge-pendiente");

        txtEnlaceBilletera.setText(res.enlaceBilletera() != null ? res.enlaceBilletera() : "N/D");

        // QR1 Físico (viene en Base64 desde el backend)
        if (res.qr1() != null && res.qr1().imagenPngBase64() != null) {
            try {
                byte[] bytesQr1 = Base64.getDecoder().decode(res.qr1().imagenPngBase64());
                imgQr1.setImage(new Image(new ByteArrayInputStream(bytesQr1)));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error al decodificar QR1 base64: " + ex.getMessage());
            }
        }

        // QR2 Digital (Renderizado al vuelo del enlace con ZXingAdapter)
        if (res.enlaceBilletera() != null && !res.enlaceBilletera().isBlank()) {
            try {
                byte[] bytesQr2 = imagenQRPort.generarPNG(res.enlaceBilletera(), 200);
                imgQr2Enlace.setImage(new Image(new ByteArrayInputStream(bytesQr2)));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error al generar imagen de QR2 enlace: " + ex.getMessage());
            }
        }

        panelResultado.setVisible(true);
        panelResultado.setManaged(true);
    }

    @FXML
    public void copiarEnlaceBilletera() {
        String url = txtEnlaceBilletera.getText();
        if (url != null && !url.isBlank()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(url);
            clipboard.setContent(content);
            NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Billetera Digital", "Enlace Copiado",
                    "El enlace de retiro ha sido copiado al portapapeles del sistema.");
        }
    }

    @FXML
    public void manejarImprimirTicket() {
        if (ultimoResultado == null) return;

        String comprobante = ultimoResultado.codigoComprobante();
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String origen = ultimoResultado.idSucursalOrigen();
        String destino = ultimoResultado.idSucursalDestino();
        String remitente = ultimoComandoRegistrado != null && ultimoComandoRegistrado.remitente() != null
                ? ultimoComandoRegistrado.remitente().nombresRazonSocial() : "REMITENTE";
        String destinatario = ultimoComandoRegistrado != null && ultimoComandoRegistrado.destinatario() != null
                ? ultimoComandoRegistrado.destinatario().nombresRazonSocial() : "DESTINATARIO";
        String peso = ultimoResultado.pesoKg() != null ? ultimoResultado.pesoKg().toPlainString() : "0.00";
        String total = ultimoResultado.montoTotal() != null ? ultimoResultado.montoTotal().toPlainString() : "0.00";
        String pago = ultimoResultado.estadoPago();

        String ticket = """
                ================================
                      EXPRESO MARVISUR S.A.
                   COMPROBANTE DE ENCOMIENDA
                ================================
                COMPROBANTE: %s
                FECHA:       %s
                ORIGEN:      %s
                DESTINO:     %s
                --------------------------------
                REMITENTE:   %s
                DESTINATARIO:%s
                --------------------------------
                PESO:        %s Kg
                ESTADO PAGO: %s
                TOTAL:       S/. %s
                ================================
                 Consulte su retiro escaneando
                  el enlace de su Billetera
                ================================
                """.formatted(comprobante, fecha, origen, destino, remitente, destinatario, peso, pago, total);

        impresionPort.imprimirTicket(ticket);

        NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Impresión Térmica", "Ticket Generado",
                "Se ha generado y guardado la imagen del ticket térmico de 58 mm en la carpeta salida/.");
    }

    @FXML
    public void cerrarModalResultado() {
        // REGLA DE SEGURIDAD: Limpiar en memoria el token QR2 tras cerrar el diálogo
        this.ultimoResultado = null;
        this.ultimoComandoRegistrado = null;
        imgQr1.setImage(null);
        imgQr2Enlace.setImage(null);
        panelResultado.setVisible(false);
        panelResultado.setManaged(false);
        limpiarFormulario();
    }

    @FXML
    public void limpiarFormulario() {
        txtCodigoExpress.clear();
        txtRemitenteNumDoc.clear();
        txtRemitenteNombres.clear();
        txtRemitenteTelefono.clear();
        txtRemitenteCorreo.clear();

        txtDestinatarioNumDoc.clear();
        txtDestinatarioNombres.clear();
        txtDestinatarioTelefono.clear();
        txtDestinatarioCorreo.clear();

        txtPesoKg.clear();
        txtVolumenM3.clear();
        txtDescripcion.clear();
        rdoContado.setSelected(true);
        lblTarifaEstimada.setText("Tarifa: S/. --");
    }
}
