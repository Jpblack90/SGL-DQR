package pe.marvisur.core.aplicacion.usecase;

import pe.marvisur.core.aplicacion.puerto.ImpresionPort;
import pe.marvisur.core.aplicacion.puerto.RepositorioCodigoQRPort;
import pe.marvisur.core.aplicacion.puerto.RepositorioEncomiendaPort;
import pe.marvisur.core.dominio.modelo.CodigoQR;
import pe.marvisur.core.dominio.modelo.Encomienda;
import pe.marvisur.core.dominio.modelo.EstadoEnvio;
import pe.marvisur.core.dominio.modelo.EstadoPago;
import pe.marvisur.core.dominio.servicio.MotorQR;
import pe.marvisur.core.dominio.servicio.ParDobleQR;

/**
 * Caso de uso: Registrar una nueva encomienda en el sistema SGL-DQR.
 *
 * <p><strong>Flujo de negocio:</strong></p>
 * <ol>
 *   <li>Crear la entidad {@link Encomienda} con UUID nuevo.</li>
 *   <li>Calcular la tarifa de envío según el peso.</li>
 *   <li>Determinar el estado de pago según la forma de pago elegida.</li>
 *   <li>Generar el par de códigos QR (Doble QR) con el {@link MotorQR}.</li>
 *   <li>Persistir los códigos QR en el repositorio.</li>
 *   <li>Persistir la encomienda en el repositorio.</li>
 *   <li>Imprimir el ticket de despacho con el QR1.</li>
 *   <li>Retornar la encomienda registrada.</li>
 * </ol>
 */
public class RegistrarEncomiendaUseCase {

    // -------------------------------------------------------------------------
    // Puertos (dependencias inyectadas)
    // -------------------------------------------------------------------------

    private final RepositorioEncomiendaPort repositorioEncomienda;
    private final RepositorioCodigoQRPort repositorioCodigoQR;
    private final ImpresionPort impresion;
    private final MotorQR motorQR;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Construye el caso de uso con sus dependencias inyectadas via constructor.
     *
     * @param repositorioEncomienda puerto de persistencia de encomiendas
     * @param repositorioCodigoQR   puerto de persistencia de códigos QR
     * @param impresion             puerto de impresión de tickets
     * @param motorQR               servicio de dominio para generar y validar QR
     */
    public RegistrarEncomiendaUseCase(RepositorioEncomiendaPort repositorioEncomienda,
                                      RepositorioCodigoQRPort repositorioCodigoQR,
                                      ImpresionPort impresion,
                                      MotorQR motorQR) {
        this.repositorioEncomienda = repositorioEncomienda;
        this.repositorioCodigoQR = repositorioCodigoQR;
        this.impresion = impresion;
        this.motorQR = motorQR;
    }

    // -------------------------------------------------------------------------
    // Método principal del caso de uso
    // -------------------------------------------------------------------------

    /**
     * Registra una nueva encomienda a partir del comando recibido.
     *
     * @param comando datos de entrada con remitente, destinatario, peso y forma de pago
     * @return encomienda recién registrada con ID, tarifa y estados asignados
     */
    public Encomienda registrar(ComandoRegistrarEncomienda comando) {

        // --- Paso 1: Determinar estado de pago según forma de pago ---
        EstadoPago estadoPago = (comando.formaPago() == FormaPago.CONTRA_ENTREGA)
                ? EstadoPago.PENDIENTE
                : EstadoPago.PAGADO;

        // --- Paso 2: Crear la encomienda (genera UUID internamente) ---
        Encomienda encomienda = new Encomienda(
                comando.remitente(),
                comando.destinatario(),
                comando.peso(),
                estadoPago
        );
        // estadoEnvio = REGISTRADA (asignado en el constructor)

        // --- Paso 3: Calcular tarifa y asignar ---
        encomienda.setTarifa(encomienda.calcularTarifa());

        // --- Paso 4: Generar par de códigos QR (Doble QR) ---
        ParDobleQR parQR = motorQR.generarDobleQR(encomienda.getId(), comando.payloadCliente());

        // --- Paso 5: Crear y persistir el CodigoQR ---
        CodigoQR codigoQR = new CodigoQR(
                encomienda.getId(),
                parQR.qr1Hash(),
                parQR.qr2Hash()
        );
        repositorioCodigoQR.guardar(codigoQR);

        // --- Paso 6: Persistir la encomienda ---
        repositorioEncomienda.guardar(encomienda);

        // --- Paso 7: Imprimir ticket con datos del QR1 ---
        String datosTicket = construirDatosTicket(encomienda, parQR);
        impresion.imprimirTicket(datosTicket);

        // --- Paso 8: Retornar encomienda registrada ---
        return encomienda;
    }

    // -------------------------------------------------------------------------
    // Métodos privados auxiliares
    // -------------------------------------------------------------------------

    /**
     * Construye la cadena de texto del ticket de despacho.
     *
     * @param encomienda encomienda registrada
     * @param parQR      par de QR generados
     * @return cadena con la información del ticket para impresión
     */
    private String construirDatosTicket(Encomienda encomienda, ParDobleQR parQR) {
        return "=== EXPRESO MARVISUR ===\n"
                + "ID: " + encomienda.getId() + "\n"
                + "Remitente: " + encomienda.getRemitente() + "\n"
                + "Destinatario: " + encomienda.getDestinatario() + "\n"
                + "Peso: " + encomienda.getPeso() + " kg\n"
                + "Tarifa: S/. " + encomienda.getTarifa() + "\n"
                + "Estado Pago: " + encomienda.getEstadoPago() + "\n"
                + "QR1 Hash: " + parQR.qr1Hash().substring(0, 16) + "...\n"
                + "========================";
    }
}
