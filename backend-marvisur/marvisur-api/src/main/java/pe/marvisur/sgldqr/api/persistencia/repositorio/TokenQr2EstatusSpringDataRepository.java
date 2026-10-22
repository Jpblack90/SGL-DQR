package pe.marvisur.sgldqr.api.persistencia.repositorio;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.TokenQr2EstatusJpaEntity;

import java.util.Optional;

@Repository
public interface TokenQr2EstatusSpringDataRepository extends JpaRepository<TokenQr2EstatusJpaEntity, String> {

    Optional<TokenQr2EstatusJpaEntity> findByIdParejaQr(String idParejaQr);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT t FROM TokenQr2EstatusJpaEntity t WHERE t.idParejaQr = :idParejaQr")
    Optional<TokenQr2EstatusJpaEntity> findByIdParejaQrConBloqueo(@Param("idParejaQr") String idParejaQr);
}
