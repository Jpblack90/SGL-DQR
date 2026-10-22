package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.HistorialTrackingJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.HistorialTrackingMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.HistorialTrackingSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.HistorialTracking;
import pe.marvisur.sgldqr.puertos.HistorialTrackingPort;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador de persistencia PostgreSQL para historial de tracking y trazabilidad.
 */
@Component
public class HistorialTrackingPostgresAdapter implements HistorialTrackingPort {

    private final HistorialTrackingSpringDataRepository repository;

    public HistorialTrackingPostgresAdapter(HistorialTrackingSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void registrarEvento(HistorialTracking evento) {
        Objects.requireNonNull(evento, "El evento no puede ser nulo");
        HistorialTrackingJpaEntity entidad = HistorialTrackingMapper.aEntidad(evento);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialTracking> listarPorEncomienda(String idEncomienda) {
        Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        return repository.findByIdEncomiendaOrderByFechaHoraEventoAsc(idEncomienda)
                .stream()
                .map(HistorialTrackingMapper::aDominio)
                .toList();
    }
}
