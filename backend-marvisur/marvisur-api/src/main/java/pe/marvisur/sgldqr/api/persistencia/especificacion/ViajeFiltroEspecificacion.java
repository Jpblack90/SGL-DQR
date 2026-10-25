package pe.marvisur.sgldqr.api.persistencia.especificacion;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.RutaJpaEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructor de condiciones dinámicas (JPA Criteria API) para la consulta de viajes interprovinciales (RF-05).
 * Genera predicados únicamente para los filtros presentes, evitando parámetros nulos sin tipo en PostgreSQL.
 */
public final class ViajeFiltroEspecificacion {

    private ViajeFiltroEspecificacion() {
    }

    /**
     * Construye una especificación basada en los filtros opcionales de estado, sucursal de origen y chofer.
     *
     * @param estado           estado del viaje (PROGRAMADO, EN_RUTA, FINALIZADO) o null
     * @param idSucursalOrigen identificador de la sucursal de origen de la ruta o null
     * @param idChofer         identificador del empleado chofer asignado o null
     * @return Specification con los predicados correspondientes o conjunción vacía si no hay filtros
     */
    public static Specification<ProgramacionViajeJpaEntity> conFiltros(
            String estado,
            String idSucursalOrigen,
            String idChofer) {

        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (estado != null && !estado.isBlank()) {
                predicados.add(cb.equal(root.get("estadoViaje"), estado.trim()));
            }

            if (idChofer != null && !idChofer.isBlank()) {
                predicados.add(cb.equal(root.get("idChofer"), idChofer.trim()));
            }

            if (idSucursalOrigen != null && !idSucursalOrigen.isBlank()) {
                Subquery<String> subquery = query.subquery(String.class);
                Root<RutaJpaEntity> rutaRoot = subquery.from(RutaJpaEntity.class);
                subquery.select(rutaRoot.get("idRuta"))
                        .where(cb.equal(rutaRoot.get("idSucursalOrigen"), idSucursalOrigen.trim()));
                predicados.add(root.get("idRuta").in(subquery));
            }

            if (predicados.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
