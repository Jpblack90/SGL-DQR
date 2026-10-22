package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;

import java.util.Optional;

@Repository
public interface ParDobleQrSpringDataRepository extends JpaRepository<ParDobleQrJpaEntity, String> {
    Optional<ParDobleQrJpaEntity> findByIdEncomienda(String idEncomienda);

    Optional<ParDobleQrJpaEntity> findByUuidQr1Operativo(String uuidQr1Operativo);
}
