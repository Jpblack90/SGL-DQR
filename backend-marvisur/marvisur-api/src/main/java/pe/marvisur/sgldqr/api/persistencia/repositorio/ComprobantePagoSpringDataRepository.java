package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.ComprobantePagoJpaEntity;

import java.util.Optional;

@Repository
public interface ComprobantePagoSpringDataRepository extends JpaRepository<ComprobantePagoJpaEntity, String> {
    Optional<ComprobantePagoJpaEntity> findByIdEncomienda(String idEncomienda);
    Optional<ComprobantePagoJpaEntity> findBySerieCorrelativo(String serieCorrelativo);
}
