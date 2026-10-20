package pe.marvisur.core.aplicacion.usecase;

import pe.marvisur.core.aplicacion.puerto.RepositorioEncomiendaPort;
import pe.marvisur.core.dominio.modelo.Encomienda;

import java.util.List;
import java.util.Optional;

/**
 * Caso de uso: Consultar encomiendas del sistema SGL-DQR.
 *
 * <p>Proporciona operaciones de solo lectura para buscar encomiendas
 * por ID o listar todas las registradas.</p>
 */
public class ConsultarEncomiendaUseCase {

    // -------------------------------------------------------------------------
    // Puertos (dependencias inyectadas)
    // -------------------------------------------------------------------------

    private final RepositorioEncomiendaPort repositorioEncomienda;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * @param repositorioEncomienda puerto de persistencia de encomiendas
     */
    public ConsultarEncomiendaUseCase(RepositorioEncomiendaPort repositorioEncomienda) {
        this.repositorioEncomienda = repositorioEncomienda;
    }

    // -------------------------------------------------------------------------
    // Métodos del caso de uso
    // -------------------------------------------------------------------------

    /**
     * Busca una encomienda por su identificador UUID.
     *
     * @param id UUID de la encomienda
     * @return {@link Optional} con la encomienda si existe, vacío si no
     */
    public Optional<Encomienda> buscarPorId(String id) {
        return repositorioEncomienda.buscarPorId(id);
    }

    /**
     * Lista todas las encomiendas registradas en el sistema.
     *
     * @return lista de encomiendas (puede estar vacía, nunca {@code null})
     */
    public List<Encomienda> listarTodos() {
        return repositorioEncomienda.listarTodos();
    }
}
