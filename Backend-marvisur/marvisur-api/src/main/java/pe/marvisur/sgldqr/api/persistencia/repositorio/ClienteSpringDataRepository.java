package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity;

import java.util.Optional;

@Repository
public interface ClienteSpringDataRepository extends JpaRepository<ClienteJpaEntity, String> {
    Optional<ClienteJpaEntity> findByNumeroDocumento(String numeroDocumento);
}
