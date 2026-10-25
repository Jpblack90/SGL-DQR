package pe.marvisur.sgldqr.escritorio.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ActualizarEmpleadoRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CambiarEstadoEmpleadoRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CrearEmpleadoRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EmpleadoDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EmpleadosViewModel;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ItemAuditoriaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PaginaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RestablecerPasswordRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RolComboDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.SucursalDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.AuditoriaPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.EmpleadosPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.VentanillaPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para la gestión de empleados y consulta de bitácora de auditoría (RF-03).
 * Cumple con la regla estricta de tener UN SOLO constructor.
 */
public class ControladorEmpleados {

    private static final Logger LOGGER = Logger.getLogger(ControladorEmpleados.class.getName());
    private static final int TAMANO_PAGINA_EMPLEADOS = 20;
    private static final int TAMANO_PAGINA_AUDITORIA = 50;

    private final EmpleadosPort empleadosPort;
    private final AuditoriaPort auditoriaPort;
    private final VentanillaPort ventanillaPort;
    private final ServicioConexion servicioConexion;

    // Elementos generales
    @FXML private HBox bannerEstado;
    @FXML private Label lblBannerEstado;
    @FXML private TabPane tabPaneModulo;

    // Pestaña 1: Personal
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cboEstado;
    @FXML private ComboBox<RolComboDto> cboRol;
    @FXML private ComboBox<SucursalDto> cboSucursal;
    @FXML private Button btnBuscar;
    @FXML private Button btnLimpiarFiltros;

    @FXML private TableView<EmpleadoDto> tablaEmpleados;
    @FXML private TableColumn<EmpleadoDto, String> colDni;
    @FXML private TableColumn<EmpleadoDto, String> colNombresApellidos;
    @FXML private TableColumn<EmpleadoDto, String> colCorreo;
    @FXML private TableColumn<EmpleadoDto, String> colRol;
    @FXML private TableColumn<EmpleadoDto, String> colSucursal;
    @FXML private TableColumn<EmpleadoDto, String> colEstado;

    @FXML private Button btnNuevo;
    @FXML private Button btnEditar;
    @FXML private Button btnCambiarEstado;
    @FXML private Button btnRestablecerPassword;

    @FXML private Button btnAnterior;
    @FXML private Label lblPaginacion;
    @FXML private Button btnSiguiente;

    // Pestaña 2: Auditoría
    @FXML private ComboBox<EmpleadoDto> cboFiltroAuditoriaEmpleado;
    @FXML private ComboBox<String> cboFiltroAuditoriaAccion;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Button btnFiltrarAuditoria;
    @FXML private Button btnLimpiarAuditoria;

    @FXML private TableView<ItemAuditoriaDto> tablaAuditoria;
    @FXML private TableColumn<ItemAuditoriaDto, String> colAuditoriaFecha;
    @FXML private TableColumn<ItemAuditoriaDto, String> colAuditoriaEmpleado;
    @FXML private TableColumn<ItemAuditoriaDto, String> colAuditoriaAccion;
    @FXML private TableColumn<ItemAuditoriaDto, String> colAuditoriaTabla;
    @FXML private TableColumn<ItemAuditoriaDto, String> colAuditoriaRegistro;
    @FXML private TableColumn<ItemAuditoriaDto, String> colAuditoriaIp;

    @FXML private Button btnAuditoriaAnterior;
    @FXML private Label lblAuditoriaPaginacion;
    @FXML private Button btnAuditoriaSiguiente;

    // Estado interno de paginación
    private int paginaActualEmpleados = 0;
    private int totalPaginasEmpleados = 0;

    private int paginaActualAuditoria = 0;
    private int totalPaginasAuditoria = 0;

    private List<RolComboDto> catalogoRoles = new ArrayList<>();
    private List<SucursalDto> catalogoSucursales = new ArrayList<>();

    /**
     * Constructor canónico único.
     */
    public ControladorEmpleados() {
        MarvisurHttpClient cliente = new MarvisurHttpClient();
        this.empleadosPort = cliente;
        this.auditoriaPort = cliente;
        this.ventanillaPort = cliente;
        this.servicioConexion = ServicioConexion.getInstancia();
    }

    @FXML
    public void initialize() {
        configurarTablaEmpleados();
        configurarTablaAuditoria();
        configurarFiltros();
        configurarEventosSeleccion();

        cargarCatalogosEnSegundoPlano();
        cargarPaginaEmpleados(0);
    }

    private void configurarTablaEmpleados() {
        if (tablaEmpleados != null) {
            tablaEmpleados.setPlaceholder(new Label("Sin colaboradores para mostrar con los filtros aplicados"));
        }
        colDni.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().dni()));
        colNombresApellidos.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().nombreCompleto()));
        colCorreo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().correo()));
        colRol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().nombreRol()));
        colSucursal.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().nombreSucursal()));
        colEstado.setCellValueFactory(cell -> new SimpleStringProperty(EmpleadosViewModel.etiquetaEstado(cell.getValue().estado())));

        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Activo".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #15803D; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #64748B; -fx-alignment: CENTER;");
                    }
                }
            }
        });
    }

    private void configurarTablaAuditoria() {
        if (tablaAuditoria != null) {
            tablaAuditoria.setPlaceholder(new Label("No se encontraron registros de auditoría"));
        }
        colAuditoriaFecha.setCellValueFactory(cell -> new SimpleStringProperty(EmpleadosViewModel.formatearFechaHoraLocal(cell.getValue().fechaHora())));
        colAuditoriaEmpleado.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().nombreEmpleado() != null && !cell.getValue().nombreEmpleado().isBlank()
                        ? cell.getValue().nombreEmpleado()
                        : cell.getValue().idEmpleado()
        ));
        colAuditoriaAccion.setCellValueFactory(cell -> new SimpleStringProperty(EmpleadosViewModel.etiquetaAccionAuditoria(cell.getValue().accion())));
        colAuditoriaTabla.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().tablaAfectada()));
        colAuditoriaRegistro.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().idRegistroAfectado()));
        colAuditoriaIp.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().ipOrigen()));
    }

    private void configurarFiltros() {
        cboEstado.setItems(FXCollections.observableArrayList(EmpleadosViewModel.ESTADOS_FILTRO_ETIQUETAS));
        cboEstado.setValue("Activos");

        configurarComboRol(cboRol);
        configurarComboSucursal(cboSucursal);
        configurarComboAuditoriaEmpleado(cboFiltroAuditoriaEmpleado);

        cboFiltroAuditoriaAccion.setItems(FXCollections.observableArrayList(EmpleadosViewModel.ACCIONES_AUDITORIA));
        cboFiltroAuditoriaAccion.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String accion) {
                return EmpleadosViewModel.etiquetaAccionAuditoria(accion);
            }
            @Override
            public String fromString(String string) {
                return string;
            }
        });
        cboFiltroAuditoriaAccion.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : EmpleadosViewModel.etiquetaAccionAuditoria(item));
            }
        });
        cboFiltroAuditoriaAccion.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : EmpleadosViewModel.etiquetaAccionAuditoria(item));
            }
        });
        cboFiltroAuditoriaAccion.setValue("TODAS");

        if (tabPaneModulo != null) {
            tabPaneModulo.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null && "Bitácora de Auditoría".equals(newTab.getText())) {
                    if (tablaAuditoria.getItems().isEmpty()) {
                        cargarPaginaAuditoria(0);
                    }
                }
            });
        }
    }

    private void configurarComboRol(ComboBox<RolComboDto> combo) {
        StringConverter<RolComboDto> converter = new StringConverter<>() {
            @Override
            public String toString(RolComboDto r) {
                return EmpleadosViewModel.formatearRolCombo(r);
            }
            @Override
            public RolComboDto fromString(String string) {
                return null;
            }
        };
        combo.setConverter(converter);
        Callback<ListView<RolComboDto>, ListCell<RolComboDto>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(RolComboDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : EmpleadosViewModel.formatearRolCombo(item));
            }
        };
        combo.setCellFactory(cellFactory);
        combo.setButtonCell(cellFactory.call(null));
    }

    private void configurarComboSucursal(ComboBox<SucursalDto> combo) {
        StringConverter<SucursalDto> converter = new StringConverter<>() {
            @Override
            public String toString(SucursalDto s) {
                return EmpleadosViewModel.formatearSucursalCombo(s);
            }
            @Override
            public SucursalDto fromString(String string) {
                return null;
            }
        };
        combo.setConverter(converter);
        Callback<ListView<SucursalDto>, ListCell<SucursalDto>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(SucursalDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : EmpleadosViewModel.formatearSucursalCombo(item));
            }
        };
        combo.setCellFactory(cellFactory);
        combo.setButtonCell(cellFactory.call(null));
    }

    private void configurarComboAuditoriaEmpleado(ComboBox<EmpleadoDto> combo) {
        StringConverter<EmpleadoDto> converter = new StringConverter<>() {
            @Override
            public String toString(EmpleadoDto emp) {
                return EmpleadosViewModel.formatearColaboradorCombo(emp);
            }
            @Override
            public EmpleadoDto fromString(String string) {
                return null;
            }
        };
        combo.setConverter(converter);
        Callback<ListView<EmpleadoDto>, ListCell<EmpleadoDto>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(EmpleadoDto emp, boolean empty) {
                super.updateItem(emp, empty);
                setText(empty ? null : EmpleadosViewModel.formatearColaboradorCombo(emp));
            }
        };
        combo.setCellFactory(cellFactory);
        combo.setButtonCell(cellFactory.call(null));
    }

    private void configurarEventosSeleccion() {
        tablaEmpleados.getSelectionModel().selectedItemProperty().addListener((obs, viejo, seleccionado) -> {
            boolean haySeleccion = seleccionado != null;
            btnEditar.setDisable(!haySeleccion);
            btnCambiarEstado.setDisable(!haySeleccion);
            btnRestablecerPassword.setDisable(!haySeleccion);
            btnCambiarEstado.setText(EmpleadosViewModel.etiquetaBotonCambiarEstado(seleccionado));
        });
    }

    private void cargarCatalogosEnSegundoPlano() {
        Thread t = new Thread(() -> {
            try {
                List<RolComboDto> roles = empleadosPort.listarRoles();
                List<SucursalDto> sucursales = ventanillaPort.listarSucursales();

                Platform.runLater(() -> {
                    catalogoRoles = roles != null ? roles : List.of();
                    catalogoSucursales = sucursales != null ? sucursales : List.of();

                    List<RolComboDto> comboRolesList = new ArrayList<>();
                    comboRolesList.add(new RolComboDto(null, "Todos los roles"));
                    comboRolesList.addAll(catalogoRoles);
                    cboRol.setItems(FXCollections.observableArrayList(comboRolesList));
                    cboRol.getSelectionModel().selectFirst();

                    List<SucursalDto> comboSucList = new ArrayList<>();
                    comboSucList.add(new SucursalDto(null, "Todas las agencias", null, null));
                    comboSucList.addAll(catalogoSucursales);
                    cboSucursal.setItems(FXCollections.observableArrayList(comboSucList));
                    cboSucursal.getSelectionModel().selectFirst();
                });
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "No se pudieron precargar catálogos: " + e.getMessage());
            }
        }, "hilo-catalogos-empleados");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // ACCIONES: PERSONAL OPERATIVO
    // =========================================================================

    @FXML
    public void buscarEmpleados() {
        cargarPaginaEmpleados(0);
    }

    @FXML
    public void limpiarFiltrosEmpleados() {
        txtBuscar.clear();
        cboEstado.setValue("Activos");
        if (!cboRol.getItems().isEmpty()) cboRol.getSelectionModel().selectFirst();
        if (!cboSucursal.getItems().isEmpty()) cboSucursal.getSelectionModel().selectFirst();
        cargarPaginaEmpleados(0);
    }

    @FXML
    public void paginaAnteriorEmpleados() {
        if (paginaActualEmpleados > 0) {
            cargarPaginaEmpleados(paginaActualEmpleados - 1);
        }
    }

    @FXML
    public void paginaSiguienteEmpleados() {
        if (paginaActualEmpleados < totalPaginasEmpleados - 1) {
            cargarPaginaEmpleados(paginaActualEmpleados + 1);
        }
    }

    private void cargarPaginaEmpleados(int pagina) {
        if (!servicioConexion.estaEnLinea()) {
            mostrarBannerError("Requiere conexión: el sistema se encuentra trabajando sin conexión.");
            return;
        }

        String estado = EmpleadosViewModel.mapearEstadoApi(cboEstado.getValue());
        RolComboDto rolSel = cboRol.getValue();
        String idRol = (rolSel != null && rolSel.id() != null && !rolSel.id().isBlank()) ? rolSel.id() : null;

        SucursalDto sucSel = cboSucursal.getValue();
        String idSucursal = (sucSel != null && sucSel.id() != null && !sucSel.id().isBlank()) ? sucSel.id() : null;

        String q = txtBuscar.getText() != null ? txtBuscar.getText().trim() : null;

        ocultarBanner();

        Thread t = new Thread(() -> {
            try {
                PaginaDto<EmpleadoDto> resultado = empleadosPort.listarEmpleados(
                        estado, idRol, idSucursal, q, pagina, TAMANO_PAGINA_EMPLEADOS
                );

                Platform.runLater(() -> {
                    paginaActualEmpleados = resultado.number();
                    totalPaginasEmpleados = resultado.totalPages();

                    tablaEmpleados.setItems(FXCollections.observableArrayList(resultado.content()));
                    lblPaginacion.setText(EmpleadosViewModel.textoPaginacion(
                            paginaActualEmpleados, totalPaginasEmpleados, resultado.totalElements()
                    ));

                    btnAnterior.setDisable(resultado.first() || resultado.empty());
                    btnSiguiente.setDisable(resultado.last() || resultado.empty());

                    // Cargar combo de filtro en auditoría con los primeros 50 colaboradores
                    if (cboFiltroAuditoriaEmpleado.getItems().isEmpty() && !resultado.empty()) {
                        List<EmpleadoDto> empleadosAuditoria = new ArrayList<>();
                        empleadosAuditoria.add(null);
                        empleadosAuditoria.addAll(resultado.content());
                        cboFiltroAuditoriaEmpleado.setItems(FXCollections.observableArrayList(empleadosAuditoria));
                        cboFiltroAuditoriaEmpleado.getSelectionModel().selectFirst();
                    }
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al cargar empleados: " + e.getMessage());
                Platform.runLater(() -> mostrarBannerError(EmpleadosViewModel.limpiarMensajeError(e)));
            }
        }, "hilo-listar-empleados");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // DIÁLOGOS DE ALTA, EDICIÓN Y GESTIÓN
    // =========================================================================

    @FXML
    public void abrirDialogoNuevo() {
        Dialog<CrearEmpleadoRequest> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Colaborador");
        dialog.setHeaderText("Registro de personal operativo (RF-03)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField txtDni = new TextField();
        txtDni.setPromptText("8 dígitos");
        TextField txtNombres = new TextField();
        TextField txtApellidos = new TextField();
        TextField txtCorreo = new TextField();
        txtCorreo.setPromptText("correo@marvisur.pe");

        ComboBox<RolComboDto> cboNuevoRol = new ComboBox<>(FXCollections.observableArrayList(catalogoRoles));
        configurarComboRol(cboNuevoRol);
        ComboBox<SucursalDto> cboNuevaSucursal = new ComboBox<>(FXCollections.observableArrayList(catalogoSucursales));
        configurarComboSucursal(cboNuevaSucursal);
        PasswordField txtPassword = new PasswordField();
        PasswordField txtConfirmacion = new PasswordField();

        grid.add(new Label("DNI:"), 0, 0);
        grid.add(txtDni, 1, 0);
        grid.add(new Label("Nombres:"), 0, 1);
        grid.add(txtNombres, 1, 1);
        grid.add(new Label("Apellidos:"), 0, 2);
        grid.add(txtApellidos, 1, 2);
        grid.add(new Label("Correo:"), 0, 3);
        grid.add(txtCorreo, 1, 3);
        grid.add(new Label("Rol:"), 0, 4);
        grid.add(cboNuevoRol, 1, 4);
        grid.add(new Label("Agencia:"), 0, 5);
        grid.add(cboNuevaSucursal, 1, 5);
        grid.add(new Label("Contraseña:"), 0, 6);
        grid.add(txtPassword, 1, 6);
        grid.add(new Label("Confirmación:"), 0, 7);
        grid.add(txtConfirmacion, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node btnOk = dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String dni = txtDni.getText();
            String nombres = txtNombres.getText();
            String apellidos = txtApellidos.getText();
            String correo = txtCorreo.getText();
            String idRol = cboNuevoRol.getValue() != null ? cboNuevoRol.getValue().id() : null;
            String idSucursal = cboNuevaSucursal.getValue() != null ? cboNuevaSucursal.getValue().id() : null;
            String pass = txtPassword.getText();
            String conf = txtConfirmacion.getText();

            List<String> errores = EmpleadosViewModel.validarFormularioNuevo(dni, nombres, apellidos, correo, idRol, idSucursal, pass, conf);
            if (!errores.isEmpty()) {
                event.consume();
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Validación de Formulario", "Datos incompletos o inválidos", errores.get(0));
            }
        });

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String pass = txtPassword.getText();
                CrearEmpleadoRequest req = new CrearEmpleadoRequest(
                        txtDni.getText().trim(),
                        txtNombres.getText().trim(),
                        txtApellidos.getText().trim(),
                        txtCorreo.getText().trim().toLowerCase(),
                        cboNuevoRol.getValue().id(),
                        cboNuevaSucursal.getValue().id(),
                        pass
                );
                txtPassword.clear();
                txtConfirmacion.clear();
                return req;
            }
            txtPassword.clear();
            txtConfirmacion.clear();
            return null;
        });

        Optional<CrearEmpleadoRequest> resultado = dialog.showAndWait();
        resultado.ifPresent(req -> {
            if (!servicioConexion.estaEnLinea()) {
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Sin Conexión", "Requiere conexión", "No se puede dar de alta sin conexión.");
                return;
            }

            Thread t = new Thread(() -> {
                try {
                    empleadosPort.crearEmpleado(req);
                    Platform.runLater(() -> {
                        NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Colaborador Creado", "Operación Exitosa", "Colaborador registrado exitosamente.");
                        cargarPaginaEmpleados(paginaActualEmpleados);
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al crear colaborador: " + e.getMessage());
                    Platform.runLater(() -> NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Error de Registro", "No se pudo registrar al colaborador", EmpleadosViewModel.limpiarMensajeError(e)));
                }
            }, "hilo-crear-empleado");
            t.setDaemon(true);
            t.start();
        });
    }

    @FXML
    public void abrirDialogoEditar() {
        EmpleadoDto sel = tablaEmpleados.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Dialog<ActualizarEmpleadoRequest> dialog = new Dialog<>();
        dialog.setTitle("Editar Colaborador");
        dialog.setHeaderText("Modificación de datos (DNI no editable per RF-03)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField txtDni = new TextField(sel.dni());
        txtDni.setDisable(true);
        TextField txtNombres = new TextField(sel.nombres());
        TextField txtApellidos = new TextField(sel.apellidos());
        TextField txtCorreo = new TextField(sel.correo());

        ComboBox<RolComboDto> cboEditarRol = new ComboBox<>(FXCollections.observableArrayList(catalogoRoles));
        configurarComboRol(cboEditarRol);
        for (RolComboDto r : catalogoRoles) {
            if (r.id().equals(sel.idRol())) {
                cboEditarRol.setValue(r);
                break;
            }
        }

        ComboBox<SucursalDto> cboEditarSucursal = new ComboBox<>(FXCollections.observableArrayList(catalogoSucursales));
        configurarComboSucursal(cboEditarSucursal);
        for (SucursalDto s : catalogoSucursales) {
            if (s.id().equals(sel.idSucursal())) {
                cboEditarSucursal.setValue(s);
                break;
            }
        }

        grid.add(new Label("DNI:"), 0, 0);
        grid.add(txtDni, 1, 0);
        grid.add(new Label("Nombres:"), 0, 1);
        grid.add(txtNombres, 1, 1);
        grid.add(new Label("Apellidos:"), 0, 2);
        grid.add(txtApellidos, 1, 2);
        grid.add(new Label("Correo:"), 0, 3);
        grid.add(txtCorreo, 1, 3);
        grid.add(new Label("Rol:"), 0, 4);
        grid.add(cboEditarRol, 1, 4);
        grid.add(new Label("Agencia:"), 0, 5);
        grid.add(cboEditarSucursal, 1, 5);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node btnOk = dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String nombres = txtNombres.getText();
            String apellidos = txtApellidos.getText();
            String correo = txtCorreo.getText();
            String idRol = cboEditarRol.getValue() != null ? cboEditarRol.getValue().id() : null;
            String idSucursal = cboEditarSucursal.getValue() != null ? cboEditarSucursal.getValue().id() : null;

            List<String> errores = EmpleadosViewModel.validarFormularioEdicion(nombres, apellidos, correo, idRol, idSucursal);
            if (!errores.isEmpty()) {
                event.consume();
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Validación", "Datos inválidos", errores.get(0));
            }
        });

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new ActualizarEmpleadoRequest(
                        txtNombres.getText().trim(),
                        txtApellidos.getText().trim(),
                        txtCorreo.getText().trim().toLowerCase(),
                        cboEditarRol.getValue().id(),
                        cboEditarSucursal.getValue().id()
                );
            }
            return null;
        });

        Optional<ActualizarEmpleadoRequest> resultado = dialog.showAndWait();
        resultado.ifPresent(req -> {
            if (!servicioConexion.estaEnLinea()) {
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Sin Conexión", "Requiere conexión", "No se puede editar sin conexión.");
                return;
            }

            Thread t = new Thread(() -> {
                try {
                    empleadosPort.actualizarEmpleado(sel.idEmpleado(), req);
                    Platform.runLater(() -> {
                        NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Actualización Exitosa", "Datos actualizados", "Los datos del colaborador fueron modificados.");
                        cargarPaginaEmpleados(paginaActualEmpleados);
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al editar colaborador: " + e.getMessage());
                    Platform.runLater(() -> NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Error de Modificación", "No se pudo actualizar al colaborador", EmpleadosViewModel.limpiarMensajeError(e)));
                }
            }, "hilo-editar-empleado");
            t.setDaemon(true);
            t.start();
        });
    }

    @FXML
    public void confirmarCambioEstado() {
        EmpleadoDto sel = tablaEmpleados.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        boolean nuevoEstado = !sel.esActivo();
        String accionTexto = nuevoEstado ? "reactivar" : "dar de baja";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(NavegadorUI.getStagePrincipal());
        confirm.setTitle("Confirmar Cambio de Estado");
        confirm.setHeaderText("¿Está seguro de " + accionTexto + " a este colaborador?");
        confirm.setContentText("Colaborador: " + sel.nombreCompleto() + "\nDNI: " + sel.dni());

        Optional<ButtonType> respuesta = confirm.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            if (!servicioConexion.estaEnLinea()) {
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Sin Conexión", "Requiere conexión", "No se puede modificar estado sin conexión.");
                return;
            }

            Thread t = new Thread(() -> {
                try {
                    empleadosPort.cambiarEstado(sel.idEmpleado(), new CambiarEstadoEmpleadoRequest(nuevoEstado));
                    Platform.runLater(() -> {
                        NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Estado Modificado", "Operación Exitosa", "El estado laboral fue actualizado.");
                        cargarPaginaEmpleados(paginaActualEmpleados);
                    });
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al cambiar estado de colaborador: " + e.getMessage());
                    Platform.runLater(() -> NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Acción no permitida", "No se pudo cambiar el estado", EmpleadosViewModel.limpiarMensajeError(e)));
                }
            }, "hilo-cambiar-estado-empleado");
            t.setDaemon(true);
            t.start();
        }
    }

    @FXML
    public void abrirDialogoRestablecerPassword() {
        EmpleadoDto sel = tablaEmpleados.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Restablecer Contraseña");
        dialog.setHeaderText("Nueva contraseña para: " + sel.nombreCompleto() + " (" + sel.dni() + ")");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        PasswordField txtNuevaPass = new PasswordField();
        PasswordField txtConf = new PasswordField();

        grid.add(new Label("Nueva contraseña:"), 0, 0);
        grid.add(txtNuevaPass, 1, 0);
        grid.add(new Label("Confirmación:"), 0, 1);
        grid.add(txtConf, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node btnOk = dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String p1 = txtNuevaPass.getText();
            String p2 = txtConf.getText();

            List<String> errores = EmpleadosViewModel.validarPasswordRestablecimiento(p1, p2);
            if (!errores.isEmpty()) {
                event.consume();
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Contraseña Inválida", "Verifique los datos", errores.get(0));
            }
        });

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                String p = txtNuevaPass.getText();
                txtNuevaPass.clear();
                txtConf.clear();
                return p;
            }
            txtNuevaPass.clear();
            txtConf.clear();
            return null;
        });

        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(nuevaPass -> {
            if (!servicioConexion.estaEnLinea()) {
                NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Sin Conexión", "Requiere conexión", "No se puede restablecer sin conexión.");
                return;
            }

            Thread t = new Thread(() -> {
                try {
                    empleadosPort.restablecerPassword(sel.idEmpleado(), new RestablecerPasswordRequest(nuevaPass));
                    Platform.runLater(() -> NavegadorUI.mostrarAlerta(
                            Alert.AlertType.INFORMATION, "Contraseña Actualizada", "Operación Exitosa", "Contraseña restablecida exitosamente."
                    ));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al restablecer contraseña: " + e.getMessage());
                    Platform.runLater(() -> NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo restablecer la contraseña", EmpleadosViewModel.limpiarMensajeError(e)));
                }
            }, "hilo-restablecer-password");
            t.setDaemon(true);
            t.start();
        });
    }

    // =========================================================================
    // ACCIONES: BITÁCORA DE AUDITORÍA
    // =========================================================================

    @FXML
    public void filtrarAuditoria() {
        cargarPaginaAuditoria(0);
    }

    @FXML
    public void limpiarFiltrosAuditoria() {
        if (!cboFiltroAuditoriaEmpleado.getItems().isEmpty()) {
            cboFiltroAuditoriaEmpleado.getSelectionModel().selectFirst();
        }
        cboFiltroAuditoriaAccion.setValue("TODAS");
        dpDesde.setValue(null);
        dpHasta.setValue(null);
        cargarPaginaAuditoria(0);
    }

    @FXML
    public void paginaAnteriorAuditoria() {
        if (paginaActualAuditoria > 0) {
            cargarPaginaAuditoria(paginaActualAuditoria - 1);
        }
    }

    @FXML
    public void paginaSiguienteAuditoria() {
        if (paginaActualAuditoria < totalPaginasAuditoria - 1) {
            cargarPaginaAuditoria(paginaActualAuditoria + 1);
        }
    }

    private void cargarPaginaAuditoria(int pagina) {
        if (!servicioConexion.estaEnLinea()) {
            mostrarBannerError("Requiere conexión: no se puede consultar la bitácora sin conexión.");
            return;
        }

        EmpleadoDto empSel = cboFiltroAuditoriaEmpleado.getValue();
        String idEmpleado = (empSel != null && empSel.idEmpleado() != null) ? empSel.idEmpleado() : null;

        String accionSel = cboFiltroAuditoriaAccion.getValue();
        String accion = (accionSel != null && !"TODAS".equalsIgnoreCase(accionSel)) ? accionSel : null;

        LocalDate fDesde = dpDesde.getValue();
        LocalDate fHasta = dpHasta.getValue();

        OffsetDateTime desde = EmpleadosViewModel.convertirAInicioDiaLima(fDesde);
        OffsetDateTime hasta = EmpleadosViewModel.convertirAFinDiaLima(fHasta);

        ocultarBanner();

        Thread t = new Thread(() -> {
            try {
                PaginaDto<ItemAuditoriaDto> resultado = auditoriaPort.listarAuditoria(
                        idEmpleado, accion, desde, hasta, pagina, TAMANO_PAGINA_AUDITORIA
                );

                Platform.runLater(() -> {
                    paginaActualAuditoria = resultado.number();
                    totalPaginasAuditoria = resultado.totalPages();

                    tablaAuditoria.setItems(FXCollections.observableArrayList(resultado.content()));
                    lblAuditoriaPaginacion.setText(EmpleadosViewModel.textoPaginacion(
                            paginaActualAuditoria, totalPaginasAuditoria, resultado.totalElements()
                    ));

                    btnAuditoriaAnterior.setDisable(resultado.first() || resultado.empty());
                    btnAuditoriaSiguiente.setDisable(resultado.last() || resultado.empty());
                });
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error al consultar auditoría: " + e.getMessage());
                Platform.runLater(() -> mostrarBannerError(EmpleadosViewModel.limpiarMensajeError(e)));
            }
        }, "hilo-listar-auditoria");
        t.setDaemon(true);
        t.start();
    }

    private void mostrarBannerError(String mensaje) {
        bannerEstado.setVisible(true);
        bannerEstado.setManaged(true);
        lblBannerEstado.setText(mensaje);
    }

    private void ocultarBanner() {
        bannerEstado.setVisible(false);
        bannerEstado.setManaged(false);
        lblBannerEstado.setText("");
    }
}
