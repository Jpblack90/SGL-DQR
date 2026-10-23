package pe.marvisur.sgldqr.escritorio;

import javafx.application.Application;
import javafx.stage.Stage;
import pe.marvisur.sgldqr.escritorio.ui.NavegadorUI;

import java.util.logging.Logger;

/**
 * Punto de entrada principal de la aplicación de escritorio JavaFX de Expreso Marvisur (SGL-DQR).
 */
public class App extends Application {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    @Override
    public void start(Stage stage) {
        LOGGER.info("Iniciando aplicación de escritorio SGL-DQR...");
        NavegadorUI.inicializar(stage);
        NavegadorUI.mostrarLogin(null);
    }

    @Override
    public void stop() {
        LOGGER.info("Cerrando aplicación de escritorio SGL-DQR.");
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
