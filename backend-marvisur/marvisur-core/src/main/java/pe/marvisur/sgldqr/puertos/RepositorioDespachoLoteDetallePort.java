package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.DespachoLoteDetalle;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia y consulta de detalles de despachos por lote.
 */
public interface RepositorioDespachoLoteDetallePort {

    void guardar(DespachoLoteDetalle detalle);

    Optional<DespachoLoteDetalle> buscarPorId(String idDespachoDetalle);

    List<DespachoLoteDetalle> listarPorViaje(String idViaje);

    boolean existeEncomiendaEnViajeActivo(String idEncomienda);
}
