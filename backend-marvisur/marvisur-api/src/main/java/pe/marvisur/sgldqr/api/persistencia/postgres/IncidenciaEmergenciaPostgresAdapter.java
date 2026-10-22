package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.IncidenciaEmergenciaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.IncidenciaEmergenciaMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.IncidenciaEmergenciaSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.IncidenciaEmergencia;
import pe.marvisur.sgldqr.puertos.RepositorioIncidenciaEmergenciaPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para la entidad IncidenciaEmergencia.
 */
@Component
public class IncidenciaEmergenciaPostgresAdapter implements RepositorioIncidenciaEmergenciaPort {

    private final IncidenciaEmergenciaSpringDataRepository repository;

    public IncidenciaEmergenciaPostgresAdapter(IncidenciaEmergenciaSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(IncidenciaEmergencia incidencia) {
        Objects.requireNonNull(incidencia, "La incidencia no puede ser nula");
        IncidenciaEmergenciaJpaEntity entidad = IncidenciaEmergenciaMapper.aEntidad(incidencia);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IncidenciaEmergencia> buscarPorId(String idIncidencia) {
        Objects.requireNonNull(idIncidencia, "El idIncidencia no puede ser nulo");
        return repository.findById(idIncidencia).map(IncidenciaEmergenciaMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidenciaEmergencia> listarPorEncomienda(String idEncomienda) {
        Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        return repository.findByIdEncomiendaOrderByFechaIncidenciaDesc(idEncomienda).stream()
                .map(IncidenciaEmergenciaMapper::aDominio)
                .toList();
    }
}
