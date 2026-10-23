package pe.marvisur.sgldqr.escritorio.aplicacion.puertos;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.*;

import java.util.List;

/**
 * Puerto para las operaciones del módulo de almacén:
 * validación cruzada Doble QR, entrega física, cobro Contra Entrega,
 * escaneo masivo de lotes, búsqueda de emergencia e incidencias de etiquetas.
 */
public interface AlmacenPort {

    /**
     * Valida el QR1 operativo del bulto frente al QR2 digital del cliente.
     * Retorna el resultado estructurado tanto para 200 (EXITOSO) como para 409 (PAGO_PENDIENTE, TOKEN_INVALIDO, TRANSICION_INVALIDA) y 404.
     */
    ValidacionEntregaResultado validarEntrega(String qr1, String qr2);

    /**
     * Registra el cobro de una encomienda en modalidad Contra Entrega, desbloqueándola.
     */
    RespuestaCobroDto registrarCobro(String idEncomienda);

    /**
     * Lista los viajes programados filtrados por estado y sucursal de origen.
     */
    List<ViajeDto> listarViajes(String estado, String idSucursalOrigen);

    /**
     * Ejecuta el procesamiento masivo de un lote de códigos QR1.
     */
    RespuestaLoteEscaneoDto procesarLote(LoteEscaneoComando comando);

    /**
     * Localiza encomiendas no entregadas asociadas al número de documento (DNI o RUC).
     */
    List<EncomiendaBusquedaDto> buscarEncomiendasPorDocumento(String documento);

    /**
     * Reporta una incidencia de etiqueta dañada o perdida y obtiene el QR1 operativo regenerado.
     */
    RespuestaIncidenciaDto reportarIncidencia(RegistrarIncidenciaComando comando);

    /**
     * Lista las incidencias registradas, opcionalmente filtradas por encomienda.
     */
    List<IncidenciaHistorialDto> listarIncidencias(String idEncomienda);
}
