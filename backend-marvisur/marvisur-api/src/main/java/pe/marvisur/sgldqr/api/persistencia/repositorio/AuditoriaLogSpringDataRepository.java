package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AuditoriaLogSpringDataRepository extends JpaRepository<AuditoriaLogJpaEntity, String> {

    List<AuditoriaLogJpaEntity> findByTablaAfectadaAndIdRegistroAfectadoOrderByFechaHoraDesc(String tablaAfectada, String idRegistroAfectado);

    @Query("SELECT a FROM AuditoriaLogJpaEntity a WHERE " +
           "(:idEmpleado IS NULL OR :idEmpleado = '' OR a.idEmpleado = :idEmpleado) AND " +
           "(:accion IS NULL OR :accion = '' OR a.accion = :accion) AND " +
           "(:desde IS NULL OR a.fechaHora >= :desde) AND " +
           "(:hasta IS NULL OR a.fechaHora <= :hasta) " +
           "ORDER BY a.fechaHora DESC")
    Page<AuditoriaLogJpaEntity> buscarConFiltros(
            @Param("idEmpleado") String idEmpleado,
            @Param("accion") String accion,
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta,
            Pageable pageable);
}
