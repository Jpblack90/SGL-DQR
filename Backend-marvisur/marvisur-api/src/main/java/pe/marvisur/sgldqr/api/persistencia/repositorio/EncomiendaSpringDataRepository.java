package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncomiendaSpringDataRepository extends JpaRepository<EncomiendaJpaEntity, String>, JpaSpecificationExecutor<EncomiendaJpaEntity> {
    Optional<EncomiendaJpaEntity> findByCodigoComprobante(String codigoComprobante);
    boolean existsByCodigoComprobante(String codigoComprobante);
    List<EncomiendaJpaEntity> findByIdSucursalDestinoAndEstadoLogistico(String idSucursalDestino, String estadoLogistico);
}
