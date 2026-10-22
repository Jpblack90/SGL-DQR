package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.Cliente;

import java.util.Optional;

/**
 * Puerto de repositorio para la entidad Cliente.
 */
public interface RepositorioClientePort {

    void guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(String idCliente);

    Optional<Cliente> buscarPorNumeroDocumento(String numeroDocumento);
}
