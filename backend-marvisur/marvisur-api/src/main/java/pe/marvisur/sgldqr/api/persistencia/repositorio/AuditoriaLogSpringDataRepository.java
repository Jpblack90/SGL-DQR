package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad 'auditoria_logs' (RF-03).
 * Soporta ejecución de especificaciones dinámicas (JpaSpecificationExecutor)
 * para consultas con filtros opcionales seguras en PostgreSQL.
 */
@Repository
public interface AuditoriaLogSpringDataRepository extends JpaRepository<AuditoriaLogJpaEntity, String>, JpaSpecificationExecutor<AuditoriaLogJpaEntity> {

    List<AuditoriaLogJpaEntity> findByTablaAfectadaAndIdRegistroAfectadoOrderByFechaHoraDesc(String tablaAfectada, String idRegistroAfectado);
}
