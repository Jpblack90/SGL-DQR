package pe.marvisur.core.aplicacion.puerto;

/**
 * Puerto de impresión de tickets.
 *
 * <p>Los adaptadores que implementen este puerto pueden enviar el ticket a:
 * <ul>
 *   <li>Ticketera térmica vía ESC/POS (adaptador de escritorio).</li>
 *   <li>Impresora virtual PDF (para pruebas o entornos sin hardware).</li>
 *   <li>Consola estándar (adaptador de prueba en entorno de test).</li>
 * </ul>
 * </p>
 */
public interface ImpresionPort {

    /**
     * Imprime el ticket de la encomienda con los datos proporcionados.
     *
     * @param datos cadena con la información del ticket a imprimir
     *              (puede incluir texto y representación del QR1)
     */
    void imprimirTicket(String datos);
}
