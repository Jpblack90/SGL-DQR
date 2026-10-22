package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para el agregado Encomienda.
 */
public interface RepositorioEncomiendaPort {

    void guardar(Encomienda encomienda);

    Optional<Encomienda> buscarPorId(String idEncomienda);

    Optional<Encomienda> buscarPorCodigoComprobante(String codigoComprobante);

    List<Encomienda> buscarPorSucursalDestinoYEstado(String idSucursalDestino, EstadoLogistico estado);

    void actualizar(Encomienda encomienda);
}
