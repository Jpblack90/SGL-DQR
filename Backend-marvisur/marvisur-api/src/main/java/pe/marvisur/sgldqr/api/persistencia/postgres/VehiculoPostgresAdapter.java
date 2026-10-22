package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.VehiculoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.VehiculoMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.VehiculoSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.Vehiculo;
import pe.marvisur.sgldqr.puertos.RepositorioVehiculoPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para la entidad Vehiculo.
 */
@Component
public class VehiculoPostgresAdapter implements RepositorioVehiculoPort {

    private final VehiculoSpringDataRepository repository;

    public VehiculoPostgresAdapter(VehiculoSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo, "El vehículo no puede ser nulo");
        VehiculoJpaEntity entidad = VehiculoMapper.aEntidad(vehiculo);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehiculo> buscarPorId(String idVehiculo) {
        Objects.requireNonNull(idVehiculo, "El idVehiculo no puede ser nulo");
        return repository.findById(idVehiculo).map(VehiculoMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehiculo> buscarPorPlaca(String placa) {
        Objects.requireNonNull(placa, "La placa no puede ser nula");
        return repository.findByPlaca(placa).map(VehiculoMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehiculo> listarTodos() {
        return repository.findAll().stream().map(VehiculoMapper::aDominio).toList();
    }
}
