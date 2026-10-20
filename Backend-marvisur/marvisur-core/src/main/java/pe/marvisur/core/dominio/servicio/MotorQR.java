package pe.marvisur.core.dominio.servicio;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import pe.marvisur.core.dominio.modelo.CodigoQR;
import pe.marvisur.core.dominio.modelo.EstadoQR2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de dominio responsable del Motor Doble QR del sistema SGL-DQR.
 *
 * <p><strong>Responsabilidades:</strong></p>
 * <ol>
 *   <li>Generar el par QR1 (físico) + QR2 (digital) con ZXing.</li>
 *   <li>Calcular el hash SHA-256 de los bytes PNG de cada QR (nunca del texto en claro).</li>
 *   <li>Validar cruzadamente QR1 + QR2 contra los hashes almacenados.</li>
 *   <li>Delegar la invalidación al propio objeto {@link CodigoQR}.</li>
 * </ol>
 *
 * <p><strong>Seguridad:</strong> Este servicio nunca almacena ni retorna los valores
 * en claro de los QR; solo opera con hashes SHA-256 y bytes de imagen.</p>
 */
public class MotorQR {

    // -------------------------------------------------------------------------
    // Constantes de generación de QR
    // -------------------------------------------------------------------------

    /** Dimensión (ancho y alto) de los QR generados en píxeles. */
    private static final int DIMENSION_QR_PX = 300;

    /** Algoritmo de hash utilizado para proteger los valores QR. */
    private static final String ALGORITMO_HASH = "SHA-256";

    /** Prefijo de contenido para el QR1 físico. */
    private static final String PREFIJO_QR1 = "MARVISUR:QR1:";

    /** Prefijo de contenido para el QR2 digital. */
    private static final String PREFIJO_QR2 = "MARVISUR:QR2:";

    // -------------------------------------------------------------------------
    // Métodos públicos del servicio
    // -------------------------------------------------------------------------

    /**
     * Genera el par de códigos QR (doble QR) para una encomienda.
     *
     * <p>El contenido de cada QR incluye un UUID aleatorio para garantizar
     * unicidad incluso si los parámetros se repiten.</p>
     *
     * <p>Proceso:
     * <ol>
     *   <li>Construye cadenas de contenido únicas para QR1 y QR2.</li>
     *   <li>Genera los bytes PNG con ZXing (300×300 px).</li>
     *   <li>Calcula SHA-256 de los bytes PNG (no de la cadena).</li>
     *   <li>Retorna {@link ParDobleQR} con hashes e imágenes.</li>
     * </ol>
     * </p>
     *
     * @param encomiendaId  UUID de la encomienda a la que pertenecen los QR
     * @param payloadCliente datos adicionales del cliente para el QR2 (nombre, token, etc.)
     * @return par de QR con sus hashes SHA-256 e imágenes PNG
     * @throws RuntimeException si ZXing falla al generar la imagen
     */
    public ParDobleQR generarDobleQR(String encomiendaId, String payloadCliente) {
        // Contenido único para QR1 (físico — impreso en ticket)
        String contenidoQR1 = PREFIJO_QR1 + encomiendaId + ":" + UUID.randomUUID();

        // Contenido único para QR2 (digital — enviado al destinatario)
        String contenidoQR2 = PREFIJO_QR2 + encomiendaId + ":" + payloadCliente + ":" + UUID.randomUUID();

        // Generar imágenes PNG con ZXing
        byte[] imagenQR1 = generarImagenQR(contenidoQR1);
        byte[] imagenQR2 = generarImagenQR(contenidoQR2);

        // Calcular SHA-256 de los bytes de imagen (nunca del texto en claro)
        String hashQR1 = calcularSHA256(imagenQR1);
        String hashQR2 = calcularSHA256(imagenQR2);

        return new ParDobleQR(hashQR1, imagenQR1, hashQR2, imagenQR2);
    }

    /**
     * Valida cruzadamente los códigos QR1 y QR2 escaneados contra los hashes almacenados.
     *
     * <p>La validación calcula el SHA-256 de los bytes del string leído (codificado en UTF-8)
     * y lo compara con el hash guardado en {@link CodigoQR}.</p>
     *
     * <p><strong>Nota:</strong> En un flujo real, los QR se escanean como cadena de texto;
     * se hashea esa cadena UTF-8 para comparar con el hash almacenado.</p>
     *
     * @param qr1Leido  contenido del QR1 tal como fue leído por el escáner
     * @param qr2Leido  contenido del QR2 tal como fue leído por el escáner
     * @param codigoQR  objeto con los hashes SHA-256 almacenados para esta encomienda
     * @return resultado de la validación cruzada
     */
    public ResultadoValidacion validarCruzado(String qr1Leido, String qr2Leido, CodigoQR codigoQR) {
        // Verificar si el QR2 ya fue invalidado (entrega previa detectada)
        if (codigoQR.getQr2Estado() == EstadoQR2.INVALIDADO) {
            return ResultadoValidacion.QR2_YA_INVALIDADO;
        }

        // Calcular hashes de los QR escaneados (codificados en UTF-8)
        String hashQR1Leido = calcularSHA256(qr1Leido.getBytes(StandardCharsets.UTF_8));
        String hashQR2Leido = calcularSHA256(qr2Leido.getBytes(StandardCharsets.UTF_8));

        // Comparar con los hashes almacenados
        boolean qr1Valido = hashQR1Leido.equals(codigoQR.getQr1Hash());
        boolean qr2Valido = hashQR2Leido.equals(codigoQR.getQr2Hash());

        if (qr1Valido && qr2Valido) {
            return ResultadoValidacion.VALIDO;
        }

        return ResultadoValidacion.HASH_NO_COINCIDE;
    }

    /**
     * Delega la invalidación del QR2 al objeto {@link CodigoQR}.
     *
     * @param codigoQR objeto QR cuyo QR2 debe invalidarse
     * @return el mismo objeto {@link CodigoQR} con el estado actualizado
     * @throws pe.marvisur.core.dominio.excepcion.ExcepcionQR2YaInvalidado si ya estaba invalidado
     */
    public CodigoQR invalidarQR2(CodigoQR codigoQR) {
        codigoQR.invalidarQR2();
        return codigoQR;
    }

    // -------------------------------------------------------------------------
    // Métodos privados auxiliares
    // -------------------------------------------------------------------------

    /**
     * Genera los bytes PNG de un código QR usando ZXing (300×300 px).
     *
     * @param contenido texto a codificar en el QR
     * @return bytes PNG del QR generado
     * @throws RuntimeException si ZXing lanza {@link WriterException} o {@link IOException}
     */
    private byte[] generarImagenQR(String contenido) {
        try {
            QRCodeWriter escritorQR = new QRCodeWriter();

            // Configuración de codificación UTF-8 para caracteres especiales
            Map<EncodeHintType, Object> sugerencias = new EnumMap<>(EncodeHintType.class);
            sugerencias.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            sugerencias.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrizBits = escritorQR.encode(
                    contenido, BarcodeFormat.QR_CODE,
                    DIMENSION_QR_PX, DIMENSION_QR_PX,
                    sugerencias
            );

            ByteArrayOutputStream flujoSalida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrizBits, "PNG", flujoSalida);
            return flujoSalida.toByteArray();

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Error al generar imagen QR con ZXing: " + e.getMessage(), e);
        }
    }

    /**
     * Calcula el hash SHA-256 de un arreglo de bytes y lo retorna como cadena hexadecimal.
     *
     * @param datos bytes de entrada
     * @return cadena hexadecimal del hash SHA-256 (64 caracteres)
     * @throws RuntimeException si el algoritmo SHA-256 no está disponible
     */
    private String calcularSHA256(byte[] datos) {
        try {
            MessageDigest digestor = MessageDigest.getInstance(ALGORITMO_HASH);
            byte[] bytesHash = digestor.digest(datos);
            return bytesAHexadecimal(bytesHash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en Java; esto nunca debería ocurrir
            throw new RuntimeException("Algoritmo SHA-256 no disponible en esta JVM", e);
        }
    }

    /**
     * Convierte un arreglo de bytes en su representación hexadecimal en minúsculas.
     *
     * @param bytes arreglo de bytes a convertir
     * @return cadena hexadecimal
     */
    private String bytesAHexadecimal(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
