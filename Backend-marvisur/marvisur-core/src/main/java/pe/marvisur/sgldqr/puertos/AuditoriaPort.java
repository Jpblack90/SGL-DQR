package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.AuditoriaLog;

import java.util.List;

/**
 * Puerto para el registro y consulta de auditoría operativa del sistema.
 */
public interface AuditoriaPort {

    void registrar(AuditoriaLog log);

    default void registrar(String idEmpleado, String accion, String tablaAfectada, String idRegistroAfectado, String ipOrigen) {
        registrar(AuditoriaLog.crear(idEmpleado, accion, tablaAfectada, idRegistroAfectado, ipOrigen, java.time.LocalDateTime.now()));
    }

    List<AuditoriaLog> buscarPorRegistro(String tablaAfectada, String idRegistroAfectado);
}
