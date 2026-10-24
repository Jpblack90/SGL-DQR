package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.ValidacionCruzadaLogJpaEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ValidacionCruzadaLogSpringDataRepository extends JpaRepository<ValidacionCruzadaLogJpaEntity, String> {
    List<ValidacionCruzadaLogJpaEntity> findByIdParejaQrOrderByFechaValidacionDesc(String idParejaQr);

    @Query("SELECT " +
           "COUNT(v) AS total, " +
           "COALESCE(SUM(CASE WHEN v.resultado = 'EXITOSO' THEN 1L ELSE 0L END), 0L) AS exitosas, " +
           "COALESCE(SUM(CASE WHEN v.resultado = 'FALLIDO_PAGO_PENDIENTE' THEN 1L ELSE 0L END), 0L) AS rechazadasPagoPendiente, " +
           "COALESCE(SUM(CASE WHEN v.resultado = 'FALLIDO_TOKEN_INVALIDO' THEN 1L ELSE 0L END), 0L) AS rechazadasTokenInvalido, " +
           "COALESCE(AVG(v.latenciaProcesamientoMs), 0.0) AS latenciaPromedioMs, " +
           "COALESCE(MAX(v.latenciaProcesamientoMs), 0) AS latenciaMaximaMs " +
           "FROM ValidacionCruzadaLogJpaEntity v " +
           "WHERE v.fechaValidacion >= :inicio AND v.fechaValidacion < :fin " +
           "AND (:idSucursal IS NULL OR EXISTS (" +
           "  SELECT 1 FROM ParDobleQrJpaEntity p, EncomiendaJpaEntity e " +
           "  WHERE p.idParejaQr = v.idParejaQr AND e.idEncomienda = p.idEncomienda AND e.idSucursalOrigen = :idSucursal" +
           "))")
    MetricasValidacionProjection obtenerMetricasValidacion(@Param("inicio") OffsetDateTime inicio,
                                                           @Param("fin") OffsetDateTime fin,
                                                           @Param("idSucursal") String idSucursal);
}
