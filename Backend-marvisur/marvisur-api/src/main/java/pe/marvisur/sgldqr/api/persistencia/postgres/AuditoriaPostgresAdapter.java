package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.AuditoriaMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.AuditoriaLog;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Adaptador de persistencia PostgreSQL para auditoría operativa y de seguridad.
 *
 * NOTA ARQUITECTÓNICA DE TRANSACCIONALIDAD:
 * - Eventos de autenticación (LOGIN_EXITOSO, LOGIN_FALLIDO): usan transacciones autónomas (REQUIRES_NEW)
 *   para garantizar que los intentos de acceso queden auditados incluso ante fallos de login.
 * - Eventos de negocio (REGISTRO_ENCOMIENDA, COBRO_CONTRA_ENTREGA, VALIDACION_ENTREGA, etc.):
 *   participan en la MISMA transacción de la operación de negocio (REQUIRED) para garantizar atomicidad
 *   completa (si la operación hace rollback, la fila de auditoría también se revierte).
 */
@Component
public class AuditoriaPostgresAdapter implements AuditoriaPort {

    private static final Set<String> ACCIONES_AUTONOMAS = Set.of("LOGIN_EXITOSO", "LOGIN_FALLIDO");

    private final AuditoriaLogSpringDataRepository repository;
    private final AuditoriaEscritorAutonomo escritorAutonomo;

    public AuditoriaPostgresAdapter(AuditoriaLogSpringDataRepository repository,
                                    AuditoriaEscritorAutonomo escritorAutonomo) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
        this.escritorAutonomo = Objects.requireNonNull(escritorAutonomo, "El escritorAutonomo no puede ser nulo");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(AuditoriaLog log) {
        Objects.requireNonNull(log, "El log no puede ser nulo");
        if (ACCIONES_AUTONOMAS.contains(log.getAccion())) {
            escritorAutonomo.guardarEnTransaccionAutonoma(log);
        } else {
            AuditoriaLogJpaEntity entidad = AuditoriaMapper.aEntidad(log);
            repository.save(entidad);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void registrar(String idEmpleado, String accion, String tablaAfectada, String idRegistroAfectado, String ipOrigen) {
        AuditoriaLog log = AuditoriaLog.crear(idEmpleado, accion, tablaAfectada, idRegistroAfectado, ipOrigen, java.time.LocalDateTime.now());
        registrar(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaLog> buscarPorRegistro(String tablaAfectada, String idRegistroAfectado) {
        Objects.requireNonNull(tablaAfectada, "La tablaAfectada no puede ser nula");
        Objects.requireNonNull(idRegistroAfectado, "El idRegistroAfectado no puede ser nulo");
        return repository.findByTablaAfectadaAndIdRegistroAfectadoOrderByFechaHoraDesc(tablaAfectada, idRegistroAfectado)
                .stream()
                .map(AuditoriaMapper::aDominio)
                .toList();
    }
}
