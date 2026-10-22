package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.DespachoLoteDetalleJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.DespachoLoteDetalleMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.DespachoLoteDetalleSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.DespachoLoteDetalle;
import pe.marvisur.sgldqr.puertos.RepositorioDespachoLoteDetallePort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para la entidad DespachoLoteDetalle.
 */
@Component
public class DespachoLoteDetallePostgresAdapter implements RepositorioDespachoLoteDetallePort {

    private final DespachoLoteDetalleSpringDataRepository repository;

    public DespachoLoteDetallePostgresAdapter(DespachoLoteDetalleSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(DespachoLoteDetalle detalle) {
        Objects.requireNonNull(detalle, "El detalle de despacho no puede ser nulo");
        DespachoLoteDetalleJpaEntity entidad = DespachoLoteDetalleMapper.aEntidad(detalle);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DespachoLoteDetalle> buscarPorId(String idDespachoDetalle) {
        Objects.requireNonNull(idDespachoDetalle, "El idDespachoDetalle no puede ser nulo");
        return repository.findById(idDespachoDetalle).map(DespachoLoteDetalleMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DespachoLoteDetalle> listarPorViaje(String idViaje) {
        Objects.requireNonNull(idViaje, "El idViaje no puede ser nulo");
        return repository.findByIdViaje(idViaje).stream().map(DespachoLoteDetalleMapper::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeEncomiendaEnViajeActivo(String idEncomienda) {
        Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        return repository.existeEncomiendaEnViajeActivo(idEncomienda);
    }
}
