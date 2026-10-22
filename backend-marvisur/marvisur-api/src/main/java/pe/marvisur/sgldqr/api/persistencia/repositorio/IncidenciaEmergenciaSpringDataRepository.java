package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.IncidenciaEmergenciaJpaEntity;

import java.util.List;

@Repository
public interface IncidenciaEmergenciaSpringDataRepository extends JpaRepository<IncidenciaEmergenciaJpaEntity, String> {

    List<IncidenciaEmergenciaJpaEntity> findByIdEncomiendaOrderByFechaIncidenciaDesc(String idEncomienda);
}
