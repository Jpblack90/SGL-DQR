package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;

import java.util.List;

@Repository
public interface AuditoriaLogSpringDataRepository extends JpaRepository<AuditoriaLogJpaEntity, String> {
    List<AuditoriaLogJpaEntity> findByTablaAfectadaAndIdRegistroAfectadoOrderByFechaHoraDesc(String tablaAfectada, String idRegistroAfectado);
}
