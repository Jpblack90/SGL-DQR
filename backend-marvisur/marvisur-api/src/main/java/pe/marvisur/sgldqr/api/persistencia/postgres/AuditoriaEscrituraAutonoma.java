package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.mapeador.AuditoriaMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.AuditoriaLog;

import java.util.Objects;

/**
 * Componente interno para persistencia de auditoría en transacciones autónomas (REQUIRES_NEW).
 * Utilizado exclusivamente para eventos de autenticación (LOGIN_EXITOSO y LOGIN_FALLIDO).
 */
@Component
public class AuditoriaEscrituraAutonoma {

    private final AuditoriaLogSpringDataRepository repository;

    public AuditoriaEscrituraAutonoma(AuditoriaLogSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarEnTransaccionAutonoma(AuditoriaLog log) {
        Objects.requireNonNull(log, "El log no puede ser nulo");
        repository.save(AuditoriaMapper.aEntidad(log));
    }
}
