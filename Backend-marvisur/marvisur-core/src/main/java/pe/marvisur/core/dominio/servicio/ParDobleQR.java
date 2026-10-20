package pe.marvisur.core.dominio.servicio;

/**
 * Par de códigos QR generados por el {@link MotorQR} para una encomienda.
 *
 * <p>Cada campo de hash es el digest SHA-256 de los bytes PNG del QR correspondiente.
 * Los bytes de imagen se utilizan para impresión en ticket térmico (QR1)
 * y envío digital al destinatario (QR2).</p>
 *
 * @param qr1Hash   hash SHA-256 hexadecimal del QR1 físico (impreso en ticket)
 * @param qr1Imagen bytes PNG del QR1 generado con ZXing (300×300 px)
 * @param qr2Hash   hash SHA-256 hexadecimal del QR2 digital (enviado al destinatario)
 * @param qr2Imagen bytes PNG del QR2 generado con ZXing (300×300 px)
 */
public record ParDobleQR(
        String qr1Hash,
        byte[] qr1Imagen,
        String qr2Hash,
        byte[] qr2Imagen
) {
}
