package pe.marvisur.core.aplicacion.puerto;

import pe.marvisur.core.dominio.modelo.Encomienda;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para la entidad {@link Encomienda}.
 *
 * <p>Define el contrato que deben implementar los adaptadores de persistencia
 * (SQLite para la app de escritorio, PostgreSQL para la API REST).
 * Pertenece a la capa de aplicación siguiendo el patrón de arquitectura hexagonal.</p>
 */
public interface RepositorioEncomiendaPort {

    /**
     * Persiste una nueva encomienda en el almacén de datos.
     *
     * @param encomienda encomienda a guardar (no debe existir previamente)
     */
    void guardar(Encomienda encomienda);

    /**
     * Busca una encomienda por su identificador UUID.
     *
     * @param id identificador UUID de la encomienda
     * @return {@link Optional} con la encomienda si existe, vacío si no
     */
    Optional<Encomienda> buscarPorId(String id);

    /**
     * Retorna todas las encomiendas almacenadas.
     *
     * @return lista de encomiendas (puede estar vacía, nunca {@code null})
     */
    List<Encomienda> listarTodos();

    /**
     * Actualiza los datos de una encomienda existente.
     *
     * @param encomienda encomienda con los datos actualizados
     */
    void actualizar(Encomienda encomienda);
}
