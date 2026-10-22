package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.PreRegistroExpressJpaEntity;

import java.util.Optional;

@Repository
public interface PreRegistroExpressSpringDataRepository extends JpaRepository<PreRegistroExpressJpaEntity, String> {

    Optional<PreRegistroExpressJpaEntity> findByCodigoExpress(String codigoExpress);

    boolean existsByCodigoExpress(String codigoExpress);
}
