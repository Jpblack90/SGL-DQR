package pe.marvisur.core.dominio.excepcion;

/**
 * Se lanza cuando se intenta realizar una transición de estado
 * no permitida en el ciclo de vida de una {@link pe.marvisur.core.dominio.modelo.Encomienda}.
 *
 * <p>Ejemplo: intentar pasar de {@code REGISTRADA} directamente a {@code ENTREGADA}
 * sin pasar por los estados intermedios obligatorios.</p>
 */
public class ExcepcionTransicionInvalida extends ExcepcionDominio {

    /**
     * @param mensaje descripción de la transición inválida intentada
     */
    public ExcepcionTransicionInvalida(String mensaje) {
        super(mensaje);
    }
}
