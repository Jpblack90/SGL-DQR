package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.DespachoLoteDetalleJpaEntity;

import java.util.List;

@Repository
public interface DespachoLoteDetalleSpringDataRepository extends JpaRepository<DespachoLoteDetalleJpaEntity, String> {

    List<DespachoLoteDetalleJpaEntity> findByIdViaje(String idViaje);

    @Query("SELECT COUNT(d) > 0 FROM DespachoLoteDetalleJpaEntity d " +
           "JOIN ProgramacionViajeJpaEntity v ON d.idViaje = v.idViaje " +
           "WHERE d.idEncomienda = :idEncomienda AND v.estadoViaje <> 'FINALIZADO'")
    boolean existeEncomiendaEnViajeActivo(@Param("idEncomienda") String idEncomienda);

    boolean existsByIdViajeAndIdEncomienda(String idViaje, String idEncomienda);
}
