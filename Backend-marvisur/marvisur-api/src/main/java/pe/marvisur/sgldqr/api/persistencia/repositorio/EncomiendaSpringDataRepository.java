package pe.marvisur.sgldqr.api.persistencia.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.marvisur.sgldqr.api.persistencia.entidad.RolJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.TokenQr2EstatusJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ValidacionCruzadaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.HistorialTrackingJpaEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncomiendaSpringDataRepository extends JpaRepository<EncomiendaJpaEntity, String> {
    Optional<EncomiendaJpaEntity> findByCodigoComprobante(String codigoComprobante);
    List<EncomiendaJpaEntity> findByIdSucursalDestinoAndEstadoLogistico(String idSucursalDestino, String estadoLogistico);
}
