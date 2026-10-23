package pe.marvisur.sgldqr.escritorio.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ElementoColaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EstadoElementoCola;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.ColaLocalPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.SincronizadorOffline;
import pe.marvisur.sgldqr.escritorio.infraestructura.sqlite.SQLiteColaAdapter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador de la vista de Cola Local (TN1 Recepcionista y TN3 Administrador).
 * Permite monitorear el estado de los registros locales en SQLite y forzar reintentos de sincronización (O2).
 */
public class ControladorColaLocal {

    private static final Logger LOGGER = Logger.getLogger(ControladorColaLocal.class.getName());
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML private TableView<ElementoColaDto> tablaColaLocal;
    @FXML private TableColumn<ElementoColaDto, String> colId;
    @FXML private TableColumn<ElementoColaDto, String> colEstado;
    @FXML private TableColumn<ElementoColaDto, Number> colIntentos;
    @FXML private TableColumn<ElementoColaDto, String> colCreadoEn;
    @FXML private TableColumn<ElementoColaDto, String> colActualizadoEn;
    @FXML private TableColumn<ElementoColaDto, String> colUltimoError;

    @FXML private Label lblTotalPendientes;
    @FXML private Label lblTotalSincronizadas;
    @FXML private Label lblTotalErrores;
    @FXML private Label lblTotalRegistros;
    @FXML private Label lblEstadoSincronizacion;

    @FXML private Button btnReintentarErrores;
    @FXML private Button btnSincronizarAhora;
    @FXML private Button btnRefrescar;

    private final ColaLocalPort colaLocalPort;
    private final SincronizadorOffline sincronizadorOffline;
    private final ServicioConexion servicioConexion;

    private final ObservableList<ElementoColaDto> elementosObservables = FXCollections.observableArrayList();
    private SincronizadorOffline.ListenerSincronizacion listenerSincronizacion;

    public ControladorColaLocal() {
        this(new SQLiteColaAdapter(), SincronizadorOffline.getInstancia(), ServicioConexion.getInstancia());
    }

    public ControladorColaLocal(ColaLocalPort colaLocalPort,
                                SincronizadorOffline sincronizadorOffline,
                                ServicioConexion servicioConexion) {
        this.colaLocalPort = colaLocalPort;
        this.sincronizadorOffline = sincronizadorOffline;
        this.servicioConexion = servicioConexion;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarDatos();
        suscribirSincronizacion();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().id()));
        colEstado.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().estado() != null ? cellData.getValue().estado().name() : ""
        ));
        colIntentos.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().intentos()));

        colCreadoEn.setCellValueFactory(cellData -> {
            var fecha = cellData.getValue().creadoEn();
            return new SimpleStringProperty(fecha != null ? fecha.format(FORMATO_FECHA) : "");
        });

        colActualizadoEn.setCellValueFactory(cellData -> {
            var fecha = cellData.getValue().actualizadoEn();
            return new SimpleStringProperty(fecha != null ? fecha.format(FORMATO_FECHA) : "--");
        });

        colUltimoError.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().ultimoError() != null ? cellData.getValue().ultimoError() : "--"
        ));

        // Estilos para el estado
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("PENDIENTE".equals(item)) {
                        setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                    } else if ("SINCRONIZADA".equals(item)) {
                        setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                    } else if ("ERROR".equals(item)) {
                        setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        tablaColaLocal.setItems(elementosObservables);
    }

    @FXML
    public void cargarDatos() {
        try {
            List<ElementoColaDto> todos = colaLocalPort.listarTodos();
            elementosObservables.setAll(todos);

            int pendientes = 0;
            int sincronizadas = 0;
            int errores = 0;

            for (ElementoColaDto item : todos) {
                if (item.estado() == EstadoElementoCola.PENDIENTE) pendientes++;
                else if (item.estado() == EstadoElementoCola.SINCRONIZADA) sincronizadas++;
                else if (item.estado() == EstadoElementoCola.ERROR) errores++;
            }

            lblTotalPendientes.setText(String.valueOf(pendientes));
            lblTotalSincronizadas.setText(String.valueOf(sincronizadas));
            lblTotalErrores.setText(String.valueOf(errores));
            lblTotalRegistros.setText(String.valueOf(todos.size()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al cargar elementos de la cola local SQLite: " + e.getMessage());
        }
    }

    private void suscribirSincronizacion() {
        this.listenerSincronizacion = new SincronizadorOffline.ListenerSincronizacion() {
            @Override
            public void onSincronizacionCompletada(int totalSincronizados, int pendientesRestantes) {
                Platform.runLater(() -> {
                    cargarDatos();
                    lblEstadoSincronizacion.setText("Sincronización completada exitosamente (" + totalSincronizados + " sincronizados).");
                });
            }

            @Override
            public void onColaActualizada(int pendientesRestantes) {
                Platform.runLater(() -> {
                    lblTotalPendientes.setText(String.valueOf(pendientesRestantes));
                });
            }
        };
        sincronizadorOffline.agregarListener(this.listenerSincronizacion);
    }

    @FXML
    public void manejarReintentarErrores() {
        try {
            colaLocalPort.reiniciarErroresReintentables();
            cargarDatos();
            NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Reintento de Sincronización",
                    "Registros restablecidos",
                    "Se han restablecido los registros en error a estado PENDIENTE para un nuevo intento.");
            if (servicioConexion.estaEnLinea()) {
                sincronizadorOffline.sincronizarAhora();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al reiniciar errores en cola local: " + e.getMessage(), e);
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudieron reiniciar los errores", e.getMessage());
        }
    }

    @FXML
    public void manejarSincronizarAhora() {
        if (!servicioConexion.estaEnLinea()) {
            NavegadorUI.mostrarAlerta(Alert.AlertType.WARNING, "Sin Conexión", "No se puede sincronizar",
                    "El equipo se encuentra actualmente sin conexión con el servidor central. "
                    + "Los registros se sincronizarán automáticamente al restablecerse la red.");
            return;
        }

        btnSincronizarAhora.setDisable(true);
        lblEstadoSincronizacion.setText("Sincronizando lote con el servidor central...");

        Task<Integer> tarea = new Task<>() {
            @Override
            protected Integer call() {
                return sincronizadorOffline.sincronizarLote(true);
            }
        };

        tarea.setOnSucceeded(e -> {
            btnSincronizarAhora.setDisable(false);
            int sinc = tarea.getValue();
            cargarDatos();
            lblEstadoSincronizacion.setText("Sincronización manual finalizada: " + sinc + " procesados.");
            NavegadorUI.mostrarAlerta(Alert.AlertType.INFORMATION, "Sincronización", "Lote Procesado",
                    "Se procesaron con éxito " + sinc + " registros en el servidor central.");
        });

        tarea.setOnFailed(e -> {
            btnSincronizarAhora.setDisable(false);
            cargarDatos();
            Throwable ex = tarea.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Error en sincronización.";
            lblEstadoSincronizacion.setText("Error en sincronización manual: " + msg);
            NavegadorUI.mostrarAlerta(Alert.AlertType.ERROR, "Fallo de Sincronización", "Error", msg);
        });

        Thread hilo = new Thread(tarea, "hilo-sincronizacion-manual");
        hilo.setDaemon(true);
        hilo.start();
    }
}
