package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;

@Repository
public interface SucursalSpringDataRepository extends JpaRepository<SucursalJpaEntity, String> {
}
