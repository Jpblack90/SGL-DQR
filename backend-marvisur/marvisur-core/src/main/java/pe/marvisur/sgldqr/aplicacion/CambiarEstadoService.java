package pe.marvisur.sgldqr.aplicacion;

import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionDominio;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.puertos.RepositorioEncomiendaPort;

import java.util.Objects;

/**
 * Caso de uso para transicionar el estado logístico de una encomienda o registrar su pago.
 */
public class CambiarEstadoService {

    private final RepositorioEncomiendaPort repositorioEncomiendaPort;

    public CambiarEstadoService(RepositorioEncomiendaPort repositorioEncomiendaPort) {
        this.repositorioEncomiendaPort = Objects.requireNonNull(repositorioEncomiendaPort, "El repositorioEncomiendaPort no puede ser nulo");
    }

    /**
     * Cambia el estado logístico de la encomienda, aplicando las validaciones de flujo
     * y el bloqueo automático si el pago está PENDIENTE al intentar entregarla.
     *
     * @param idEncomienda identificador de la encomienda
     * @param nuevoEstado  estado logístico destino
     * @return encomienda con el estado actualizado
     */
    public Encomienda cambiarEstado(String idEncomienda, EstadoLogistico nuevoEstado) {
        Encomienda encomienda = repositorioEncomiendaPort.buscarPorId(idEncomienda)
                .orElseThrow(() -> new ExcepcionDominio(String.format("No se encontró la encomienda con ID '%s'", idEncomienda)));

        encomienda.transicionarEstado(nuevoEstado);
        repositorioEncomiendaPort.actualizar(encomienda);
        return encomienda;
    }

    /**
     * Registra el cobro de una encomienda en modalidad Contra Entrega.
     * Permite desbloquearla para su posterior entrega.
     *
     * @param idEncomienda identificador de la encomienda
     * @return encomienda con el pago actualizado a PAGADO
     */
    public Encomienda registrarCobro(String idEncomienda) {
        Encomienda encomienda = repositorioEncomiendaPort.buscarPorId(idEncomienda)
                .orElseThrow(() -> new ExcepcionDominio(String.format("No se encontró la encomienda con ID '%s'", idEncomienda)));

        encomienda.registrarPago();
        repositorioEncomiendaPort.actualizar(encomienda);
        return encomienda;
    }
}
