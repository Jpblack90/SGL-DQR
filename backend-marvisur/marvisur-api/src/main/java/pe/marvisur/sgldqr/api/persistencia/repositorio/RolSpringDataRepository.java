package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.RolJpaEntity;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad 'roles'.
 */
@Repository
public interface RolSpringDataRepository extends JpaRepository<RolJpaEntity, String> {

    Optional<RolJpaEntity> findByNombreRol(String nombreRol);
}
