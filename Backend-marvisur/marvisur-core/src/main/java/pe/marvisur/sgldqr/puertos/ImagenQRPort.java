package pe.marvisur.sgldqr.puertos;

/**
 * Puerto para la generación y decodificación óptica de imágenes de códigos QR (ZXing).
 */
public interface ImagenQRPort {

    /**
     * Genera la imagen visual del código QR en formato PNG.
     *
     * @param contenido texto o payload que contendrá el código QR
     * @param tamano    dimensión en píxeles (ancho x alto)
     * @return arreglo de bytes con el archivo de imagen PNG
     */
    byte[] generarPNG(String contenido, int tamano);

    /**
     * Decodifica un código QR a partir de los bytes de una imagen.
     *
     * @param bytesImagen bytes del archivo de imagen
     * @return texto contenido dentro del código QR decodificado
     */
    String decodificar(byte[] bytesImagen);
}
