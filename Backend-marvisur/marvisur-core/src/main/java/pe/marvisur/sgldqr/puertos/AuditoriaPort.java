package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.AuditoriaLog;

import java.util.List;

/**
 * Puerto para el registro y consulta de auditoría operativa del sistema.
 */
public interface AuditoriaPort {

    void registrar(AuditoriaLog log);

    List<AuditoriaLog> buscarPorRegistro(String tablaAfectada, String idRegistroAfectado);
}
