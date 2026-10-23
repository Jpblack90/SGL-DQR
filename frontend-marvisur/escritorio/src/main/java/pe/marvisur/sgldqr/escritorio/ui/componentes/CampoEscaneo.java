package pe.marvisur.sgldqr.escritorio.ui.componentes;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.function.Consumer;

/**
 * Componente de entrada reutilizable para escáneres ópticos USB (que emulan teclado enviando Enter al final).
 * <p>
 * Características:
 * - Procesa automáticamente el código al recibir la tecla Enter.
 * - Limpia inmediatamente el campo tras la lectura exitosa.
 * - Mantiene el foco para escaneos continuos en ráfaga (Batch Scanning).
 * - Admite ingreso manual o pegado desde portapapeles para pruebas sin hardware físico.
 * </p>
 */
public class CampoEscaneo extends TextField {

    private Consumer<String> onCodigoEscaneado;
    private boolean autoLimpiar = true;

    public CampoEscaneo() {
        this(null);
    }

    public CampoEscaneo(String promptText) {
        if (promptText != null) {
            setPromptText(promptText);
        }
        getStyleClass().add("campo-escaneo");

        // Captura de Enter (tanto de emulación de escáner USB como teclado físico)
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                procesarEntrada();
            }
        });
    }

    public void setOnCodigoEscaneado(Consumer<String> onCodigoEscaneado) {
        this.onCodigoEscaneado = onCodigoEscaneado;
    }

    public void setAutoLimpiar(boolean autoLimpiar) {
        this.autoLimpiar = autoLimpiar;
    }

    public void procesarEntrada() {
        String texto = getText();
        if (texto != null && !texto.isBlank()) {
            String codigoLimpio = texto.trim();
            if (autoLimpiar) {
                clear();
            }
            if (onCodigoEscaneado != null) {
                onCodigoEscaneado.accept(codigoLimpio);
            }
        }
        enfocar();
    }

    public void enfocar() {
        Platform.runLater(this::requestFocus);
    }
}
