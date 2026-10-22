package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.ProgramacionViajeMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ProgramacionViajeSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;
import pe.marvisur.sgldqr.dominio.modelo.ProgramacionViaje;
import pe.marvisur.sgldqr.puertos.RepositorioProgramacionViajePort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para la entidad ProgramacionViaje.
 */
@Component
public class ProgramacionViajePostgresAdapter implements RepositorioProgramacionViajePort {

    private final ProgramacionViajeSpringDataRepository repository;

    public ProgramacionViajePostgresAdapter(ProgramacionViajeSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(ProgramacionViaje viaje) {
        Objects.requireNonNull(viaje, "El viaje no puede ser nulo");
        ProgramacionViajeJpaEntity entidad = ProgramacionViajeMapper.aEntidad(viaje);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProgramacionViaje> buscarPorId(String idViaje) {
        Objects.requireNonNull(idViaje, "El idViaje no puede ser nulo");
        return repository.findById(idViaje).map(ProgramacionViajeMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramacionViaje> listarPorFiltros(EstadoViaje estado, String idSucursalOrigen) {
        String estadoStr = estado != null ? estado.name() : null;
        return repository.listarPorFiltros(estadoStr, idSucursalOrigen).stream()
                .map(ProgramacionViajeMapper::aDominio)
                .toList();
    }
}
