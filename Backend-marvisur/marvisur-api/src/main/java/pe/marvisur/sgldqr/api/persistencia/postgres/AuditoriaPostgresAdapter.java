package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.AuditoriaMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.AuditoriaLog;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador de persistencia PostgreSQL para auditoría operativa.
 */
@Component
public class AuditoriaPostgresAdapter implements AuditoriaPort {

    private final AuditoriaLogSpringDataRepository repository;

    public AuditoriaPostgresAdapter(AuditoriaLogSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void registrar(AuditoriaLog log) {
        Objects.requireNonNull(log, "El log no puede ser nulo");
        AuditoriaLogJpaEntity entidad = AuditoriaMapper.aEntidad(log);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaLog> buscarPorRegistro(String tablaAfectada, String idRegistroAfectado) {
        Objects.requireNonNull(tablaAfectada, "La tablaAfectada no puede ser nula");
        Objects.requireNonNull(idRegistroAfectado, "El idRegistroAfectado no puede ser nulo");
        return repository.findByTablaAfectadaAndIdRegistroAfectadoOrderByFechaHoraDesc(tablaAfectada, idRegistroAfectado)
                .stream()
                .map(AuditoriaMapper::aDominio)
                .toList();
    }
}
