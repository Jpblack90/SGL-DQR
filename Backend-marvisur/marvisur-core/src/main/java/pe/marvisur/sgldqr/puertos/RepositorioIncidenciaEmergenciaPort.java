package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.IncidenciaEmergencia;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia y consulta de incidencias de emergencia.
 */
public interface RepositorioIncidenciaEmergenciaPort {

    void guardar(IncidenciaEmergencia incidencia);

    Optional<IncidenciaEmergencia> buscarPorId(String idIncidencia);

    List<IncidenciaEmergencia> listarPorEncomienda(String idEncomienda);
}
