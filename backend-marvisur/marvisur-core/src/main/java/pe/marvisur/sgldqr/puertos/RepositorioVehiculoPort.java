package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.Vehiculo;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia y consulta de vehículos de la flota.
 */
public interface RepositorioVehiculoPort {

    void guardar(Vehiculo vehiculo);

    Optional<Vehiculo> buscarPorId(String idVehiculo);

    Optional<Vehiculo> buscarPorPlaca(String placa);

    List<Vehiculo> listarTodos();
}
