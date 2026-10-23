package pe.marvisur.sgldqr.escritorio.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ApiExcepcion;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.UsuarioSesion;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PestanasBloqueadasOfflineViewModel;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.AutenticacionPort;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;

/**
 * Controlador de la pantalla de inicio de sesión de ventanilla.
 */
public class ControladorLogin {

    @FXML
    private TextField txtCorreo;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnIniciarSesion;

    @FXML
    private Label lblError;

    @FXML
    private ProgressIndicator spinnerCarga;

    private final AutenticacionPort autenticacionPort;

    public ControladorLogin() {
        this(new MarvisurHttpClient());
    }

    public ControladorLogin(AutenticacionPort autenticacionPort) {
        this.autenticacionPort = autenticacionPort;
    }

    @FXML
    public void initialize() {
        lblError.setVisible(false);
        spinnerCarga.setVisible(false);
    }

    @FXML
    public void manejarLogin() {
        String correo = txtCorreo.getText() != null ? txtCorreo.getText().trim() : "";
        String pass = txtPassword.getText() != null ? txtPassword.getText() : "";

        if (correo.isBlank() || pass.isBlank()) {
            mostrarMensajeError("Por favor ingrese su correo corporativo y contraseña.");
            return;
        }

        if (!ServicioConexion.getInstancia().estaEnLinea()) {
            mostrarMensajeError(PestanasBloqueadasOfflineViewModel.MENSAJE_LOGIN_REQUIERE_CONEXION);
            return;
        }

        establecerEstadoCarga(true);
        lblError.setVisible(false);

        Task<UsuarioSesion> tareaLogin = new Task<>() {
            @Override
            protected UsuarioSesion call() {
                return autenticacionPort.autenticar(correo, pass);
            }
        };

        tareaLogin.setOnSucceeded(e -> {
            establecerEstadoCarga(false);
            NavegadorUI.mostrarPrincipal();
        });

        tareaLogin.setOnFailed(e -> {
            establecerEstadoCarga(false);
            Throwable error = tareaLogin.getException();
            String mensaje = "Error al autenticar con el servidor.";
            if (error instanceof ApiExcepcion ae) {
                mensaje = ae.getMessage();
            } else if (error != null && error.getMessage() != null) {
                mensaje = error.getMessage();
            }
            mostrarMensajeError(mensaje);
        });

        Thread hilo = new Thread(tareaLogin, "hilo-login");
        hilo.setDaemon(true);
        hilo.start();
    }

    public void mostrarMensajeError(String mensaje) {
        Platform.runLater(() -> {
            lblError.setText(mensaje);
            lblError.setVisible(true);
        });
    }

    private void establecerEstadoCarga(boolean cargando) {
        btnIniciarSesion.setDisable(cargando);
        btnIniciarSesion.setVisible(!cargando);
        spinnerCarga.setVisible(cargando);
        txtCorreo.setDisable(cargando);
        txtPassword.setDisable(cargando);
    }
}
