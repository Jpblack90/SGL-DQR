package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad 'empleados' (RF-01, RF-03).
 * Soporta ejecución de especificaciones dinámicas (JpaSpecificationExecutor)
 * para consultas con filtros opcionales seguras en PostgreSQL.
 */
@Repository
public interface EmpleadoSpringDataRepository extends JpaRepository<EmpleadoJpaEntity, String>, JpaSpecificationExecutor<EmpleadoJpaEntity> {

    Optional<EmpleadoJpaEntity> findByDni(String dni);

    Optional<EmpleadoJpaEntity> findByCorreo(String correo);

    long countByIdRolAndEstadoTrue(String idRol);
}
