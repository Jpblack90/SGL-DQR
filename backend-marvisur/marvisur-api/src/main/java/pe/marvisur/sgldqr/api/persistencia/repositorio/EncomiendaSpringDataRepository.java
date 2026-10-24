package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;

import java.time.OffsetDateTime;
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

    @Query("SELECT " +
           "COUNT(e) AS registradas, " +
           "COALESCE(SUM(CASE WHEN e.estadoLogistico = 'ENTREGADO' THEN 1L ELSE 0L END), 0L) AS entregadas, " +
           "COALESCE(SUM(CASE WHEN e.estadoLogistico = 'EN_RUTA' THEN 1L ELSE 0L END), 0L) AS enRuta, " +
           "COALESCE(SUM(CASE WHEN e.estadoPago = 'PENDIENTE' THEN 1L ELSE 0L END), 0L) AS pendientesCantidad, " +
           "COALESCE(SUM(CASE WHEN e.estadoPago = 'PENDIENTE' THEN e.montoTotal ELSE 0 END), 0) AS pendientesMonto, " +
           "COALESCE(SUM(CASE WHEN e.estadoPago = 'PAGADO' THEN e.montoTotal ELSE 0 END), 0) AS recaudado " +
           "FROM EncomiendaJpaEntity e " +
           "WHERE e.fechaRegistro >= :inicio AND e.fechaRegistro < :fin " +
           "AND (:idSucursal IS NULL OR e.idSucursalOrigen = :idSucursal)")
    TotalesDashboardProjection obtenerTotalesPeriodo(@Param("inicio") OffsetDateTime inicio,
                                                     @Param("fin") OffsetDateTime fin,
                                                     @Param("idSucursal") String idSucursal);

    @Query("SELECT COUNT(DISTINCT t.idEncomienda) " +
           "FROM HistorialTrackingJpaEntity t " +
           "JOIN EncomiendaJpaEntity e ON e.idEncomienda = t.idEncomienda " +
           "WHERE t.estadoEvento = 'ENTREGADO' " +
           "AND t.fechaHoraEvento >= :inicio AND t.fechaHoraEvento < :fin " +
           "AND (:idSucursal IS NULL OR e.idSucursalOrigen = :idSucursal)")
    long contarEntregadasEnPeriodo(@Param("inicio") OffsetDateTime inicio,
                                   @Param("fin") OffsetDateTime fin,
                                   @Param("idSucursal") String idSucursal);

    @Query("SELECT e.estadoLogistico AS estadoLogistico, COUNT(e) AS total " +
           "FROM EncomiendaJpaEntity e " +
           "WHERE (:idSucursal IS NULL OR e.idSucursalOrigen = :idSucursal) " +
           "GROUP BY e.estadoLogistico")
    List<ConteoEstadoLogisticoProjection> contarPorEstadoLogistico(@Param("idSucursal") String idSucursal);

    @Query("SELECT " +
           "s.idSucursal AS idSucursal, " +
           "s.nombreAgencia AS nombreAgencia, " +
           "COALESCE(SUM(CASE WHEN e.estadoPago = 'PAGADO' THEN e.montoTotal ELSE 0 END), 0) AS recaudado, " +
           "COALESCE(SUM(CASE WHEN e.estadoPago = 'PENDIENTE' THEN e.montoTotal ELSE 0 END), 0) AS pendiente " +
           "FROM SucursalJpaEntity s " +
           "LEFT JOIN EncomiendaJpaEntity e ON e.idSucursalOrigen = s.idSucursal " +
           "  AND e.fechaRegistro >= :inicio AND e.fechaRegistro < :fin " +
           "WHERE (:idSucursal IS NULL OR s.idSucursal = :idSucursal) " +
           "GROUP BY s.idSucursal, s.nombreAgencia " +
           "ORDER BY s.nombreAgencia ASC")
    List<RecaudacionSucursalProjection> obtenerRecaudacionPorSucursal(@Param("inicio") OffsetDateTime inicio,
                                                                      @Param("fin") OffsetDateTime fin,
                                                                      @Param("idSucursal") String idSucursal);

    @Query("SELECT " +
           "e.codigoComprobante AS codigoComprobante, " +
           "e.estadoLogistico AS estadoLogistico, " +
           "sDestino.nombreAgencia AS nombreAgenciaDestino, " +
           "COALESCE(MAX(t.fechaHoraEvento), e.fechaRegistro) AS ultimoMovimiento " +
           "FROM EncomiendaJpaEntity e " +
           "JOIN SucursalJpaEntity sDestino ON sDestino.idSucursal = e.idSucursalDestino " +
           "LEFT JOIN HistorialTrackingJpaEntity t ON t.idEncomienda = e.idEncomienda " +
           "WHERE e.estadoLogistico <> 'ENTREGADO' " +
           "AND (:idSucursal IS NULL OR e.idSucursalOrigen = :idSucursal) " +
           "GROUP BY e.idEncomienda, e.codigoComprobante, e.estadoLogistico, sDestino.nombreAgencia, e.fechaRegistro " +
           "HAVING COALESCE(MAX(t.fechaHoraEvento), e.fechaRegistro) <= :limiteEstancado " +
           "ORDER BY COALESCE(MAX(t.fechaHoraEvento), e.fechaRegistro) ASC")
    List<EncomiendaEstancadaProjection> buscarEstancadas(@Param("limiteEstancado") OffsetDateTime limiteEstancado,
                                                         @Param("idSucursal") String idSucursal,
                                                         Pageable pageable);
}
