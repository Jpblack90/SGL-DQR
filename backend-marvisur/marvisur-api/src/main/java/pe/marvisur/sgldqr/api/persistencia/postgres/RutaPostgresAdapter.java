package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.RutaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.RutaMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RutaSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.Ruta;
import pe.marvisur.sgldqr.puertos.RepositorioRutaPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para la entidad Ruta.
 */
@Component
public class RutaPostgresAdapter implements RepositorioRutaPort {

    private final RutaSpringDataRepository repository;

    public RutaPostgresAdapter(RutaSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(Ruta ruta) {
        Objects.requireNonNull(ruta, "La ruta no puede ser nula");
        RutaJpaEntity entidad = RutaMapper.aEntidad(ruta);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Ruta> buscarPorId(String idRuta) {
        Objects.requireNonNull(idRuta, "El idRuta no puede ser nulo");
        return repository.findById(idRuta).map(RutaMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Ruta> listarTodas() {
        return repository.findAll().stream().map(RutaMapper::aDominio).toList();
    }
}
