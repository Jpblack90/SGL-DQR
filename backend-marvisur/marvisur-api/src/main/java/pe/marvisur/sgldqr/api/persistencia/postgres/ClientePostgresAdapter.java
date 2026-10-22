package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.ClienteMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.Cliente;
import pe.marvisur.sgldqr.puertos.RepositorioClientePort;

import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para la entidad Cliente.
 */
@Component
public class ClientePostgresAdapter implements RepositorioClientePort {

    private final ClienteSpringDataRepository repository;

    public ClientePostgresAdapter(ClienteSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(Cliente cliente) {
        Objects.requireNonNull(cliente, "El cliente no puede ser nulo");
        ClienteJpaEntity entidad = ClienteMapper.aEntidad(cliente);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorId(String idCliente) {
        Objects.requireNonNull(idCliente, "El idCliente no puede ser nulo");
        return repository.findById(idCliente)
                .map(ClienteMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorNumeroDocumento(String numeroDocumento) {
        Objects.requireNonNull(numeroDocumento, "El numeroDocumento no puede ser nulo");
        return repository.findByNumeroDocumento(numeroDocumento)
                .map(ClienteMapper::aDominio);
    }
}
