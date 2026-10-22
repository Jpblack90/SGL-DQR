package pe.marvisur.sgldqr.dominio.excepcion;

import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;

/**
 * Se lanza cuando se intenta realizar una transición de estado logístico no permitida.
 */
public class ExcepcionTransicionInvalida extends ExcepcionDominio {

    public ExcepcionTransicionInvalida(EstadoLogistico actual, EstadoLogistico destino) {
        super(String.format("Transición de estado logístico inválida: no es posible pasar de '%s' a '%s'.", actual, destino));
    }
}
