package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.PreRegistroExpress;

import java.util.Optional;

/**
 * Puerto de repositorio para la persistencia y consulta de pre-registros web ('Código Express').
 */
public interface RepositorioPreRegistroExpressPort {

    void guardar(PreRegistroExpress preRegistro);

    void actualizar(PreRegistroExpress preRegistro);

    Optional<PreRegistroExpress> buscarPorId(String idPreRegistro);

    Optional<PreRegistroExpress> buscarPorCodigoExpress(String codigoExpress);

    boolean existePorCodigoExpress(String codigoExpress);
}
