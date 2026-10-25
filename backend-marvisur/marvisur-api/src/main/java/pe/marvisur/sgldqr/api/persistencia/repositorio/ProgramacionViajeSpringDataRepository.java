package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.especificacion.ViajeFiltroEspecificacion;

import java.util.List;

@Repository
public interface ProgramacionViajeSpringDataRepository extends
        JpaRepository<ProgramacionViajeJpaEntity, String>,
        JpaSpecificationExecutor<ProgramacionViajeJpaEntity> {

    boolean existsByIdChoferAndEstadoViaje(String idChofer, String estadoViaje);

    boolean existsByIdVehiculoAndEstadoViaje(String idVehiculo, String estadoViaje);

    default List<ProgramacionViajeJpaEntity> listarPorFiltros(String estado, String idSucursalOrigen) {
        return findAll(
                ViajeFiltroEspecificacion.conFiltros(estado, idSucursalOrigen, null),
                Sort.by(Sort.Direction.DESC, "fechaSalida")
        );
    }
}
