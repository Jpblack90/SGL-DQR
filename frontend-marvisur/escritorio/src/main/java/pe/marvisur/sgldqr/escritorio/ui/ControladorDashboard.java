package pe.marvisur.sgldqr.escritorio.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.*;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.DashboardPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.VentanillaPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador de la pestaña Dashboard Administrativo (RF-02) en JavaFX.
 * Muestra KPIs de volumen, tiempos, recaudación y auditoría Doble QR (RF-04, RNF-03).
 */
public class ControladorDashboard {

    private static final Logger LOGGER = Logger.getLogger(ControladorDashboard.class.getName());

    public record ItemSucursalFiltro(String idSucursal, String nombreMostrar) {
        @Override
        public String toString() {
            return nombreMostrar;
        }
    }

    // --- Controles de Barra Superior ---
    @FXML
    private ComboBox<PeriodoDashboard> cboPeriodo;

    @FXML
    private ComboBox<ItemSucursalFiltro> cboSucursal;

    @FXML
    private Button btnActualizar;

    @FXML
    private ProgressIndicator spinnerCarga;

    @FXML
    private Label lblUltimaActualizacion;

    @FXML
    private HBox bannerEstado;

    @FXML
    private Label lblBannerEstado;

    // --- Tarjetas KPI ---
    @FXML
    private Label lblKpiRegistradas;

    @FXML
    private Label lblKpiEntregadas;

    @FXML
    private Label lblKpiEnRuta;

    @FXML
    private Label lblKpiPendientesCantidad;

    @FXML
    private Label lblKpiPendientesMonto;

    @FXML
    private Label lblKpiRecaudado;

    // --- Gráfico de Estados ---
    @FXML
    private BarChart<String, Number> chartEstados;

    @FXML
    private CategoryAxis ejeEstados;

    @FXML
    private NumberAxis ejeCantidades;

    // --- Tabla Recaudación ---
    @FXML
    private TableView<RecaudacionSucursalDto> tblRecaudacion;

    @FXML
    private TableColumn<RecaudacionSucursalDto, String> colAgenciaNombre;

    @FXML
    private TableColumn<RecaudacionSucursalDto, String> colAgenciaRecaudado;

    @FXML
    private TableColumn<RecaudacionSucursalDto, String> colAgenciaPendiente;

    // --- Panel Validaciones Doble QR ---
    @FXML
    private Label lblValTotal;

    @FXML
    private Label lblValExitosas;

    @FXML
    private Label lblValRechazadasPago;

    @FXML
    private Label lblValRechazadasToken;

    @FXML
    private Label lblLatenciaPromedio;

    @FXML
    private Label lblLatenciaMaxima;

    // --- Tabla Estancados ---
    @FXML
    private Label lblUmbralHoras;

    @FXML
    private TableView<EncomiendaEstancadaDto> tblEstancados;

    @FXML
    private TableColumn<EncomiendaEstancadaDto, String> colEstancadoComprobante;

    @FXML
    private TableColumn<EncomiendaEstancadaDto, String> colEstancadoEstado;

    @FXML
    private TableColumn<EncomiendaEstancadaDto, String> colEstancadoDestino;

    @FXML
    private TableColumn<EncomiendaEstancadaDto, String> colEstancadoHoras;

    // --- Dependencias y Estado ---
    private final DashboardPort dashboardPort;
    private final VentanillaPort ventanillaPort;
    private final ServicioConexion servicioConexion;
    private final PoliticaActualizacionDashboard politicaActualizacion;

    private ScheduledExecutorService planificadorRefresco;
    private DashboardResumenDto ultimoResumen;

    public ControladorDashboard() {
        this(new MarvisurHttpClient(), new MarvisurHttpClient(), ServicioConexion.getInstancia());
    }

    public ControladorDashboard(DashboardPort dashboardPort, VentanillaPort ventanillaPort, ServicioConexion servicioConexion) {
        this.dashboardPort = dashboardPort;
        this.ventanillaPort = ventanillaPort;
        this.servicioConexion = servicioConexion;
        this.politicaActualizacion = new PoliticaActualizacionDashboard(false);
    }

    @FXML
    public void initialize() {
        configurarFiltros();
        configurarTablas();
    }

    private void configurarFiltros() {
        cboPeriodo.getItems().setAll(PeriodoDashboard.values());
        cboPeriodo.setValue(PeriodoDashboard.HOY);

        cboSucursal.getItems().add(new ItemSucursalFiltro(null, "Todas las agencias"));
        cboSucursal.getSelectionModel().selectFirst();

        // Cargar agencias desde el servidor en segundo plano
        Thread hiloSucursales = new Thread(() -> {
            try {
                List<SucursalDto> sucursales = ventanillaPort.listarSucursales();
                if (sucursales != null && !sucursales.isEmpty()) {
                    Platform.runLater(() -> {
                        for (SucursalDto s : sucursales) {
                            cboSucursal.getItems().add(new ItemSucursalFiltro(s.id(), s.nombreAgencia() + " (" + s.ciudad() + ")"));
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "No se pudo cargar lista de agencias para filtro: " + e.getMessage());
            }
        }, "hilo-filtro-sucursales-dashboard");
        hiloSucursales.setDaemon(true);
        hiloSucursales.start();

        cboPeriodo.setOnAction(e -> {
            if (politicaActualizacion.isActivo()) {
                dispararCargaAsincrona();
            }
        });

        cboSucursal.setOnAction(e -> {
            if (politicaActualizacion.isActivo()) {
                dispararCargaAsincrona();
            }
        });
    }

    private void configurarTablas() {
        // Tabla Recaudación
        colAgenciaNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nombreAgencia()));
        colAgenciaRecaudado.setCellValueFactory(data ->
                new SimpleStringProperty(DashboardViewModel.formatearMonto(data.getValue().recaudado())));
        colAgenciaPendiente.setCellValueFactory(data ->
                new SimpleStringProperty(DashboardViewModel.formatearMonto(data.getValue().pendiente())));

        // Tabla Estancados
        colEstancadoComprobante.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().codigoComprobante()));
        colEstancadoEstado.setCellValueFactory(data ->
                new SimpleStringProperty(DashboardViewModel.etiquetaEstadoLogistico(data.getValue().estadoLogistico())));
        colEstancadoDestino.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nombreAgenciaDestino()));
        colEstancadoHoras.setCellValueFactory(data ->
                new SimpleStringProperty(DashboardViewModel.formatearHoras(data.getValue().horasSinMovimiento())));
    }

    /**
     * Activa el ciclo de vida y sondeo periódico de 30s mientras la pestaña esté visible.
     */
    public void activar() {
        politicaActualizacion.activar();

        detenerPlanificador();
        planificadorRefresco = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hilo-refresco-dashboard");
            t.setDaemon(true);
            return t;
        });

        // Tarea periódica de refresco cada 30 segundos
        planificadorRefresco.scheduleWithFixedDelay(
                this::dispararCargaAsincrona,
                PoliticaActualizacionDashboard.INTERVALO_REFRESCO_SEGUNDOS,
                PoliticaActualizacionDashboard.INTERVALO_REFRESCO_SEGUNDOS,
                TimeUnit.SECONDS
        );

        // Disparo inicial inmediato
        dispararCargaAsincrona();
    }

    /**
     * Detiene el ciclo de vida al cambiar de pestaña, cerrar sesión o salir.
     */
    public void desactivar() {
        politicaActualizacion.desactivar();
        detenerPlanificador();
    }

    private void detenerPlanificador() {
        if (planificadorRefresco != null && !planificadorRefresco.isShutdown()) {
            planificadorRefresco.shutdownNow();
            planificadorRefresco = null;
        }
    }

    @FXML
    public void manejarActualizarManual() {
        dispararCargaAsincrona();
    }

    private void dispararCargaAsincrona() {
        if (!politicaActualizacion.intentarIniciarCarga()) {
            LOGGER.log(Level.FINE, "Actualización de dashboard omitida (carga en curso o inactiva)");
            return;
        }

        if (!servicioConexion.estaEnLinea()) {
            politicaActualizacion.finalizarCarga();
            Platform.runLater(() -> {
                bannerEstado.setVisible(true);
                bannerEstado.setManaged(true);
                lblBannerEstado.setText(DashboardViewModel.MENSAJE_SIN_CONEXION);
                if (ultimoResumen != null) {
                    lblUltimaActualizacion.setText(DashboardViewModel.formatearTextoActualizacion(
                            ultimoResumen.generadoEn(), true, DashboardViewModel.MENSAJE_SIN_CONEXION));
                }
            });
            return;
        }

        PeriodoDashboard periodo = cboPeriodo.getValue() != null ? cboPeriodo.getValue() : PeriodoDashboard.HOY;
        ItemSucursalFiltro sucursalFiltro = cboSucursal.getValue();
        String idSucursal = (sucursalFiltro != null) ? sucursalFiltro.idSucursal() : null;

        Platform.runLater(() -> {
            spinnerCarga.setVisible(true);
            spinnerCarga.setManaged(true);
            btnActualizar.setDisable(true);
        });

        Thread hilo = new Thread(() -> {
            try {
                DashboardResumenDto resultado = dashboardPort.obtenerResumenDashboard(periodo, idSucursal);
                Platform.runLater(() -> aplicarResultadoEnUI(resultado));
            } catch (Throwable error) {
                Platform.runLater(() -> aplicarErrorEnUI(error));
            } finally {
                politicaActualizacion.finalizarCarga();
                Platform.runLater(() -> {
                    spinnerCarga.setVisible(false);
                    spinnerCarga.setManaged(false);
                    btnActualizar.setDisable(false);
                });
            }
        }, "hilo-consulta-dashboard");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void aplicarResultadoEnUI(DashboardResumenDto dto) {
        if (dto == null) return;
        this.ultimoResumen = dto;

        // Ocultar banner de error
        bannerEstado.setVisible(false);
        bannerEstado.setManaged(false);

        // 1. Tarjetas KPI
        lblKpiRegistradas.setText(String.valueOf(dto.totales().registradas()));
        lblKpiEntregadas.setText(String.valueOf(dto.totales().entregadas()));
        lblKpiEnRuta.setText(String.valueOf(dto.totales().enRuta()));
        lblKpiPendientesCantidad.setText(String.valueOf(dto.totales().pendientesDePago().cantidad()));
        lblKpiPendientesMonto.setText("Monto: " + DashboardViewModel.formatearMonto(dto.totales().pendientesDePago().monto()));
        lblKpiRecaudado.setText(DashboardViewModel.formatearMonto(dto.totales().recaudado()));

        // 2. Gráfico de barras por estado
        actualizarGraficoEstados(dto.porEstadoLogistico());

        // 3. Tabla Recaudación por sucursal
        tblRecaudacion.getItems().setAll(dto.recaudacionPorSucursal() != null ? dto.recaudacionPorSucursal() : List.of());

        // 4. Panel de validaciones Doble QR
        ValidacionesDashboardDto v = dto.validaciones();
        if (v != null) {
            lblValTotal.setText(String.valueOf(v.total()));
            lblValExitosas.setText(String.valueOf(v.exitosas()));
            lblValRechazadasPago.setText(String.valueOf(v.rechazadasPagoPendiente()));
            lblValRechazadasToken.setText(String.valueOf(v.rechazadasTokenInvalido()));
            lblLatenciaPromedio.setText(DashboardViewModel.formatearLatenciaPromedio(v.latenciaPromedioMs()));
            lblLatenciaMaxima.setText(DashboardViewModel.formatearLatenciaMaxima(v.latenciaMaximaMs()));

            if (DashboardViewModel.esLatenciaCritica(v.latenciaMaximaMs())) {
                lblLatenciaMaxima.setStyle("-fx-font-weight: bold; -fx-text-fill: #F59E0B;");
            } else {
                lblLatenciaMaxima.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");
            }
        }

        // 5. Tabla de estancados
        lblUmbralHoras.setText(DashboardViewModel.textoUmbralEstancado(dto.umbralEstancadoHoras()));
        tblEstancados.getItems().setAll(dto.estancados() != null ? dto.estancados() : List.of());

        // 6. Etiqueta de última actualización
        lblUltimaActualizacion.setText(DashboardViewModel.formatearTextoActualizacion(dto.generadoEn(), false, null));
    }

    private void actualizarGraficoEstados(Map<String, Long> mapaEstados) {
        chartEstados.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();

        String[] estadosOrdenados = {
                "REGISTRADA",
                "EN_ALMACEN_ORIGEN",
                "EN_RUTA",
                "EN_ALMACEN_DESTINO",
                "ENTREGADA"
        };

        for (String cod : estadosOrdenados) {
            String etiqueta = DashboardViewModel.etiquetaEstadoLogistico(cod);
            long cantidad = (mapaEstados != null && mapaEstados.containsKey(cod)) ? mapaEstados.get(cod) : 0L;
            serie.getData().add(new XYChart.Data<>(etiqueta, cantidad));
        }

        chartEstados.getData().add(serie);
    }

    private void aplicarErrorEnUI(Throwable error) {
        String mensajeLimpio = DashboardViewModel.limpiarMensajeError(error);

        bannerEstado.setVisible(true);
        bannerEstado.setManaged(true);
        lblBannerEstado.setText(mensajeLimpio);

        if (ultimoResumen != null) {
            lblUltimaActualizacion.setText(DashboardViewModel.formatearTextoActualizacion(
                    ultimoResumen.generadoEn(), true, mensajeLimpio));
        } else {
            lblUltimaActualizacion.setText("Desactualizado · " + mensajeLimpio);
        }
    }
}
