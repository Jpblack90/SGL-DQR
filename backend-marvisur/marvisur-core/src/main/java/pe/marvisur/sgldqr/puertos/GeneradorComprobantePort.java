package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.ComprobantePdfDatos;

/**
 * Puerto de salida para la generación física del documento de comprobante de envío en PDF (RF-11).
 * <p>
 * Desacopla la lógica de negocio y dominio de las librerías concretas de generación de PDF (como Apache PDFBox).
 * </p>
 */
public interface GeneradorComprobantePort {

    /**
     * Genera el arreglo de bytes que representa el archivo PDF del comprobante de envío.
     *
     * @param datos modelo con la información del envío, montos y QR1 a plasmar en el documento
     * @return arreglo de bytes del documento PDF
     */
    byte[] generarComprobantePdf(ComprobantePdfDatos datos);
}
