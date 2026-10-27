package pe.marvisur.sgldqr.escritorio.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.SesionContexto;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor de navegación de escenas de JavaFX y manejo global de transiciones y alertas.
 */
public final class NavegadorUI {

    private static final Logger LOGGER = Logger.getLogger(NavegadorUI.class.getName());
    private static Stage stagePrincipal;

    private NavegadorUI() {
    }

    public static final String TITULO_BASE = "Expreso Marvisur · SGL-DQR";

    public static void inicializar(Stage stage) {
        stagePrincipal = stage;
        actualizarTitulo(null);
        stagePrincipal.setMinWidth(960);
        stagePrincipal.setMinHeight(640);

        // Registro de callback de expiración de sesión (401)
        SesionContexto.registrarListenerExpiracion(motivo -> {
            Platform.runLater(() -> {
                mostrarLogin(motivo);
                mostrarAlerta(Alert.AlertType.WARNING, "Sesión Cerrada", "Acceso denegado o sesión expirada", motivo);
            });
        });
    }

    public static Stage getStagePrincipal() {
        return stagePrincipal;
    }

    public static void actualizarTitulo(String submodulo) {
        if (stagePrincipal == null) return;
        if (submodulo != null && !submodulo.isBlank()) {
            stagePrincipal.setTitle(TITULO_BASE + " · " + submodulo.trim());
        } else {
            stagePrincipal.setTitle(TITULO_BASE);
        }
    }

    public static void mostrarLogin(String mensajeOpcional) {
        try {
            FXMLLoader loader = new FXMLLoader(NavegadorUI.class.getResource("/pe/marvisur/sgldqr/escritorio/ui/LoginView.fxml"));
            Parent root = loader.load();

            ControladorLogin controlador = loader.getController();
            if (mensajeOpcional != null && !mensajeOpcional.isBlank()) {
                controlador.mostrarMensajeError(mensajeOpcional);
            }

            Scene scene = new Scene(root, 960, 640);
            aplicarHojasDeEstilo(scene);

            actualizarTitulo(null);
            stagePrincipal.setScene(scene);
            stagePrincipal.centerOnScreen();
            stagePrincipal.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar LoginView.fxml: " + e.getMessage(), e);
        }
    }

    public static void mostrarPrincipal() {
        try {
            FXMLLoader loader = new FXMLLoader(NavegadorUI.class.getResource("/pe/marvisur/sgldqr/escritorio/ui/PrincipalView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1100, 720);
            aplicarHojasDeEstilo(scene);

            actualizarTitulo("Ventanilla");
            stagePrincipal.setScene(scene);
            stagePrincipal.centerOnScreen();
            stagePrincipal.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al cargar PrincipalView.fxml: " + e.getMessage(), e);
        }
    }

    public static void mostrarAlerta(Alert.AlertType tipo, String titulo, String encabezado, String contenido) {
        Alert alerta = new Alert(tipo);
        alerta.initOwner(stagePrincipal);
        alerta.setTitle(titulo);
        alerta.setHeaderText(encabezado);
        alerta.setContentText(contenido);
        alerta.showAndWait();
    }

    private static void aplicarHojasDeEstilo(Scene scene) {
        URL css = NavegadorUI.class.getResource("/pe/marvisur/sgldqr/escritorio/ui/estilos.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }
}
