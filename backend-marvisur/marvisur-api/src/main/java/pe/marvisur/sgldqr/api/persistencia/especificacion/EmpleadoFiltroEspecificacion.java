package pe.marvisur.sgldqr.api.persistencia.especificacion;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Constructor de condiciones dinámicas (JPA Criteria API) para consulta de empleados con filtros (RF-03).
 * Solo agrega predicados para los filtros presentes, evitando parámetros nulos sin tipo en PostgreSQL.
 */
public final class EmpleadoFiltroEspecificacion {

    private EmpleadoFiltroEspecificacion() {
    }

    /**
     * Construye una especificación basada en los filtros opcionales de empleados.
     *
     * @param estadoFiltro true para activos, false para inactivos, null para todos
     * @param idRol        identificador de rol (opcional)
     * @param idSucursal   identificador de sucursal (opcional)
     * @param q            término de búsqueda en DNI, nombres, apellidos o correo (opcional)
     * @return Specification con los predicados correspondientes
     */
    public static Specification<EmpleadoJpaEntity> conFiltros(
            Boolean estadoFiltro,
            String idRol,
            String idSucursal,
            String q) {

        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (estadoFiltro != null) {
                predicados.add(cb.equal(root.get("estado"), estadoFiltro));
            }
            if (idRol != null && !idRol.isBlank()) {
                predicados.add(cb.equal(root.get("idRol"), idRol.trim()));
            }
            if (idSucursal != null && !idSucursal.isBlank()) {
                predicados.add(cb.equal(root.get("idSucursal"), idSucursal.trim()));
            }
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("dni")), patron),
                        cb.like(cb.lower(root.get("nombres")), patron),
                        cb.like(cb.lower(root.get("apellidos")), patron),
                        cb.like(cb.lower(root.get("correo")), patron)
                ));
            }

            if (predicados.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
