package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.ValidacionCruzadaLogJpaEntity;

import java.util.List;

@Repository
public interface ValidacionCruzadaLogSpringDataRepository extends JpaRepository<ValidacionCruzadaLogJpaEntity, String> {
    List<ValidacionCruzadaLogJpaEntity> findByIdParejaQrOrderByFechaValidacionDesc(String idParejaQr);
}
