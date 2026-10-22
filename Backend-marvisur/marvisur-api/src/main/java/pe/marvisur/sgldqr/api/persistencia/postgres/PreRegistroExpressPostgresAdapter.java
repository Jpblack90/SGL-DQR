package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.PreRegistroExpressJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.PreRegistroExpressMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.PreRegistroExpressSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.PreRegistroExpress;
import pe.marvisur.sgldqr.puertos.RepositorioPreRegistroExpressPort;

import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para pre-registros web ('Código Express').
 */
@Component
public class PreRegistroExpressPostgresAdapter implements RepositorioPreRegistroExpressPort {

    private final PreRegistroExpressSpringDataRepository repository;

    public PreRegistroExpressPostgresAdapter(PreRegistroExpressSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(PreRegistroExpress preRegistro) {
        Objects.requireNonNull(preRegistro, "El preRegistro no puede ser nulo");
        PreRegistroExpressJpaEntity entidad = PreRegistroExpressMapper.aEntidad(preRegistro);
        repository.save(entidad);
    }

    @Override
    @Transactional
    public void actualizar(PreRegistroExpress preRegistro) {
        Objects.requireNonNull(preRegistro, "El preRegistro no puede ser nulo");
        PreRegistroExpressJpaEntity entidad = PreRegistroExpressMapper.aEntidad(preRegistro);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PreRegistroExpress> buscarPorId(String idPreRegistro) {
        Objects.requireNonNull(idPreRegistro, "El idPreRegistro no puede ser nulo");
        return repository.findById(idPreRegistro)
                .map(PreRegistroExpressMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PreRegistroExpress> buscarPorCodigoExpress(String codigoExpress) {
        Objects.requireNonNull(codigoExpress, "El codigoExpress no puede ser nulo");
        return repository.findByCodigoExpress(codigoExpress)
                .map(PreRegistroExpressMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorCodigoExpress(String codigoExpress) {
        Objects.requireNonNull(codigoExpress, "El codigoExpress no puede ser nulo");
        return repository.existsByCodigoExpress(codigoExpress);
    }
}
