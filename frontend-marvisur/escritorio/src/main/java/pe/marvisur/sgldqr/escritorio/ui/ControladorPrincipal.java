package pe.marvisur.sgldqr.escritorio.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ModeloMenuRol;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.UsuarioSesion;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.AutenticacionPort;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.EstadoConexionPort;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador de la ventana principal y orquestador del menú lateral según rol (RBAC).
 */
public class ControladorPrincipal {

    private static final Logger LOGGER = Logger.getLogger(ControladorPrincipal.class.getName());

    @FXML
    private Circle circuloConexion;

    @FXML
    private Label lblEstadoConexion;

    @FXML
    private Label lblSucursal;

    @FXML
    private Label lblUsuario;

    @FXML
    private Label lblRol;

    @FXML
    private Button btnMenuVentanilla;

    @FXML
    private Button btnMenuAlmacen;

    @FXML
    private Button btnMenuRutas;

    @FXML
    private Button btnMenuDashboard;

    @FXML
    private Button btnMenuEmpleados;

    @FXML
    private StackPane contenedorContenido;

    private final AutenticacionPort autenticacionPort;
    private final EstadoConexionPort estadoConexionPort;
    private ScheduledExecutorService planificadorConectividad;

    public ControladorPrincipal() {
        MarvisurHttpClient client = new MarvisurHttpClient();
        this.autenticacionPort = client;
        this.estadoConexionPort = client;
    }

    public ControladorPrincipal(AutenticacionPort autenticacionPort, EstadoConexionPort estadoConexionPort) {
        this.autenticacionPort = autenticacionPort;
        this.estadoConexionPort = estadoConexionPort;
    }

    @FXML
    public void initialize() {
        configurarDatosUsuario();
        configurarMenuSegunRol();
        iniciarMonitoreoConectividad();
        mostrarVentanilla();
    }

    private void configurarDatosUsuario() {
        UsuarioSesion sesion = SesionContexto.obtenerSesion();
        if (sesion != null) {
            lblUsuario.setText(sesion.getNombreCompleto());
            lblRol.setText(sesion.rol() != null ? sesion.rol().replace("ROLE_", "") : "COLABORADOR");
            lblSucursal.setText("Agencia: " + (sesion.idSucursal() != null ? sesion.idSucursal() : "S/A"));
        } else {
            lblUsuario.setText("Colaborador");
            lblRol.setText("INVITADO");
            lblSucursal.setText("Agencia: --");
        }
    }

    private void configurarMenuSegunRol() {
        UsuarioSesion sesion = SesionContexto.obtenerSesion();
        String rol = sesion != null ? sesion.rol() : "";

        configurarBotonMenu(btnMenuVentanilla, ModeloMenuRol.ModuloMenu.VENTANILLA, rol);
        configurarBotonMenu(btnMenuAlmacen, ModeloMenuRol.ModuloMenu.ALMACEN, rol);
        configurarBotonMenu(btnMenuRutas, ModeloMenuRol.ModuloMenu.RUTAS, rol);
        configurarBotonMenu(btnMenuDashboard, ModeloMenuRol.ModuloMenu.DASHBOARD, rol);
        configurarBotonMenu(btnMenuEmpleados, ModeloMenuRol.ModuloMenu.EMPLEADOS, rol);
    }

    private void configurarBotonMenu(Button boton, ModeloMenuRol.ModuloMenu modulo, String rol) {
        if (boton == null) return;

        boolean autorizado = ModeloMenuRol.estaAutorizado(rol, modulo);
        if (!autorizado) {
            boton.setVisible(false);
            boton.setManaged(false);
            return;
        }

        boolean habilitado = modulo.isImplementado();
        if (!habilitado) {
            boton.setDisable(false); // Se mantiene clickable para mostrar el diálogo explicativo
            boton.setStyle("-fx-opacity: 0.65;");
        }
    }

    private void iniciarMonitoreoConectividad() {
        planificadorConectividad = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hilo-monitoreo-conectividad");
            t.setDaemon(true);
            return t;
        });

        planificadorConectividad.scheduleAtFixedRate(() -> {
            boolean online = estadoConexionPort.verificarConectividad();
            Platform.runLater(() -> {
                if (online) {
                    circuloConexion.setFill(Color.web("#10B981"));
                    lblEstadoConexion.setText("En línea");
                } else {
                    circuloConexion.setFill(Color.web("#E30613"));
                    lblEstadoConexion.setText("Sin conexión");
                }
            });
        }, 0, 5, TimeUnit.SECONDS);
    }

    @FXML
    public void mostrarVentanilla() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pe/marvisur/sgldqr/escritorio/ui/VentanillaView.fxml"));
            Node vista = loader.load();
            contenedorContenido.getChildren().setAll(vista);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar VentanillaView.fxml: " + e.getMessage(), e);
        }
    }

    @FXML
    public void mostrarAvisoProximamente() {
        NavegadorUI.mostrarAlerta(
                Alert.AlertType.INFORMATION,
                "Módulo en Desarrollo",
                "Operación Próximamente Disponible",
                "Este módulo se habilitará en las siguientes entregas del plan de desarrollo (SGL-DQR)."
        );
    }

    @FXML
    public void manejarCerrarSesion() {
        detenerTareasFondo();
        autenticacionPort.cerrarSesion();
        NavegadorUI.mostrarLogin("Ha cerrado sesión correctamente.");
    }

    public void detenerTareasFondo() {
        if (planificadorConectividad != null && !planificadorConectividad.isShutdown()) {
            planificadorConectividad.shutdownNow();
        }
    }
}
