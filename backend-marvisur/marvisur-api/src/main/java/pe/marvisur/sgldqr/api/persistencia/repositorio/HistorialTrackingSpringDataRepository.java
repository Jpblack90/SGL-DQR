package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.HistorialTrackingJpaEntity;

import java.util.List;

@Repository
public interface HistorialTrackingSpringDataRepository extends JpaRepository<HistorialTrackingJpaEntity, String> {
    List<HistorialTrackingJpaEntity> findByIdEncomiendaOrderByFechaHoraEventoAsc(String idEncomienda);
}
