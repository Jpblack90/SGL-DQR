package pe.marvisur.sgldqr.api.persistencia.especificacion;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Constructor de condiciones dinámicas (JPA Criteria API) para consultas de bitácora de auditoría (RF-03).
 * Solo agrega predicados para los filtros presentes, evitando parámetros nulos sin tipo en PostgreSQL.
 */
public final class AuditoriaFiltroEspecificacion {

    private AuditoriaFiltroEspecificacion() {
    }

    /**
     * Construye una especificación basada en los filtros opcionales proporcionados.
     *
     * @param idEmpleado identificador del empleado (opcional)
     * @param accion     acción auditada (opcional)
     * @param desde      fecha y hora inicial (opcional)
     * @param hasta      fecha y hora final (opcional)
     * @return Specification con los predicados correspondientes o conjunción vacía si no hay filtros
     */
    public static Specification<AuditoriaLogJpaEntity> conFiltros(
            String idEmpleado,
            String accion,
            OffsetDateTime desde,
            OffsetDateTime hasta) {

        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (idEmpleado != null && !idEmpleado.isBlank()) {
                predicados.add(cb.equal(root.get("idEmpleado"), idEmpleado.trim()));
            }
            if (accion != null && !accion.isBlank()) {
                predicados.add(cb.equal(root.get("accion"), accion.trim()));
            }
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), desde));
            }
            if (hasta != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("fechaHora"), hasta));
            }

            if (predicados.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
