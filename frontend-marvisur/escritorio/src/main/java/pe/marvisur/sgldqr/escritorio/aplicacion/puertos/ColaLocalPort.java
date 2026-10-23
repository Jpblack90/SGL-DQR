package pe.marvisur.sgldqr.escritorio.aplicacion.puertos;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ElementoColaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.SucursalDto;

import java.util.List;

/**
 * Puerto de acceso a la persistencia local SQLite para cola de registros offline y caché de sucursales.
 */
public interface ColaLocalPort {

    /**
     * Guarda una nueva encomienda en la cola de sincronización con estado PENDIENTE.
     */
    void guardarEnCola(String id, String payloadJson);

    /**
     * Lista los elementos pendientes de sincronización ordenados por fecha de creación (FIFO).
     */
    List<ElementoColaDto> listarPendientes(int limite);

    /**
     * Lista todos los elementos de la cola (para visualización en pantalla administrativa).
     */
    List<ElementoColaDto> listarTodos();

    /**
     * Marca un elemento como SINCRONIZADA tras aceptación del servidor central.
     */
    void marcarSincronizada(String id);

    /**
     * Registra un error de sincronización, incrementando intentos y actualizando estado.
     *
     * @param id identificador de la encomienda
     * @param ultimoError descripción del error
     * @param reintentable si es true y no excede el límite se mantiene o pasa a estado reintentable
     */
    void marcarError(String id, String ultimoError, boolean reintentable);

    /**
     * Reinicia al estado PENDIENTE aquellos ítems en ERROR que sean reintentables.
     */
    void reiniciarErroresReintentables();

    /**
     * Retorna la cantidad de elementos pendientes de sincronizar en la cola local.
     */
    int contarPendientes();

    /**
     * Almacena o actualiza la lista de agencias en la caché local SQLite.
     */
    void guardarCacheSucursales(List<SucursalDto> sucursales);

    /**
     * Recupera las agencias desde la caché local SQLite cuando no hay conexión.
     */
    List<SucursalDto> obtenerCacheSucursales();
}
