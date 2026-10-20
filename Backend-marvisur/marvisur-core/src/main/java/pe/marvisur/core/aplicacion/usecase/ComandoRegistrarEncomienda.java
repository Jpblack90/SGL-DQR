package pe.marvisur.core.aplicacion.usecase;

/**
 * Comando de entrada para el caso de uso {@link RegistrarEncomiendaUseCase}.
 *
 * <p>Contiene todos los datos necesarios para registrar una nueva encomienda
 * y generar su par de códigos QR.</p>
 *
 * @param remitente      nombre completo del remitente
 * @param destinatario   nombre completo del destinatario
 * @param peso           peso del paquete en kilogramos (debe ser positivo)
 * @param formaPago      modalidad de pago ({@link FormaPago#CONTADO} o {@link FormaPago#CONTRA_ENTREGA})
 * @param payloadCliente datos adicionales del destinatario para el QR2 (nombre, teléfono, token, etc.)
 */
public record ComandoRegistrarEncomienda(
        String remitente,
        String destinatario,
        double peso,
        FormaPago formaPago,
        String payloadCliente
) {
}
