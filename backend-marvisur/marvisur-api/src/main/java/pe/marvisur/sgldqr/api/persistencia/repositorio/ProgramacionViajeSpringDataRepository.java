package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;

import java.util.List;

@Repository
public interface ProgramacionViajeSpringDataRepository extends JpaRepository<ProgramacionViajeJpaEntity, String> {

    @Query("SELECT v FROM ProgramacionViajeJpaEntity v WHERE " +
           "(:estado IS NULL OR v.estadoViaje = :estado) AND " +
           "(:idSucursalOrigen IS NULL OR v.idRuta IN (SELECT r.idRuta FROM RutaJpaEntity r WHERE r.idSucursalOrigen = :idSucursalOrigen)) " +
           "ORDER BY v.fechaSalida DESC")
    List<ProgramacionViajeJpaEntity> listarPorFiltros(@Param("estado") String estado, @Param("idSucursalOrigen") String idSucursalOrigen);
}
