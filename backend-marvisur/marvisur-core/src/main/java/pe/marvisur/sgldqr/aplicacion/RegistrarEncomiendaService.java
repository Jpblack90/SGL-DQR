package pe.marvisur.sgldqr.aplicacion;

import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.puertos.ImpresionPort;
import pe.marvisur.sgldqr.puertos.RelojPort;
import pe.marvisur.sgldqr.puertos.RepositorioEncomiendaPort;
import pe.marvisur.sgldqr.puertos.TarifarioPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Caso de uso para registrar una nueva encomienda en ventanilla (RF-04).
 *
 * <p>Flujo:
 * <ol>
 *   <li>Calcula la tarifa oficial mediante {@link TarifarioPort}.</li>
 *   <li>Determina {@link EstadoPago}: {@code PAGADO} si es {@link ModalidadPago#CONTADO},
 *       o {@code PENDIENTE} si es {@link ModalidadPago#CONTRA_ENTREGA}.</li>
 *   <li>Instancia la encomienda en estado inicial {@code REGISTRADO}.</li>
 *   <li>Persiste la encomienda mediante {@link RepositorioEncomiendaPort}.</li>
 *   <li>Genera la impresión del comprobante/ticket mediante {@link ImpresionPort}.</li>
 * </ol>
 * </p>
 */
public class RegistrarEncomiendaService {

    private final RepositorioEncomiendaPort repositorioEncomiendaPort;
    private final TarifarioPort tarifarioPort;
    private final RelojPort relojPort;
    private final ImpresionPort impresionPort;

    public RegistrarEncomiendaService(RepositorioEncomiendaPort repositorioEncomiendaPort,
                                      TarifarioPort tarifarioPort,
                                      RelojPort relojPort,
                                      ImpresionPort impresionPort) {
        this.repositorioEncomiendaPort = Objects.requireNonNull(repositorioEncomiendaPort, "El repositorioEncomiendaPort no puede ser nulo");
        this.tarifarioPort = Objects.requireNonNull(tarifarioPort, "El tarifarioPort no puede ser nulo");
        this.relojPort = Objects.requireNonNull(relojPort, "El relojPort no puede ser nulo");
        this.impresionPort = Objects.requireNonNull(impresionPort, "El impresionPort no puede ser nulo");
    }

    public Encomienda registrar(RegistrarEncomiendaCommand comando) {
        Objects.requireNonNull(comando, "El comando no puede ser nulo");

        // 1. Calcular tarifa
        BigDecimal tarifa = tarifarioPort.calcularTarifa(comando.pesoKg(), comando.volumenM3());

        // 2. Determinar estado de pago según modalidad
        EstadoPago estadoPago = (comando.modalidadPago() == ModalidadPago.CONTRA_ENTREGA)
                ? EstadoPago.PENDIENTE
                : EstadoPago.PAGADO;

        // 3. Obtener fecha y hora actual vía puerto desacoplado
        LocalDateTime fechaRegistro = relojPort.ahora();

        // 4. Crear entidad en estado inicial REGISTRADO
        Encomienda encomienda = Encomienda.crearNueva(
                comando.codigoComprobante(),
                comando.idRemitente(),
                comando.idDestinatario(),
                comando.idSucursalOrigen(),
                comando.idSucursalDestino(),
                comando.pesoKg(),
                comando.volumenM3(),
                comando.descripcionContenido(),
                tarifa,
                estadoPago,
                fechaRegistro
        );

        // 5. Guardar en repositorio
        repositorioEncomiendaPort.guardar(encomienda);

        // 6. Solicitar impresión del ticket físico de ventanilla
        String datosTicket = String.format("MARVISUR TICKET | Comprobante: %s | Tarifa: S/. %s | Pago: %s",
                encomienda.getCodigoComprobante(), encomienda.getMontoTotal(), encomienda.getEstadoPago());
        impresionPort.imprimirTicket(datosTicket);

        return encomienda;
    }
}
