package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.Sucursal;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para la entidad Sucursal.
 */
public interface RepositorioSucursalPort {

    void guardar(Sucursal sucursal);

    Optional<Sucursal> buscarPorId(String idSucursal);

    List<Sucursal> listarTodas();
}
