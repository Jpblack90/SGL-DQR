package pe.marvisur.sgldqr.escritorio.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.*;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.EmpleadosPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.FlotaPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador de interfaz JavaFX para la gestión de rutas, flotas y viajes interprovinciales (RF-05).
 * Soporta vistas diferenciadas para Administrador (TN3) y Chofer (TN4).
 */
public class ControladorRutasFlota {

    private static final Logger LOGGER = Logger.getLogger(ControladorRutasFlota.class.getName());

    // =========================================================================
    // COMPONENTES INYECTADOS POR FXML
    // =========================================================================

    @FXML private Label lblTituloModulo;
    @FXML private Label lblSubtituloModulo;
    @FXML private HBox bannerEstado;
    @FXML private Label lblBannerEstado;

    @FXML private TabPane tabPaneModulo;
    @FXML private Tab tabViajes;
    @FXML private ComboBox<String> cboFiltroEstado;
    @FXML private Button btnFiltrarViajes;
    @FXML private Button btnActualizarViajes;
    @FXML private Button btnNuevoViaje;

    @FXML private TableView<ViajeDto> tablaViajes;
    @FXML private TableColumn<ViajeDto, String> colViajeRuta;
    @FXML private TableColumn<ViajeDto, String> colViajeVehiculo;
    @FXML private TableColumn<ViajeDto, String> colViajeChofer;
    @FXML private TableColumn<ViajeDto, String> colViajeSalida;
    @FXML private TableColumn<ViajeDto, String> colViajeLlegada;
    @FXML private TableColumn<ViajeDto, String> colViajeEstado;

    @FXML private Button btnIniciarViaje;
    @FXML private Button btnFinalizarViaje;
    @FXML private Button btnVerManifiesto;
    @FXML private Label lblTotalViajes;

    @FXML private Tab tabFlota;
    @FXML private Button btnActualizarVehiculos;
    @FXML private Button btnNuevoVehiculo;

    @FXML private TableView<VehiculoDto> tablaVehiculos;
    @FXML private TableColumn<VehiculoDto, String> colVehiculoPlaca;
    @FXML private TableColumn<VehiculoDto, String> colVehiculoModelo;
    @FXML private TableColumn<VehiculoDto, String> colVehiculoCapacidad;
    @FXML private Label lblTotalVehiculos;

    // =========================================================================
    // ESTADO Y DEPENDENCIAS
    // =========================================================================

    private FlotaPort flotaPort;
    private EmpleadosPort empleadosPort;
    private final ServicioConexion servicioConexion;

    private final ObservableList<ViajeDto> datosViajes = FXCollections.observableArrayList();
    private final ObservableList<VehiculoDto> datosVehiculos = FXCollections.observableArrayList();

    /**
     * Constructor canónico único.
     */
    public ControladorRutasFlota() {
        MarvisurHttpClient cliente = new MarvisurHttpClient();
        this.flotaPort = cliente;
        this.empleadosPort = cliente;
        this.servicioConexion = ServicioConexion.getInstancia();
    }

    public void setFlotaPort(FlotaPort flotaPort) {
        this.flotaPort = flotaPort;
    }

    public void setEmpleadosPort(EmpleadosPort empleadosPort) {
        this.empleadosPort = empleadosPort;
    }

    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================

    @FXML
    public void initialize() {
        configurarTablaViajes();
        configurarTablaVehiculos();
        configurarFiltros();
        configurarSeguridadSegunRol();

        recargarViajes();
        String rol = obtenerRolActual();
        if (RutasFlotaViewModel.puedeGestionarFlota(rol)) {
            recargarVehiculos();
        }
    }

    private String obtenerRolActual() {
        UsuarioSesion sesion = SesionContexto.obtenerSesion();
        return (sesion != null && sesion.rol() != null) ? sesion.rol() : "";
    }

    private void configurarSeguridadSegunRol() {
        String rol = obtenerRolActual();
        String rolNorm = ModeloMenuRol.normalizarRol(rol);

        if ("TN4".equalsIgnoreCase(rolNorm)) {
            if (lblTituloModulo != null) {
                lblTituloModulo.setText("Mis Viajes Asignados");
            }
            if (lblSubtituloModulo != null) {
                lblSubtituloModulo.setText("Despacho, inicio y finalización de viajes interprovinciales asignados a mi unidad (RF-05).");
            }
            if (btnNuevoViaje != null) {
                btnNuevoViaje.setVisible(false);
                btnNuevoViaje.setManaged(false);
            }
            if (tabPaneModulo != null && tabFlota != null) {
                tabPaneModulo.getTabs().remove(tabFlota);
            }
        }
    }

    private void configurarFiltros() {
        if (cboFiltroEstado != null) {
            cboFiltroEstado.setItems(FXCollections.observableArrayList(RutasFlotaViewModel.FILTROS_ESTADO_ETIQUETAS));
            cboFiltroEstado.getSelectionModel().selectFirst();
        }
    }

    private void configurarTablaViajes() {
        if (tablaViajes == null) return;

        tablaViajes.setItems(datosViajes);

        colViajeRuta.setCellValueFactory(cell -> {
            ViajeDto v = cell.getValue();
            if (v == null) return new SimpleStringProperty("");
            if (v.ruta() != null) {
                return new SimpleStringProperty(RutasFlotaViewModel.formatearRutaCombo(v.ruta()));
            }
            String ori = v.idSucursalOrigen() != null ? v.idSucursalOrigen() : "";
            String des = v.idSucursalDestino() != null ? v.idSucursalDestino() : "";
            return new SimpleStringProperty(ori + " ➔ " + des);
        });

        colViajeVehiculo.setCellValueFactory(cell -> {
            ViajeDto v = cell.getValue();
            if (v == null) return new SimpleStringProperty("");
            String placa = v.placa() != null ? v.placa() : v.idVehiculo();
            if (v.modelo() != null && !v.modelo().isBlank()) {
                placa += " (" + v.modelo().trim() + ")";
            }
            return new SimpleStringProperty(placa);
        });

        colViajeChofer.setCellValueFactory(cell -> {
            ViajeDto v = cell.getValue();
            if (v == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(v.nombreChofer() != null ? v.nombreChofer() : v.idChofer());
        });

        colViajeSalida.setCellValueFactory(cell -> {
            ViajeDto v = cell.getValue();
            if (v == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(RutasFlotaViewModel.formatearFechaHoraTexto(v.fechaSalida()));
        });

        colViajeLlegada.setCellValueFactory(cell -> {
            ViajeDto v = cell.getValue();
            if (v == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(RutasFlotaViewModel.formatearFechaHoraTexto(v.fechaLlegadaEstimada()));
        });

        colViajeEstado.setCellValueFactory(cell -> {
            ViajeDto v = cell.getValue();
            if (v == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(RutasFlotaViewModel.etiquetaEstadoViaje(v.estadoViaje()));
        });

        tablaViajes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, seleccionado) ->
                actualizarEstadoBotonesViaje(seleccionado));
    }

    private void actualizarEstadoBotonesViaje(ViajeDto seleccionado) {
        if (btnIniciarViaje != null) {
            btnIniciarViaje.setDisable(!RutasFlotaViewModel.puedeIniciarViaje(seleccionado));
        }
        if (btnFinalizarViaje != null) {
            btnFinalizarViaje.setDisable(!RutasFlotaViewModel.puedeFinalizarViaje(seleccionado));
        }
        if (btnVerManifiesto != null) {
            btnVerManifiesto.setDisable(!RutasFlotaViewModel.puedeVerManifiesto(seleccionado));
        }
    }

    private void configurarTablaVehiculos() {
        if (tablaVehiculos == null) return;

        tablaVehiculos.setItems(datosVehiculos);

        colVehiculoPlaca.setCellValueFactory(cell -> {
            VehiculoDto v = cell.getValue();
            return new SimpleStringProperty(v != null ? v.placa() : "");
        });

        colVehiculoModelo.setCellValueFactory(cell -> {
            VehiculoDto v = cell.getValue();
            return new SimpleStringProperty(v != null ? v.modelo() : "");
        });

        colVehiculoCapacidad.setCellValueFactory(cell -> {
            VehiculoDto v = cell.getValue();
            if (v == null || v.capacidadCargaKg() == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(String.format("%.2f kg", v.capacidadCargaKg()));
        });
    }

    // =========================================================================
    // CARGA DE DATOS ASÍNCRONA
    // =========================================================================

    @FXML
    public void filtrarViajes() {
        cargarViajes();
    }

    @FXML
    public void recargarViajes() {
        cargarViajes();
    }

    private void cargarViajes() {
        ocultarBanner();
        if (!servicioConexion.estaEnLinea()) {
            mostrarBannerError("Requiere conexión: no se puede consultar viajes sin conexión central.");
            return;
        }

        String etiquetaFiltro = (cboFiltroEstado != null) ? cboFiltroEstado.getValue() : "Todos los estados";
        String estadoApi = RutasFlotaViewModel.mapearFiltroEstadoApi(etiquetaFiltro);

        Thread t = new Thread(() -> {
            try {
                List<ViajeDto> lista = flotaPort.listarViajes(estadoApi, null, null);
                Platform.runLater(() -> {
                    datosViajes.setAll(lista != null ? lista : List.of());
                    if (lblTotalViajes != null) {
                        lblTotalViajes.setText("Total viajes: " + datosViajes.size());
                    }
                    actualizarEstadoBotonesViaje(tablaViajes.getSelectionModel().getSelectedItem());
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al listar viajes: " + e.getMessage());
                Platform.runLater(() -> mostrarBannerError(RutasFlotaViewModel.limpiarMensajeError(e)));
            }
        }, "hilo-listar-viajes");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void recargarVehiculos() {
        ocultarBanner();
        if (!servicioConexion.estaEnLinea()) {
            mostrarBannerError("Requiere conexión: no se puede consultar la flota sin conexión central.");
            return;
        }

        Thread t = new Thread(() -> {
            try {
                List<VehiculoDto> lista = flotaPort.listarVehiculos();
                Platform.runLater(() -> {
                    datosVehiculos.setAll(lista != null ? lista : List.of());
                    if (lblTotalVehiculos != null) {
                        lblTotalVehiculos.setText("Total vehículos: " + datosVehiculos.size());
                    }
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al listar vehículos: " + e.getMessage());
                Platform.runLater(() -> mostrarBannerError(RutasFlotaViewModel.limpiarMensajeError(e)));
            }
        }, "hilo-listar-vehiculos");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // ACCIONES DE ESTADO DE VIAJE
    // =========================================================================

    @FXML
    public void confirmarIniciarViaje() {
        ViajeDto sel = tablaViajes.getSelectionModel().getSelectedItem();
        if (sel == null || !RutasFlotaViewModel.puedeIniciarViaje(sel)) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(NavegadorUI.getStagePrincipal());
        confirm.setTitle("Iniciar Viaje Interprovincial");
        confirm.setHeaderText("¿Está seguro de iniciar el viaje seleccionado?");
        confirm.setContentText("El estado pasará a EN RUTA.\nVehículo: " + (sel.placa() != null ? sel.placa() : sel.idVehiculo()));

        Optional<ButtonType> respuesta = confirm.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            ejecutarCambioEstadoViaje(sel.idViaje(), "EN_RUTA", "Viaje iniciado exitosamente.");
        }
    }

    @FXML
    public void confirmarFinalizarViaje() {
        ViajeDto sel = tablaViajes.getSelectionModel().getSelectedItem();
        if (sel == null || !RutasFlotaViewModel.puedeFinalizarViaje(sel)) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(NavegadorUI.getStagePrincipal());
        confirm.setTitle("Finalizar Viaje Interprovincial");
        confirm.setHeaderText("¿Está seguro de finalizar el viaje seleccionado?");
        confirm.setContentText("El estado pasará a FINALIZADO y la unidad quedará liberada.\nVehículo: " + (sel.placa() != null ? sel.placa() : sel.idVehiculo()));

        Optional<ButtonType> respuesta = confirm.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            ejecutarCambioEstadoViaje(sel.idViaje(), "FINALIZADO", "Viaje finalizado exitosamente.");
        }
    }

    private void ejecutarCambioEstadoViaje(String idViaje, String nuevoEstado, String mensajeExito) {
        if (!servicioConexion.estaEnLinea()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Sin Conexión", "Requiere conexión", "No se puede cambiar el estado del viaje sin conexión.");
            return;
        }

        Thread t = new Thread(() -> {
            try {
                flotaPort.cambiarEstadoViaje(idViaje, new CambiarEstadoViajeRequest(nuevoEstado));
                Platform.runLater(() -> {
                    NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Operación Exitosa", "Estado Actualizado", mensajeExito);
                    cargarViajes();
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al actualizar estado del viaje: " + e.getMessage());
                Platform.runLater(() -> NavegadorUI.mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Acción no permitida",
                        "No se pudo actualizar el estado",
                        RutasFlotaViewModel.limpiarMensajeError(e)
                ));
            }
        }, "hilo-cambiar-estado-viaje");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // DIÁLOGO MANIFIESTO DE CARGA (CERO DATOS PERSONALES)
    // =========================================================================

    @FXML
    public void abrirDialogoManifiesto() {
        ViajeDto sel = tablaViajes.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        if (!servicioConexion.estaEnLinea()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Sin Conexión", "Requiere conexión", "No se puede consultar el manifiesto sin conexión.");
            return;
        }

        Thread t = new Thread(() -> {
            try {
                List<ManifiestoEncomiendaDto> items = flotaPort.obtenerManifiesto(sel.idViaje());
                Platform.runLater(() -> mostrarVentanaManifiesto(sel, items != null ? items : List.of()));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al consultar manifiesto: " + e.getMessage());
                Platform.runLater(() -> NavegadorUI.mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error de Consulta",
                        "No se pudo obtener el manifiesto",
                        RutasFlotaViewModel.limpiarMensajeError(e)
                ));
            }
        }, "hilo-consultar-manifiesto");
        t.setDaemon(true);
        t.start();
    }

    private void mostrarVentanaManifiesto(ViajeDto viaje, List<ManifiestoEncomiendaDto> encomiendas) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(NavegadorUI.getStagePrincipal());
        dialog.setTitle("Manifiesto de Carga");
        dialog.setHeaderText("Viaje: " + (viaje.placa() != null ? viaje.placa() : viaje.idViaje()) + " — Cero Datos Personales");

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));
        content.setPrefWidth(650);

        TableView<ManifiestoEncomiendaDto> tabla = new TableView<>();
        tabla.setItems(FXCollections.observableArrayList(encomiendas));

        TableColumn<ManifiestoEncomiendaDto, String> colCod = new TableColumn<>("Comprobante");
        colCod.setPrefWidth(190);
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().codigoComprobante()));

        TableColumn<ManifiestoEncomiendaDto, String> colEst = new TableColumn<>("Estado Logístico");
        colEst.setPrefWidth(130);
        colEst.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estadoLogistico()));

        TableColumn<ManifiestoEncomiendaDto, String> colDest = new TableColumn<>("Agencia Destino");
        colDest.setPrefWidth(200);
        colDest.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombreAgenciaDestino()));

        TableColumn<ManifiestoEncomiendaDto, String> colPeso = new TableColumn<>("Peso (kg)");
        colPeso.setPrefWidth(100);
        colPeso.setStyle("-fx-alignment: CENTER-RIGHT;");
        colPeso.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().pesoKg() != null ? String.format("%.2f kg", c.getValue().pesoKg()) : "0.00 kg"));

        tabla.getColumns().addAll(colCod, colEst, colDest, colPeso);

        Label lblTotales = new Label(RutasFlotaViewModel.textoResumenManifiesto(encomiendas));
        lblTotales.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0F172A;");

        content.getChildren().addAll(tabla, lblTotales);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // =========================================================================
    // DIÁLOGO NUEVO VEHÍCULO
    // =========================================================================

    @FXML
    public void abrirDialogoNuevoVehiculo() {
        Dialog<CrearVehiculoRequest> dialog = new Dialog<>();
        dialog.initOwner(NavegadorUI.getStagePrincipal());
        dialog.setTitle("Registrar Unidad Vehicular");
        dialog.setHeaderText("Nueva unidad de transporte para la flota (RF-05)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField txtPlaca = new TextField();
        txtPlaca.setPromptText("Ej. ABC-123");
        TextField txtModelo = new TextField();
        txtModelo.setPromptText("Ej. Volvo FH16");
        TextField txtCapacidad = new TextField();
        txtCapacidad.setPromptText("Ej. 25000.00");

        grid.add(new Label("Placa de Rodaje:"), 0, 0);
        grid.add(txtPlaca, 1, 0);
        grid.add(new Label("Modelo / Descripción:"), 0, 1);
        grid.add(txtModelo, 1, 1);
        grid.add(new Label("Capacidad Carga (kg):"), 0, 2);
        grid.add(txtCapacidad, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node btnOk = dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String placa = txtPlaca.getText() != null ? txtPlaca.getText().trim().toUpperCase() : "";
            String modelo = txtModelo.getText() != null ? txtModelo.getText().trim() : "";
            BigDecimal capacidad = null;
            try {
                if (txtCapacidad.getText() != null && !txtCapacidad.getText().isBlank()) {
                    capacidad = new BigDecimal(txtCapacidad.getText().trim());
                }
            } catch (NumberFormatException nfe) {
                // será atrapado por el validador
            }

            List<String> errores = RutasFlotaViewModel.validarVehiculo(placa, modelo, capacidad);
            if (!errores.isEmpty()) {
                event.consume();
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Validación de Formulario", "Datos incompletos o inválidos", errores.get(0));
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String placa = txtPlaca.getText().trim().toUpperCase();
                String modelo = txtModelo.getText().trim();
                BigDecimal cap = new BigDecimal(txtCapacidad.getText().trim());
                return new CrearVehiculoRequest(placa, modelo, cap);
            }
            return null;
        });

        Optional<CrearVehiculoRequest> resultado = dialog.showAndWait();
        resultado.ifPresent(req -> {
            if (!servicioConexion.estaEnLinea()) {
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Sin Conexión", "Requiere conexión", "No se puede registrar vehículo sin conexión.");
                return;
            }

            Thread t = new Thread(() -> {
                try {
                    flotaPort.registrarVehiculo(req);
                    Platform.runLater(() -> {
                        NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Registro Exitoso", "Vehículo Registrado", "La unidad vehicular ha sido incorporada a la flota.");
                        recargarVehiculos();
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al registrar vehículo: " + e.getMessage());
                    Platform.runLater(() -> NavegadorUI.mostrarAlerta(
                            Alert.AlertType.ERROR,
                            "Error de Registro",
                            "No se pudo registrar el vehículo",
                            RutasFlotaViewModel.limpiarMensajeError(e)
                    ));
                }
            }, "hilo-registrar-vehiculo");
            t.setDaemon(true);
            t.start();
        });
    }

    // =========================================================================
    // DIÁLOGO NUEVO VIAJE
    // =========================================================================

    @FXML
    public void abrirDialogoNuevoViaje() {
        if (!servicioConexion.estaEnLinea()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Sin Conexión", "Requiere conexión", "No se puede programar viaje sin conexión.");
            return;
        }

        Thread t = new Thread(() -> {
            try {
                List<RutaDto> rutas = flotaPort.listarRutas();
                List<VehiculoDto> vehiculos = flotaPort.listarVehiculos();
                List<EmpleadoDto> choferes = cargarChoferesActivos();

                Platform.runLater(() -> mostrarFormularioNuevoViaje(rutas, vehiculos, choferes));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al preparar catálogo para viaje: " + e.getMessage());
                Platform.runLater(() -> NavegadorUI.mostrarAlerta(
                        Alert.AlertType.ERROR,
                        "Error de Catálogo",
                        "No se pudo cargar la información necesaria",
                        RutasFlotaViewModel.limpiarMensajeError(e)
                ));
            }
        }, "hilo-preparar-nuevo-viaje");
        t.setDaemon(true);
        t.start();
    }

    private List<EmpleadoDto> cargarChoferesActivos() {
        List<RolComboDto> roles = empleadosPort.listarRoles();
        String idRolChofer = null;
        if (roles != null) {
            for (RolComboDto r : roles) {
                if (r.id() != null && r.id().toUpperCase().contains("CHOFER")) {
                    idRolChofer = r.id();
                    break;
                }
                if (r.nombre() != null && r.nombre().toUpperCase().contains("CHOFER")) {
                    idRolChofer = r.id();
                    break;
                }
            }
        }

        PaginaDto<EmpleadoDto> pag = empleadosPort.listarEmpleados("ACTIVOS", idRolChofer, null, null, 0, 100);
        if (pag != null && pag.content() != null) {
            return pag.content().stream()
                    .filter(e -> Boolean.TRUE.equals(e.estado()))
                    .toList();
        }
        return List.of();
    }

    private void mostrarFormularioNuevoViaje(List<RutaDto> rutas, List<VehiculoDto> vehiculos, List<EmpleadoDto> choferes) {
        Dialog<CrearViajeRequest> dialog = new Dialog<>();
        dialog.initOwner(NavegadorUI.getStagePrincipal());
        dialog.setTitle("Programar Nuevo Viaje");
        dialog.setHeaderText("Asignación de ruta, unidad vehicular y chofer calificado (RF-05)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        ComboBox<RutaDto> cboRuta = new ComboBox<>(FXCollections.observableArrayList(rutas != null ? rutas : List.of()));
        configurarComboRuta(cboRuta);

        ComboBox<VehiculoDto> cboVehiculo = new ComboBox<>(FXCollections.observableArrayList(vehiculos != null ? vehiculos : List.of()));
        configurarComboVehiculo(cboVehiculo);

        ComboBox<EmpleadoDto> cboChofer = new ComboBox<>(FXCollections.observableArrayList(choferes != null ? choferes : List.of()));
        configurarComboChofer(cboChofer);

        DatePicker dpSalida = new DatePicker(LocalDate.now());
        Spinner<Integer> spnHoraSalida = new Spinner<>(0, 23, 20);
        spnHoraSalida.setPrefWidth(70);
        Spinner<Integer> spnMinSalida = new Spinner<>(0, 59, 0);
        spnMinSalida.setPrefWidth(70);
        HBox boxSalida = new HBox(6, dpSalida, new Label("Hora:"), spnHoraSalida, new Label(":"), spnMinSalida);

        DatePicker dpLlegada = new DatePicker(LocalDate.now().plusDays(1));
        Spinner<Integer> spnHoraLlegada = new Spinner<>(0, 23, 8);
        spnHoraLlegada.setPrefWidth(70);
        Spinner<Integer> spnMinLlegada = new Spinner<>(0, 59, 0);
        spnMinLlegada.setPrefWidth(70);
        HBox boxLlegada = new HBox(6, dpLlegada, new Label("Hora:"), spnHoraLlegada, new Label(":"), spnMinLlegada);

        grid.add(new Label("Ruta:"), 0, 0);
        grid.add(cboRuta, 1, 0);
        grid.add(new Label("Vehículo:"), 0, 1);
        grid.add(cboVehiculo, 1, 1);
        grid.add(new Label("Chofer:"), 0, 2);
        grid.add(cboChofer, 1, 2);
        grid.add(new Label("Salida:"), 0, 3);
        grid.add(boxSalida, 1, 3);
        grid.add(new Label("Llegada Estimada:"), 0, 4);
        grid.add(boxLlegada, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node btnOk = dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            RutaDto r = cboRuta.getValue();
            VehiculoDto v = cboVehiculo.getValue();
            EmpleadoDto c = cboChofer.getValue();
            LocalDate fs = dpSalida.getValue();
            int hs = spnHoraSalida.getValue();
            int ms = spnMinSalida.getValue();
            LocalDate fl = dpLlegada.getValue();
            int hl = spnHoraLlegada.getValue();
            int ml = spnMinLlegada.getValue();

            List<String> errores = RutasFlotaViewModel.validarViaje(r, v, c, fs, hs, ms, fl, hl, ml);
            if (!errores.isEmpty()) {
                event.consume();
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Validación de Viaje", "Datos incompletos o inconsistentes", errores.get(0));
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                RutaDto r = cboRuta.getValue();
                VehiculoDto v = cboVehiculo.getValue();
                EmpleadoDto c = cboChofer.getValue();
                OffsetDateTime salida = RutasFlotaViewModel.construirOffsetDateTimeLima(
                        dpSalida.getValue(), spnHoraSalida.getValue(), spnMinSalida.getValue());
                OffsetDateTime llegada = RutasFlotaViewModel.construirOffsetDateTimeLima(
                        dpLlegada.getValue(), spnHoraLlegada.getValue(), spnMinLlegada.getValue());

                return new CrearViajeRequest(
                        r.id(),
                        v.idVehiculo(),
                        c.idEmpleado(),
                        RutasFlotaViewModel.formatearFechaHoraIso(salida),
                        RutasFlotaViewModel.formatearFechaHoraIso(llegada)
                );
            }
            return null;
        });

        Optional<CrearViajeRequest> resultado = dialog.showAndWait();
        resultado.ifPresent(req -> {
            Thread tCrear = new Thread(() -> {
                try {
                    flotaPort.crearViaje(req);
                    Platform.runLater(() -> {
                        NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Viaje Programado", "Operación Exitosa", "El viaje ha sido programado con estado inicial PROGRAMADO.");
                        cargarViajes();
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al programar viaje: " + e.getMessage());
                    Platform.runLater(() -> NavegadorUI.mostrarAlerta(
                            Alert.AlertType.ERROR,
                            "Error al Programar",
                            "No se pudo programar el viaje",
                            RutasFlotaViewModel.limpiarMensajeError(e)
                    ));
                }
            }, "hilo-crear-viaje");
            tCrear.setDaemon(true);
            tCrear.start();
        });
    }

    private void configurarComboRuta(ComboBox<RutaDto> combo) {
        StringConverter<RutaDto> converter = new StringConverter<>() {
            @Override
            public String toString(RutaDto r) {
                return RutasFlotaViewModel.formatearRutaCombo(r);
            }
            @Override
            public RutaDto fromString(String string) {
                return null;
            }
        };
        combo.setConverter(converter);
        Callback<ListView<RutaDto>, ListCell<RutaDto>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(RutaDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : RutasFlotaViewModel.formatearRutaCombo(item));
            }
        };
        combo.setCellFactory(cellFactory);
        combo.setButtonCell(cellFactory.call(null));
    }

    private void configurarComboVehiculo(ComboBox<VehiculoDto> combo) {
        StringConverter<VehiculoDto> converter = new StringConverter<>() {
            @Override
            public String toString(VehiculoDto v) {
                return RutasFlotaViewModel.formatearVehiculoCombo(v);
            }
            @Override
            public VehiculoDto fromString(String string) {
                return null;
            }
        };
        combo.setConverter(converter);
        Callback<ListView<VehiculoDto>, ListCell<VehiculoDto>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(VehiculoDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : RutasFlotaViewModel.formatearVehiculoCombo(item));
            }
        };
        combo.setCellFactory(cellFactory);
        combo.setButtonCell(cellFactory.call(null));
    }

    private void configurarComboChofer(ComboBox<EmpleadoDto> combo) {
        StringConverter<EmpleadoDto> converter = new StringConverter<>() {
            @Override
            public String toString(EmpleadoDto e) {
                return RutasFlotaViewModel.formatearChoferCombo(e);
            }
            @Override
            public EmpleadoDto fromString(String string) {
                return null;
            }
        };
        combo.setConverter(converter);
        Callback<ListView<EmpleadoDto>, ListCell<EmpleadoDto>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(EmpleadoDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : RutasFlotaViewModel.formatearChoferCombo(item));
            }
        };
        combo.setCellFactory(cellFactory);
        combo.setButtonCell(cellFactory.call(null));
    }

    // =========================================================================
    // CONTROL DEL BANNER DE MENSAJES
    // =========================================================================

    private void mostrarBannerError(String mensaje) {
        if (bannerEstado != null) {
            bannerEstado.setVisible(true);
            bannerEstado.setManaged(true);
        }
        if (lblBannerEstado != null) {
            lblBannerEstado.setText(mensaje != null ? mensaje : "Error en el módulo de rutas y flota");
        }
    }

    private void ocultarBanner() {
        if (bannerEstado != null) {
            bannerEstado.setVisible(false);
            bannerEstado.setManaged(false);
        }
        if (lblBannerEstado != null) {
            lblBannerEstado.setText("");
        }
    }
}
