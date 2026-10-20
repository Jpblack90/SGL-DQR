package pe.marvisur.core.dominio.excepcion;

/**
 * Se lanza cuando se busca una encomienda por ID y no existe en el repositorio.
 */
public class ExcepcionEncomiendaNoEncontrada extends ExcepcionDominio {

    /**
     * @param encomiendaId identificador UUID de la encomienda no encontrada
     */
    public ExcepcionEncomiendaNoEncontrada(String encomiendaId) {
        super("No se encontró ninguna encomienda con ID: " + encomiendaId);
    }
}
