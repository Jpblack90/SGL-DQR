package pe.marvisur.sgldqr.escritorio.aplicacion.puertos;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RespuestaSyncLoteDto;

/**
 * Puerto para la sincronización centralizada de lotes de registros creados offline (B4, O2).
 */
public interface SyncPort {

    /**
     * Envía un lote de encomiendas offline al backend central (POST /api/v1/sync/encomiendas).
     *
     * @param jsonLote JSON que contiene los registros a sincronizar (formato arreglo o { "items": [...] }).
     * @return resumen y resultados individuales de la sincronización.
     */
    RespuestaSyncLoteDto sincronizarLote(String jsonLote);
}
