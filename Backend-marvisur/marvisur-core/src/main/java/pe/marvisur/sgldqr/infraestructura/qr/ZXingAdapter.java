package pe.marvisur.sgldqr.infraestructura.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionDominio;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Adaptador de infraestructura que implementa {@link ImagenQRPort} utilizando la librería ZXing.
 *
 * <p>Permite renderizar matrices QR en formato de imagen PNG para impresión de tickets térmicos (QR1)
 * o visualización en la billetera web (QR2), así como decodificar bytes de imágenes provenientes de escáneres ópticos.</p>
 */
public class ZXingAdapter implements ImagenQRPort {

    private static final String FORMATO_PNG = "PNG";

    @Override
    public byte[] generarPNG(String contenido, int tamano) {
        Objects.requireNonNull(contenido, "El contenido para generar el QR no puede ser nulo");
        if (contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido para generar el QR no puede estar vacío.");
        }
        if (tamano <= 0) {
            throw new IllegalArgumentException("El tamaño del QR debe ser mayor a cero.");
        }

        try {
            Map<EncodeHintType, Object> configuracion = new EnumMap<>(EncodeHintType.class);
            configuracion.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            configuracion.put(EncodeHintType.MARGIN, 1);
            configuracion.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

            QRCodeWriter escritor = new QRCodeWriter();
            BitMatrix matriz = escritor.encode(contenido, BarcodeFormat.QR_CODE, tamano, tamano, configuracion);

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, FORMATO_PNG, salida);
            return salida.toByteArray();
        } catch (WriterException | IOException e) {
            throw new ExcepcionDominio("Error al generar la imagen PNG del código QR: " + e.getMessage(), e);
        }
    }

    @Override
    public String decodificar(byte[] bytesImagen) {
        Objects.requireNonNull(bytesImagen, "Los bytes de la imagen no pueden ser nulos");
        if (bytesImagen.length == 0) {
            throw new IllegalArgumentException("Los bytes de la imagen no pueden estar vacíos.");
        }

        try {
            ByteArrayInputStream entrada = new ByteArrayInputStream(bytesImagen);
            BufferedImage imagen = ImageIO.read(entrada);
            if (imagen == null) {
                throw new ExcepcionDominio("No fue posible interpretar el archivo de imagen a partir de los bytes recibidos.");
            }

            BufferedImageLuminanceSource fuente = new BufferedImageLuminanceSource(imagen);
            BinaryBitmap mapaBits = new BinaryBitmap(new HybridBinarizer(fuente));
            Result resultado = new MultiFormatReader().decode(mapaBits);
            return resultado.getText();
        } catch (NotFoundException e) {
            throw new ExcepcionDominio("No se encontró ningún código QR válido en la imagen proporcionada.", e);
        } catch (IOException e) {
            throw new ExcepcionDominio("Error de lectura al decodificar la imagen del código QR: " + e.getMessage(), e);
        }
    }
}
