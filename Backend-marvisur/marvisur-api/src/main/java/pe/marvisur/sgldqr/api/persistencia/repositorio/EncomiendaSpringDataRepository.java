package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncomiendaSpringDataRepository extends JpaRepository<EncomiendaJpaEntity, String>, JpaSpecificationExecutor<EncomiendaJpaEntity> {
    Optional<EncomiendaJpaEntity> findByCodigoComprobante(String codigoComprobante);
    boolean existsByCodigoComprobante(String codigoComprobante);
    List<EncomiendaJpaEntity> findByIdSucursalDestinoAndEstadoLogistico(String idSucursalDestino, String estadoLogistico);

    @Query("SELECT e FROM EncomiendaJpaEntity e WHERE e.estadoLogistico <> 'ENTREGADO' " +
           "AND (e.idRemitente IN (SELECT c.idCliente FROM ClienteJpaEntity c WHERE c.numeroDocumento = :documento) " +
           "     OR e.idDestinatario IN (SELECT c.idCliente FROM ClienteJpaEntity c WHERE c.numeroDocumento = :documento)) " +
           "AND (:idSucursal IS NULL OR e.idSucursalOrigen = :idSucursal OR e.idSucursalDestino = :idSucursal) " +
           "ORDER BY e.fechaRegistro DESC")
    List<EncomiendaJpaEntity> buscarPorDocumentoYAlcanceSucursal(@Param("documento") String documento, @Param("idSucursal") String idSucursal);
}
