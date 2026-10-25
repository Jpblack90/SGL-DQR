package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.VehiculoJpaEntity;

import java.util.Optional;

@Repository
public interface VehiculoSpringDataRepository extends JpaRepository<VehiculoJpaEntity, String> {
    Optional<VehiculoJpaEntity> findByPlaca(String placa);

    boolean existsByPlacaIgnoreCase(String placa);
}
