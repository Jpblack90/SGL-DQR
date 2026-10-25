package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;

import java.util.Optional;

@Repository
public interface EmpleadoSpringDataRepository extends JpaRepository<EmpleadoJpaEntity, String> {

    Optional<EmpleadoJpaEntity> findByDni(String dni);

    Optional<EmpleadoJpaEntity> findByCorreo(String correo);

    long countByIdRolAndEstadoTrue(String idRol);

    @Query("SELECT e FROM EmpleadoJpaEntity e WHERE " +
           "(:estadoFiltro IS NULL OR e.estado = :estadoFiltro) AND " +
           "(:idRol IS NULL OR :idRol = '' OR e.idRol = :idRol) AND " +
           "(:idSucursal IS NULL OR :idSucursal = '' OR e.idSucursal = :idSucursal) AND " +
           "(:q IS NULL OR :q = '' OR " +
           " LOWER(e.dni) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.nombres) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.apellidos) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.correo) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<EmpleadoJpaEntity> buscarConFiltros(
            @Param("estadoFiltro") Boolean estadoFiltro,
            @Param("idRol") String idRol,
            @Param("idSucursal") String idSucursal,
            @Param("q") String q,
            Pageable pageable);
}
