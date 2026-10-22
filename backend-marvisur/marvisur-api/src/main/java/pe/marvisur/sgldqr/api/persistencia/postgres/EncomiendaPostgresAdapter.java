package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.EncomiendaMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.puertos.RepositorioEncomiendaPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para el agregado Encomienda.
 */
@Component
public class EncomiendaPostgresAdapter implements RepositorioEncomiendaPort {

    private final EncomiendaSpringDataRepository repository;

    public EncomiendaPostgresAdapter(EncomiendaSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(Encomienda encomienda) {
        Objects.requireNonNull(encomienda, "La encomienda no puede ser nula");
        EncomiendaJpaEntity entidad = EncomiendaMapper.aEntidad(encomienda);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Encomienda> buscarPorId(String idEncomienda) {
        Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        return repository.findById(idEncomienda)
                .map(EncomiendaMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Encomienda> buscarPorCodigoComprobante(String codigoComprobante) {
        Objects.requireNonNull(codigoComprobante, "El codigoComprobante no puede ser nulo");
        return repository.findByCodigoComprobante(codigoComprobante)
                .map(EncomiendaMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Encomienda> buscarPorSucursalDestinoYEstado(String idSucursalDestino, EstadoLogistico estado) {
        Objects.requireNonNull(idSucursalDestino, "El idSucursalDestino no puede ser nulo");
        Objects.requireNonNull(estado, "El estado logístico no puede ser nulo");
        return repository.findByIdSucursalDestinoAndEstadoLogistico(idSucursalDestino, estado.name())
                .stream()
                .map(EncomiendaMapper::aDominio)
                .toList();
    }

    @Override
    @Transactional
    public void actualizar(Encomienda encomienda) {
        Objects.requireNonNull(encomienda, "La encomienda no puede ser nula");
        EncomiendaJpaEntity entidad = EncomiendaMapper.aEntidad(encomienda);
        repository.save(entidad);
    }
}
