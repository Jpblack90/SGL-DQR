package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.Ruta;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia y consulta de rutas interprovinciales.
 */
public interface RepositorioRutaPort {

    void guardar(Ruta ruta);

    Optional<Ruta> buscarPorId(String idRuta);

    List<Ruta> listarTodas();
}
