package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;
import pe.marvisur.sgldqr.dominio.modelo.ProgramacionViaje;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia y consulta de programaciones de viajes.
 */
public interface RepositorioProgramacionViajePort {

    void guardar(ProgramacionViaje viaje);

    Optional<ProgramacionViaje> buscarPorId(String idViaje);

    List<ProgramacionViaje> listarPorFiltros(EstadoViaje estado, String idSucursalOrigen);
}
