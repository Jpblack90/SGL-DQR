package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.RutaJpaEntity;

import java.util.List;

@Repository
public interface RutaSpringDataRepository extends JpaRepository<RutaJpaEntity, String> {
    List<RutaJpaEntity> findByIdSucursalOrigen(String idSucursalOrigen);
}
