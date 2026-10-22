package pe.marvisur.sgldqr.puertos;

/**
 * Puerto para la impresión de tickets físicos térmicos (ej. ESC/POS 58 mm por USB).
 */
public interface ImpresionPort {

    void imprimirTicket(String datosTicket);
}
