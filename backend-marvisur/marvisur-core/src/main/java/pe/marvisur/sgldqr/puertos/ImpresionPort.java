package pe.marvisur.sgldqr.puertos;

/**
 * Puerto para la impresión de tickets físicos térmicos (ej. ESC/POS 58 mm por USB).
 */
public interface ImpresionPort {

    void imprimirTicket(String datosTicket);

    /**
     * Imprime un ticket o etiqueta con un código QR debajo del texto.
     * <p>
     * Se usa para la etiqueta del bulto (QR1) y para el QR de rastreo del ticket del
     * cliente. Nunca debe recibir el QR2: la llave de retiro solo existe en la Billetera Digital.
     * La implementación por defecto imprime solo el texto, para adaptadores sin soporte gráfico.
     * </p>
     *
     * @param datosTicket  texto del ticket, una línea por renglón
     * @param imagenQrPng  imagen PNG del código QR a imprimir
     * @param textoBajoQr  texto opcional impreso debajo del QR (puede ser {@code null})
     */
    default void imprimirTicketConQr(String datosTicket, byte[] imagenQrPng, String textoBajoQr) {
        imprimirTicket(datosTicket);
    }
}
