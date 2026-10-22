package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;

import java.util.Optional;

@Repository
public interface EmpleadoSpringDataRepository extends JpaRepository<EmpleadoJpaEntity, String> {
    Optional<EmpleadoJpaEntity> findByDni(String dni);
    Optional<EmpleadoJpaEntity> findByCorreo(String correo);
}
