package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.TokenQr2EstatusJpaEntity;

import java.util.Optional;

@Repository
public interface TokenQr2EstatusSpringDataRepository extends JpaRepository<TokenQr2EstatusJpaEntity, String> {
    Optional<TokenQr2EstatusJpaEntity> findByIdParejaQr(String idParejaQr);
}
