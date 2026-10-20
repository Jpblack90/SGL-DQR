package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.SucursalMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.Sucursal;
import pe.marvisur.sgldqr.puertos.RepositorioSucursalPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para la entidad Sucursal.
 */
@Component
public class SucursalPostgresAdapter implements RepositorioSucursalPort {

    private final SucursalSpringDataRepository repository;

    public SucursalPostgresAdapter(SucursalSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(Sucursal sucursal) {
        Objects.requireNonNull(sucursal, "La sucursal no puede ser nula");
        SucursalJpaEntity entidad = SucursalMapper.aEntidad(sucursal);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sucursal> buscarPorId(String idSucursal) {
        Objects.requireNonNull(idSucursal, "El idSucursal no puede ser nulo");
        return repository.findById(idSucursal)
                .map(SucursalMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sucursal> listarTodas() {
        return repository.findAll()
                .stream()
                .map(SucursalMapper::aDominio)
                .toList();
    }
}
