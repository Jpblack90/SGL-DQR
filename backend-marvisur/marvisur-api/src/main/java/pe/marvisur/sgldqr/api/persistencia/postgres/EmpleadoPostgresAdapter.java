package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.EmpleadoMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.Empleado;
import pe.marvisur.sgldqr.puertos.RepositorioEmpleadoPort;

import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para la entidad Empleado.
 */
@Component
public class EmpleadoPostgresAdapter implements RepositorioEmpleadoPort {

    private final EmpleadoSpringDataRepository repository;

    public EmpleadoPostgresAdapter(EmpleadoSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(Empleado empleado) {
        Objects.requireNonNull(empleado, "El empleado no puede ser nulo");
        EmpleadoJpaEntity entidad = EmpleadoMapper.aEntidad(empleado);
        repository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Empleado> buscarPorId(String idEmpleado) {
        Objects.requireNonNull(idEmpleado, "El idEmpleado no puede ser nulo");
        return repository.findById(idEmpleado)
                .map(EmpleadoMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Empleado> buscarPorDni(String dni) {
        Objects.requireNonNull(dni, "El dni no puede ser nulo");
        return repository.findByDni(dni)
                .map(EmpleadoMapper::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Empleado> buscarPorCorreo(String correo) {
        Objects.requireNonNull(correo, "El correo no puede ser nulo");
        return repository.findByCorreo(correo)
                .map(EmpleadoMapper::aDominio);
    }
}
