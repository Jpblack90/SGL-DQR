package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.HistorialTracking;

import java.util.List;

/**
 * Puerto para el registro de eventos y consulta de la línea de tiempo de encomiendas.
 */
public interface HistorialTrackingPort {

    void registrarEvento(HistorialTracking evento);

    List<HistorialTracking> listarPorEncomienda(String idEncomienda);
}
