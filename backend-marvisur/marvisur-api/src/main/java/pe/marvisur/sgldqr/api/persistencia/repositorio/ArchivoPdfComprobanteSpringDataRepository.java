package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.ArchivoPdfComprobanteJpaEntity;

import java.util.Optional;

@Repository
public interface ArchivoPdfComprobanteSpringDataRepository extends JpaRepository<ArchivoPdfComprobanteJpaEntity, String> {
    Optional<ArchivoPdfComprobanteJpaEntity> findByIdComprobante(String idComprobante);
}
