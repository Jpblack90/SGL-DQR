package pe.marvisur.sgldqr.escritorio.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.*;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PestanasBloqueadasOfflineViewModel;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.AlmacenPort;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.RutasEscritorio;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;
import pe.marvisur.sgldqr.escritorio.infraestructura.impresion.ImpresionSimuladaAdapter;
import pe.marvisur.sgldqr.escritorio.ui.componentes.CampoEscaneo;
import pe.marvisur.sgldqr.puertos.ImpresionPort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador integral del módulo de almacén:
 * - Pestaña 1: Validación cruzada Doble QR, bloqueo por pago y entrega física.
 * - Pestaña 2: Procesamiento masivo de lotes de escaneo (Batch Scanning).
 * - Pestaña 3: Contingencia, búsqueda por DNI/RUC e incidencias de etiquetas.
 */
public class ControladorAlmacen {

    private static final Logger LOGGER = Logger.getLogger(ControladorAlmacen.class.getName());

    // Pestañas principales
    @FXML private TabPane tabPaneAlmacen;
    @FXML private Tab tabEntrega;
    @FXML private Tab tabLotes;
    @FXML private Tab tabContingencia;

    // --- Pestaña 1: Entrega ---
    @FXML private CampoEscaneo txtQr1Entrega;
    @FXML private CampoEscaneo txtQr2Entrega;
    @FXML private Button btnLimpiarEntrega;
    @FXML private Button btnValidarEntrega;

    @FXML private VBox panelResultadoEntrega;
    @FXML private Label lblTituloResultadoEntrega;
    @FXML private Label lblLatenciaEntrega;
    @FXML private Label lblMensajeResultadoEntrega;
    @FXML private Label lblDetalleComprobanteEntrega;
    @FXML private Label lblMontoPendienteEntrega;
    @FXML private HBox panelAccionCobroEntrega;
    @FXML private Button btnCobrarEntrega;
    @FXML private Button btnReintentarValidacion;

    // Secretos QR en memoria exclusivamente durante el flujo de validación / cobro
    private String qr1EnMemoria;
    private String qr2EnMemoria;
    private String idEncomiendaBloqueada;

    // --- Pestaña 2: Lotes ---
    @FXML private ComboBox<String> cmbAccionLote;
    @FXML private VBox contenedorViajeLote;
    @FXML private ComboBox<ViajeDto> cmbViajeLote;
    @FXML private CampoEscaneo txtQr1Lote;
    @FXML private Label lblContadorLote;
    @FXML private Button btnQuitarItemLote;
    @FXML private Button btnLimpiarListaLote;
    @FXML private Button btnProcesarLote;
    @FXML private ListView<String> listaEscaneoLote;

    @FXML private VBox panelResultadoLote;
    @FXML private Label lblResumenTotal;
    @FXML private Label lblResumenExitosos;
    @FXML private Label lblResumenYaRegistrados;
    @FXML private Label lblResumenErrores;
    @FXML private TableView<ItemResultadoLoteDto> tablaResultadoLote;
    @FXML private TableColumn<ItemResultadoLoteDto, String> colLoteQr1;
    @FXML private TableColumn<ItemResultadoLoteDto, String> colLoteResultado;
    @FXML private TableColumn<ItemResultadoLoteDto, String> colLoteMotivo;
    @FXML private Button btnReintentarFallidos;
    @FXML private Button btnExportarCsv;

    private final ObservableList<String> itemsEscaneadosLote = FXCollections.observableArrayList();
    private final ObservableList<ItemResultadoLoteDto> resultadosProcesadosLote = FXCollections.observableArrayList();

    // --- Pestaña 3: Contingencia ---
    @FXML private TextField txtBusquedaDoc;
    @FXML private Button btnBuscarDoc;
    @FXML private TableView<EncomiendaBusquedaDto> tablaBusquedaEncomiendas;
    @FXML private TableColumn<EncomiendaBusquedaDto, String> colBusqComprobante;
    @FXML private TableColumn<EncomiendaBusquedaDto, String> colBusqRemitente;
    @FXML private TableColumn<EncomiendaBusquedaDto, String> colBusqDestinatario;
    @FXML private TableColumn<EncomiendaBusquedaDto, String> colBusqDestino;
    @FXML private TableColumn<EncomiendaBusquedaDto, String> colBusqPeso;
    @FXML private TableColumn<EncomiendaBusquedaDto, String> colBusqTotal;
    @FXML private TableColumn<EncomiendaBusquedaDto, String> colBusqEstadoPago;
    @FXML private TableColumn<EncomiendaBusquedaDto, String> colBusqEstadoLogistico;

    @FXML private Button btnCobroContingencia;
    @FXML private Button btnAbrirReportarIncidencia;

    @FXML private HBox panelIncidencia;
    @FXML private ComboBox<String> cmbTipoIncidencia;
    @FXML private TextArea txtMotivoIncidencia;
    @FXML private Button btnConfirmarIncidencia;
    @FXML private VBox panelQr1Regenerado;
    @FXML private ImageView imgQr1Regenerado;
    @FXML private Button btnImprimirEtiquetaRegenerada;
    @FXML private TableView<IncidenciaHistorialDto> tablaHistorialIncidencias;
    @FXML private TableColumn<IncidenciaHistorialDto, String> colIncTipo;
    @FXML private TableColumn<IncidenciaHistorialDto, String> colIncMotivo;
    @FXML private TableColumn<IncidenciaHistorialDto, String> colIncFecha;

    private final ObservableList<EncomiendaBusquedaDto> listaEncomiendasBusqueda = FXCollections.observableArrayList();
    private final ObservableList<IncidenciaHistorialDto> listaHistorialIncidencias = FXCollections.observableArrayList();
    private String qr1RegeneradoBase64Actual;
    private EncomiendaBusquedaDto encomiendaSeleccionadaContingencia;

    private final AlmacenPort almacenPort;
    private final ImpresionPort impresionPort;

    public ControladorAlmacen() {
        this(new MarvisurHttpClient(), new ImpresionSimuladaAdapter());
    }

    public ControladorAlmacen(AlmacenPort almacenPort, ImpresionPort impresionPort) {
        this.almacenPort = almacenPort;
        this.impresionPort = impresionPort;
    }

    @FXML
    public void initialize() {
        configurarPermisosPestanas();
        configurarBloqueoOfflinePestanas();
        inicializarPestanaEntrega();
        inicializarPestanaLotes();
        inicializarPestanaContingencia();
    }

    private void configurarPermisosPestanas() {
        UsuarioSesion sesion = SesionContexto.obtenerSesion();
        String rol = sesion != null ? sesion.rol() : "";
        boolean autorizadoAlmacen = ModeloMenuRol.estaAutorizado(rol, ModeloMenuRol.ModuloMenu.ALMACEN);
        if (!autorizadoAlmacen && tabPaneAlmacen != null) {
            tabPaneAlmacen.getTabs().remove(tabEntrega);
            tabPaneAlmacen.getTabs().remove(tabLotes);
        }
    }

    private void configurarBloqueoOfflinePestanas() {
        if (tabPaneAlmacen != null) {
            tabPaneAlmacen.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null && !ServicioConexion.getInstancia().estaEnLinea()) {
                    NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Requiere Conexión", "Operación no disponible sin conexión",
                            PestanasBloqueadasOfflineViewModel.MENSAJE_REQUIERE_CONEXION);
                }
            });
        }
    }

    public static String obtenerMensajeErrorClaro(Throwable error) {
        if (error == null) {
            return "Sin conexión con el servidor.";
        }
        Throwable causa = error;
        while (causa != null) {
            if (causa instanceof java.net.ConnectException
                    || causa instanceof java.net.http.HttpConnectTimeoutException
                    || causa instanceof java.net.http.HttpTimeoutException
                    || causa instanceof java.util.concurrent.TimeoutException
                    || causa instanceof java.net.SocketTimeoutException) {
                return "Sin conexión con el servidor.";
            }
            if (causa.getCause() == causa) break;
            causa = causa.getCause();
        }
        String msg = error.getMessage();
        if (msg == null || msg.isBlank() || "null".equalsIgnoreCase(msg.trim())) {
            return "Sin conexión con el servidor.";
        }
        return msg.trim();
    }

    // =========================================================================
    // 1. PESTAÑA ENTREGA (VALIDACIÓN CRUZADA)
    // =========================================================================

    private void inicializarPestanaEntrega() {
        txtQr1Entrega.setOnCodigoEscaneado(codigo -> {
            this.qr1EnMemoria = codigo;
            txtQr2Entrega.enfocar();
        });

        txtQr2Entrega.setOnCodigoEscaneado(codigo -> {
            this.qr2EnMemoria = codigo;
            if (this.qr1EnMemoria != null && !this.qr1EnMemoria.isBlank()) {
                manejarValidarEntrega();
            } else {
                txtQr1Entrega.enfocar();
            }
        });

        limpiarEntrega();
    }

    @FXML
    public void manejarValidarEntrega() {
        if (!ServicioConexion.getInstancia().estaEnLinea()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Requiere Conexión", "Operación no disponible sin conexión",
                    PestanasBloqueadasOfflineViewModel.MENSAJE_REQUIERE_CONEXION);
            return;
        }

        String qr1 = (this.idEncomiendaBloqueada != null && this.qr1EnMemoria != null && !this.qr1EnMemoria.isBlank())
                ? this.qr1EnMemoria : txtQr1Entrega.getText().trim();
        String qr2 = (this.idEncomiendaBloqueada != null && this.qr2EnMemoria != null && !this.qr2EnMemoria.isBlank())
                ? this.qr2EnMemoria : txtQr2Entrega.getText().trim();

        if (qr1.isBlank() || qr2.isBlank()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Validación Cruzada", "Faltan códigos",
                    "Debe escanear tanto el QR1 operativo como el QR2 digital del cliente.");
            return;
        }

        // Guardar en memoria mientras dure el proceso
        this.qr1EnMemoria = qr1;
        this.qr2EnMemoria = qr2;

        btnValidarEntrega.setDisable(true);

        Task<ValidacionEntregaResultado> tarea = new Task<>() {
            @Override
            protected ValidacionEntregaResultado call() {
                return almacenPort.validarEntrega(qr1, qr2);
            }
        };

        tarea.setOnSucceeded(e -> {
            btnValidarEntrega.setDisable(false);
            mostrarResultadoEntrega(tarea.getValue());
        });

        tarea.setOnFailed(e -> {
            btnValidarEntrega.setDisable(false);
            Throwable err = tarea.getException();
            String msg = obtenerMensajeErrorClaro(err);
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Error de Red / Servidor", "Fallo al validar", msg);
        });

        Thread hilo = new Thread(tarea, "hilo-validar-entrega");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarResultadoEntrega(ValidacionEntregaResultado res) {
        panelResultadoEntrega.setVisible(true);
        panelResultadoEntrega.setManaged(true);
        lblLatenciaEntrega.setText(res.latenciaMs() + " ms");
        lblMensajeResultadoEntrega.setText(res.mensaje());

        if (res.tipo() != ValidacionEntregaResultado.TipoValidacion.PAGO_PENDIENTE) {
            this.qr1EnMemoria = null;
            this.qr2EnMemoria = null;
            this.idEncomiendaBloqueada = null;
        }

        switch (res.tipo()) {
            case EXITOSO -> {
                panelResultadoEntrega.setStyle("-fx-border-color: #10B981; -fx-border-width: 2px; -fx-background-color: #F0FDF4;");
                lblTituloResultadoEntrega.setText("✅ Entrega Física Autorizada");
                lblTituloResultadoEntrega.setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold; -fx-font-size: 15px;");
                lblDetalleComprobanteEntrega.setText("Comprobante: " + res.codigoComprobante());
                lblMontoPendienteEntrega.setVisible(false);
                lblMontoPendienteEntrega.setManaged(false);
                panelAccionCobroEntrega.setVisible(false);
                panelAccionCobroEntrega.setManaged(false);

                // REGLA DE SEGURIDAD: Limpiar de inmediato los QR en memoria tras éxito
                this.qr1EnMemoria = null;
                this.qr2EnMemoria = null;
                this.idEncomiendaBloqueada = null;
                txtQr1Entrega.clear();
                txtQr2Entrega.clear();
            }
            case PAGO_PENDIENTE -> {
                panelResultadoEntrega.setStyle("-fx-border-color: #F59E0B; -fx-border-width: 2px; -fx-background-color: #FFFBEB;");
                lblTituloResultadoEntrega.setText("⚠️ Entrega Bloqueada por Pago Pendiente");
                lblTituloResultadoEntrega.setStyle("-fx-text-fill: #B45309; -fx-font-weight: bold; -fx-font-size: 15px;");
                lblDetalleComprobanteEntrega.setText("Comprobante: " + (res.codigoComprobante() != null ? res.codigoComprobante() : "--"));
                lblMontoPendienteEntrega.setText("Pago pendiente: S/ " + (res.montoPendiente() != null ? res.montoPendiente().setScale(2).toPlainString() : "0.00"));
                lblMontoPendienteEntrega.setVisible(true);
                lblMontoPendienteEntrega.setManaged(true);

                this.idEncomiendaBloqueada = res.idEncomienda();
                panelAccionCobroEntrega.setVisible(true);
                panelAccionCobroEntrega.setManaged(true);
                btnCobrarEntrega.setDisable(false);
                btnReintentarValidacion.setDisable(true);
            }
            case TOKEN_INVALIDO -> {
                panelResultadoEntrega.setStyle("-fx-border-color: #DC2626; -fx-border-width: 2px; -fx-background-color: #FEF2F2;");
                lblTituloResultadoEntrega.setText("❌ Token QR2 Inválido o Ya Utilizado");
                lblTituloResultadoEntrega.setStyle("-fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-font-size: 15px;");
                lblDetalleComprobanteEntrega.setText("Código digital no corresponde o ya fue invalidado irreversiblemente.");
                lblMontoPendienteEntrega.setVisible(false);
                lblMontoPendienteEntrega.setManaged(false);
                panelAccionCobroEntrega.setVisible(false);
                panelAccionCobroEntrega.setManaged(false);
            }
            case TRANSICION_INVALIDA -> {
                panelResultadoEntrega.setStyle("-fx-border-color: #DC2626; -fx-border-width: 2px; -fx-background-color: #FEF2F2;");
                lblTituloResultadoEntrega.setText("❌ Transición Inválida");
                lblTituloResultadoEntrega.setStyle("-fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-font-size: 15px;");
                lblDetalleComprobanteEntrega.setText("La encomienda debe estar en estado 'EN_ALMACEN_DESTINO'. Estado actual: " + res.estadoLogistico());
                lblMontoPendienteEntrega.setVisible(false);
                lblMontoPendienteEntrega.setManaged(false);
                panelAccionCobroEntrega.setVisible(false);
                panelAccionCobroEntrega.setManaged(false);
            }
            case NO_ENCONTRADO -> {
                panelResultadoEntrega.setStyle("-fx-border-color: #DC2626; -fx-border-width: 2px; -fx-background-color: #FEF2F2;");
                lblTituloResultadoEntrega.setText("❌ QR1 No Registrado");
                lblTituloResultadoEntrega.setStyle("-fx-text-fill: #991B1B; -fx-font-weight: bold; -fx-font-size: 15px;");
                lblDetalleComprobanteEntrega.setText("No se encontró encomienda asociada en el sistema.");
                lblMontoPendienteEntrega.setVisible(false);
                lblMontoPendienteEntrega.setManaged(false);
                panelAccionCobroEntrega.setVisible(false);
                panelAccionCobroEntrega.setManaged(false);
            }
            default -> {
                panelResultadoEntrega.setStyle("-fx-border-color: #DC2626; -fx-border-width: 2px; -fx-background-color: #FEF2F2;");
                lblTituloResultadoEntrega.setText("❌ Error al Validar");
                lblDetalleComprobanteEntrega.setText(res.mensaje());
            }
        }
    }

    @FXML
    public void manejarCobroContraEntrega() {
        if (!ServicioConexion.getInstancia().estaEnLinea()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Requiere Conexión", "Operación no disponible sin conexión",
                    PestanasBloqueadasOfflineViewModel.MENSAJE_REQUIERE_CONEXION);
            return;
        }

        if (idEncomiendaBloqueada == null || idEncomiendaBloqueada.isBlank()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Cobro", "Sin encomienda", "No hay encomienda pendiente de cobro.");
            return;
        }

        btnCobrarEntrega.setDisable(true);

        Task<RespuestaCobroDto> tarea = new Task<>() {
            @Override
            protected RespuestaCobroDto call() {
                return almacenPort.registrarCobro(idEncomiendaBloqueada);
            }
        };

        tarea.setOnSucceeded(e -> {
            RespuestaCobroDto respuesta = tarea.getValue();
            NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Cobro Exitoso", "Encomienda Desbloqueada",
                    "Se registró el cobro por S/ " + respuesta.montoTotal().setScale(2) + ". Proceda a reintentar la validación.");
            btnCobrarEntrega.setDisable(true);
            btnReintentarValidacion.setDisable(false);
            lblMontoPendienteEntrega.setText("✅ Cobro registrado. Estado: " + respuesta.estadoPago());
            lblMontoPendienteEntrega.setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold;");
        });

        tarea.setOnFailed(e -> {
            btnCobrarEntrega.setDisable(false);
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Fallo al Cobrar", "No se pudo registrar el cobro",
                    obtenerMensajeErrorClaro(tarea.getException()));
        });

        Thread hilo = new Thread(tarea, "hilo-cobro-entrega");
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    public void manejarReintentarValidacion() {
        if (qr1EnMemoria != null && qr2EnMemoria != null) {
            manejarValidarEntrega();
        } else {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Reintento", "Códigos ausentes", "Escanee nuevamente los códigos para reintentar.");
        }
    }

    @FXML
    public void limpiarEntrega() {
        this.qr1EnMemoria = null;
        this.qr2EnMemoria = null;
        this.idEncomiendaBloqueada = null;
        txtQr1Entrega.clear();
        txtQr2Entrega.clear();
        panelResultadoEntrega.setVisible(false);
        panelResultadoEntrega.setManaged(false);
        panelAccionCobroEntrega.setVisible(false);
        panelAccionCobroEntrega.setManaged(false);
        txtQr1Entrega.enfocar();
    }

    // =========================================================================
    // 2. PESTAÑA LOTES (BATCH SCANNING)
    // =========================================================================

    private void inicializarPestanaLotes() {
        cmbAccionLote.setItems(FXCollections.observableArrayList(
                "RECEPCIONAR_ORIGEN",
                "DESPACHAR_A_VIAJE",
                "RECEPCIONAR_DESTINO"
        ));
        cmbAccionLote.setValue("RECEPCIONAR_ORIGEN");

        cmbAccionLote.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean esDespacho = "DESPACHAR_A_VIAJE".equalsIgnoreCase(newVal);
            contenedorViajeLote.setVisible(esDespacho);
            contenedorViajeLote.setManaged(esDespacho);
            if (esDespacho) {
                cargarViajesDisponibles();
            }
        });

        listaEscaneoLote.setItems(itemsEscaneadosLote);

        txtQr1Lote.setOnCodigoEscaneado(codigo -> {
            String codigoLimpio = codigo.trim();
            if (itemsEscaneadosLote.contains(codigoLimpio)) {
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Lote de Escaneo", "Código Duplicado",
                        "El código QR1 ya se encuentra en la lista de escaneo actual.");
            } else {
                itemsEscaneadosLote.add(codigoLimpio);
                actualizarContadorLote();
            }
            txtQr1Lote.enfocar();
        });

        colLoteQr1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().qr1()));
        colLoteResultado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().resultado()));
        colLoteMotivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMotivoLegible()));

        // Formato visual de la celda de resultado (verde, gris, rojo)
        colLoteResultado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("OK".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold;");
                    } else if ("YA_REGISTRADA".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tablaResultadoLote.setItems(resultadosProcesadosLote);
    }

    private void cargarViajesDisponibles() {
        UsuarioSesion sesion = SesionContexto.obtenerSesion();
        String sucursal = sesion != null ? sesion.idSucursal() : "";

        Task<List<ViajeDto>> tarea = new Task<>() {
            @Override
            protected List<ViajeDto> call() {
                return almacenPort.listarViajes("PROGRAMADO", sucursal);
            }
        };

        tarea.setOnSucceeded(e -> cmbViajeLote.setItems(FXCollections.observableArrayList(tarea.getValue())));
        tarea.setOnFailed(e -> LOGGER.warning("No se pudieron cargar viajes: " + obtenerMensajeErrorClaro(tarea.getException())));

        Thread hilo = new Thread(tarea, "hilo-viajes-lote");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void actualizarContadorLote() {
        lblContadorLote.setText("Total escaneados: " + itemsEscaneadosLote.size());
    }

    @FXML
    public void quitarItemLote() {
        String seleccionado = listaEscaneoLote.getSelectionModel().getSelectedItem();
        if (seleccionado != null) {
            itemsEscaneadosLote.remove(seleccionado);
            actualizarContadorLote();
        }
    }

    @FXML
    public void limpiarListaLote() {
        itemsEscaneadosLote.clear();
        actualizarContadorLote();
    }

    @FXML
    public void manejarProcesarLote() {
        if (!ServicioConexion.getInstancia().estaEnLinea()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Requiere Conexión", "Operación no disponible sin conexión",
                    PestanasBloqueadasOfflineViewModel.MENSAJE_REQUIERE_CONEXION);
            return;
        }

        if (itemsEscaneadosLote.isEmpty()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Lote de Escaneo", "Lista Vacía", "Debe escanear al menos un código QR1.");
            return;
        }

        String accion = cmbAccionLote.getValue();
        String idViaje = null;

        if ("DESPACHAR_A_VIAJE".equalsIgnoreCase(accion)) {
            ViajeDto viaje = cmbViajeLote.getValue();
            if (viaje == null) {
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Lote de Escaneo", "Viaje Requerido", "Debe seleccionar un viaje programado para despachar.");
                return;
            }
            idViaje = viaje.idViaje();
        }

        btnProcesarLote.setDisable(true);
        LoteEscaneoComando comando = new LoteEscaneoComando(accion, idViaje, new ArrayList<>(itemsEscaneadosLote));

        Task<RespuestaLoteEscaneoDto> tarea = new Task<>() {
            @Override
            protected RespuestaLoteEscaneoDto call() {
                return almacenPort.procesarLote(comando);
            }
        };

        tarea.setOnSucceeded(e -> {
            btnProcesarLote.setDisable(false);
            RespuestaLoteEscaneoDto res = tarea.getValue();
            mostrarResultadoLote(res);
            itemsEscaneadosLote.clear();
            actualizarContadorLote();
        });

        tarea.setOnFailed(e -> {
            btnProcesarLote.setDisable(false);
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Lote de Escaneo", "Error al procesar lote",
                    obtenerMensajeErrorClaro(tarea.getException()));
        });

        Thread hilo = new Thread(tarea, "hilo-procesar-lote");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarResultadoLote(RespuestaLoteEscaneoDto res) {
        lblResumenTotal.setText("Total: " + res.totalProcesados());
        lblResumenExitosos.setText("Exitosos: " + res.totalExitosos());
        lblResumenYaRegistrados.setText("Ya Registrados: " + res.totalYaRegistrados());
        lblResumenErrores.setText("Errores: " + res.totalErrores());

        resultadosProcesadosLote.setAll(res.detalles());
        panelResultadoLote.setVisible(true);
        panelResultadoLote.setManaged(true);
    }

    @FXML
    public void reintentarSoloFallidos() {
        List<String> fallidos = resultadosProcesadosLote.stream()
                .filter(ItemResultadoLoteDto::esError)
                .map(ItemResultadoLoteDto::qr1)
                .filter(Objects::nonNull)
                .toList();

        if (fallidos.isEmpty()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Reintento de Lote", "Sin errores", "No hay ítems con error para reintentar.");
            return;
        }

        itemsEscaneadosLote.setAll(fallidos);
        actualizarContadorLote();
        NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Reintento de Lote", "Ítems cargados",
                "Se cargaron " + fallidos.size() + " códigos con error en la cola de escaneo.");
    }

    @FXML
    public void exportarResultadoLoteCsv() {
        if (resultadosProcesadosLote.isEmpty()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Exportar CSV", "Sin datos", "No hay resultados de lote para exportar.");
            return;
        }

        try {
            Path salidaDir = RutasEscritorio.obtenerDirectorioSalida();
            if (!Files.exists(salidaDir)) {
                Files.createDirectories(salidaDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path csvArchivo = salidaDir.resolve("lote_" + timestamp + ".csv");

            StringBuilder csv = new StringBuilder("QR1,Resultado,Motivo\n");
            for (ItemResultadoLoteDto item : resultadosProcesadosLote) {
                csv.append('"').append(item.qr1() != null ? item.qr1() : "").append("\",")
                   .append('"').append(item.resultado() != null ? item.resultado() : "").append("\",")
                   .append('"').append(item.getMotivoLegible()).append("\"\n");
            }

            Files.writeString(csvArchivo, csv.toString());
            NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Exportar CSV", "Archivo Generado",
                    "Se exportó el reporte del lote exitosamente a: " + csvArchivo.toAbsolutePath());
        } catch (IOException e) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Exportar CSV", "Error al escribir archivo", e.getMessage());
        }
    }

    // =========================================================================
    // 3. PESTAÑA CONTINGENCIA (BÚSQUEDA DNI/RUC E INCIDENCIAS)
    // =========================================================================

    private void inicializarPestanaContingencia() {
        colBusqComprobante.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().codigoComprobante()));
        colBusqRemitente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombreRemitente()));
        colBusqDestinatario.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombreDestinatario()));
        colBusqDestino.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().idSucursalDestino()));
        colBusqPeso.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().pesoKg() != null ? d.getValue().pesoKg().toPlainString() : ""));
        colBusqTotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().montoTotal() != null ? d.getValue().montoTotal().toPlainString() : ""));
        colBusqEstadoPago.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().estadoPago()));
        colBusqEstadoLogistico.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().estadoLogistico()));

        tablaBusquedaEncomiendas.setItems(listaEncomiendasBusqueda);

        tablaBusquedaEncomiendas.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            this.encomiendaSeleccionadaContingencia = newVal;
            boolean seleccionado = newVal != null;
            btnAbrirReportarIncidencia.setDisable(!seleccionado);
            btnCobroContingencia.setDisable(!seleccionado || !newVal.esPagoPendiente());

            if (seleccionado) {
                cargarHistorialIncidencias(newVal.idEncomienda());
            } else {
                listaHistorialIncidencias.clear();
            }
        });

        cmbTipoIncidencia.setItems(FXCollections.observableArrayList("ETIQUETA_DANADA", "ETIQUETA_PERDIDA"));
        cmbTipoIncidencia.setValue("ETIQUETA_DANADA");

        colIncTipo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().tipoIncidencia()));
        colIncMotivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().motivoReimpresion()));
        colIncFecha.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().fechaIncidencia()));
        tablaHistorialIncidencias.setItems(listaHistorialIncidencias);
    }

    @FXML
    public void manejarBuscarPorDocumento() {
        if (!ServicioConexion.getInstancia().estaEnLinea()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Requiere Conexión", "Operación no disponible sin conexión",
                    PestanasBloqueadasOfflineViewModel.MENSAJE_REQUIERE_CONEXION);
            return;
        }

        String doc = txtBusquedaDoc.getText() != null ? txtBusquedaDoc.getText().trim() : "";
        if (doc.isBlank()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Búsqueda de Contingencia", "Documento Requerido",
                    "Ingrese el DNI (8 dígitos) o RUC (11 dígitos) a buscar.");
            return;
        }

        btnBuscarDoc.setDisable(true);

        Task<List<EncomiendaBusquedaDto>> tarea = new Task<>() {
            @Override
            protected List<EncomiendaBusquedaDto> call() {
                return almacenPort.buscarEncomiendasPorDocumento(doc);
            }
        };

        tarea.setOnSucceeded(e -> {
            btnBuscarDoc.setDisable(false);
            listaEncomiendasBusqueda.setAll(tarea.getValue());
            if (listaEncomiendasBusqueda.isEmpty()) {
                NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Búsqueda", "Sin resultados",
                        "No se encontraron encomiendas no entregadas asociadas al documento: " + doc);
            }
        });

        tarea.setOnFailed(e -> {
            btnBuscarDoc.setDisable(false);
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Búsqueda", "Error en búsqueda",
                    obtenerMensajeErrorClaro(tarea.getException()));
        });

        Thread hilo = new Thread(tarea, "hilo-buscar-doc");
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    public void manejarCobroContingencia() {
        if (!ServicioConexion.getInstancia().estaEnLinea()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Requiere Conexión", "Operación no disponible sin conexión",
                    PestanasBloqueadasOfflineViewModel.MENSAJE_REQUIERE_CONEXION);
            return;
        }

        if (encomiendaSeleccionadaContingencia == null) return;

        Task<RespuestaCobroDto> tarea = new Task<>() {
            @Override
            protected RespuestaCobroDto call() {
                return almacenPort.registrarCobro(encomiendaSeleccionadaContingencia.idEncomienda());
            }
        };

        tarea.setOnSucceeded(e -> {
            NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Cobro", "Cobro Exitoso",
                    "Se registró el cobro para " + encomiendaSeleccionadaContingencia.codigoComprobante() + ".");
            manejarBuscarPorDocumento();
        });

        tarea.setOnFailed(e -> NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Cobro", "Error al cobrar",
                obtenerMensajeErrorClaro(tarea.getException())));

        Thread hilo = new Thread(tarea, "hilo-cobro-contingencia");
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    public void abrirFormularioIncidencia() {
        if (encomiendaSeleccionadaContingencia == null) return;
        panelIncidencia.setVisible(true);
        panelIncidencia.setManaged(true);
        panelQr1Regenerado.setVisible(false);
        panelQr1Regenerado.setManaged(false);
        txtMotivoIncidencia.clear();
    }

    @FXML
    public void cerrarPanelIncidencia() {
        panelIncidencia.setVisible(false);
        panelIncidencia.setManaged(false);
        panelQr1Regenerado.setVisible(false);
        panelQr1Regenerado.setManaged(false);
    }

    @FXML
    public void manejarRegistrarIncidencia() {
        if (!ServicioConexion.getInstancia().estaEnLinea()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Requiere Conexión", "Operación no disponible sin conexión",
                    PestanasBloqueadasOfflineViewModel.MENSAJE_REQUIERE_CONEXION);
            return;
        }

        if (encomiendaSeleccionadaContingencia == null) return;

        String tipo = cmbTipoIncidencia.getValue();
        String motivo = txtMotivoIncidencia.getText() != null && !txtMotivoIncidencia.getText().isBlank()
                ? txtMotivoIncidencia.getText().trim() : "Etiqueta ilegible";

        RegistrarIncidenciaComando comando = new RegistrarIncidenciaComando(
                encomiendaSeleccionadaContingencia.idEncomienda(), tipo, motivo
        );

        btnConfirmarIncidencia.setDisable(true);

        Task<RespuestaIncidenciaDto> tarea = new Task<>() {
            @Override
            protected RespuestaIncidenciaDto call() {
                return almacenPort.reportarIncidencia(comando);
            }
        };

        tarea.setOnSucceeded(e -> {
            btnConfirmarIncidencia.setDisable(false);
            RespuestaIncidenciaDto res = tarea.getValue();
            this.qr1RegeneradoBase64Actual = res.qr1Base64();

            if (qr1RegeneradoBase64Actual != null) {
                try {
                    String base64Limpia = qr1RegeneradoBase64Actual.contains(",")
                            ? qr1RegeneradoBase64Actual.split(",")[1]
                            : qr1RegeneradoBase64Actual;
                    byte[] bytes = Base64.getDecoder().decode(base64Limpia);
                    imgQr1Regenerado.setImage(new Image(new ByteArrayInputStream(bytes)));
                    panelQr1Regenerado.setVisible(true);
                    panelQr1Regenerado.setManaged(true);
                } catch (Exception ex) {
                    LOGGER.warning("Error al decodificar QR1 regenerado: " + ex.getMessage());
                }
            }

            cargarHistorialIncidencias(encomiendaSeleccionadaContingencia.idEncomienda());
            NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Incidencia Registrada", "QR1 Regenerado",
                    "Incidencia registrada y nuevo código QR1 operativo generado exitosamente.");
        });

        tarea.setOnFailed(e -> {
            btnConfirmarIncidencia.setDisable(false);
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Incidencia", "Error al reportar",
                    obtenerMensajeErrorClaro(tarea.getException()));
        });

        Thread hilo = new Thread(tarea, "hilo-reportar-incidencia");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cargarHistorialIncidencias(String idEncomienda) {
        Task<List<IncidenciaHistorialDto>> tarea = new Task<>() {
            @Override
            protected List<IncidenciaHistorialDto> call() {
                return almacenPort.listarIncidencias(idEncomienda);
            }
        };

        tarea.setOnSucceeded(e -> listaHistorialIncidencias.setAll(tarea.getValue()));
        tarea.setOnFailed(e -> LOGGER.warning("Error al cargar historial incidencias: " + obtenerMensajeErrorClaro(tarea.getException())));

        Thread hilo = new Thread(tarea, "hilo-historial-incidencias");
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    public void imprimirQr1Regenerado() {
        if (encomiendaSeleccionadaContingencia == null) return;

        String ticket = """
                ================================
                      EXPRESO MARVISUR S.A.
                   REIMPRESIÓN ETIQUETA QR1
                ================================
                COMPROBANTE: %s
                FECHA:       %s
                REMITENTE:   %s
                DESTINATARIO:%s
                DESTINO:     %s
                ================================
                  ETIQUETA OPERATIVA REGENERADA
                ================================
                """.formatted(
                encomiendaSeleccionadaContingencia.codigoComprobante(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                encomiendaSeleccionadaContingencia.nombreRemitente(),
                encomiendaSeleccionadaContingencia.nombreDestinatario(),
                encomiendaSeleccionadaContingencia.idSucursalDestino()
        );

        impresionPort.imprimirTicket(ticket);
        NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Impresión Térmica", "Etiqueta Generada",
                "Se generó la nueva etiqueta física en salida/.");
    }

    public void seleccionarPestana(String pestana) {
        if (tabPaneAlmacen == null) return;
        if ("CONTINGENCIA".equalsIgnoreCase(pestana)) {
            if (tabPaneAlmacen.getTabs().contains(tabContingencia)) {
                tabPaneAlmacen.getSelectionModel().select(tabContingencia);
            }
        } else if ("LOTES".equalsIgnoreCase(pestana)) {
            if (tabPaneAlmacen.getTabs().contains(tabLotes)) {
                tabPaneAlmacen.getSelectionModel().select(tabLotes);
            }
        } else {
            if (tabPaneAlmacen.getTabs().contains(tabEntrega)) {
                tabPaneAlmacen.getSelectionModel().select(tabEntrega);
            }
        }
    }
}
