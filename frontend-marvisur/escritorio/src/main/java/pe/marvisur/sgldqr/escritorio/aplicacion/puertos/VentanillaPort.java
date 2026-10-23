package pe.marvisur.sgldqr.escritorio.aplicacion.puertos;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CotizacionComando;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CotizacionResultado;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EncomiendaRegistradaResultado;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PreRegistroDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RegistrarEncomiendaComando;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.SucursalDto;

import java.util.List;

/**
 * Puerto para las operaciones de ventanilla: consulta de agencias, cotización,
 * precarga de Código Express y registro de encomienda con Doble QR.
 */
public interface VentanillaPort {

    /**
     * Obtiene la lista de agencias activas en el sistema.
     */
    List<SucursalDto> listarSucursales();

    /**
     * Solicita el cálculo estimado de tarifa según peso y dimensiones.
     */
    CotizacionResultado cotizar(CotizacionComando comando);

    /**
     * Consulta los datos de un pre-registro express mediante su código legible (ej. CEX-481920).
     */
    PreRegistroDto consultarPreRegistro(String codigoExpress);

    /**
     * Registra una nueva encomienda emitiendo el par Doble QR y comprobante.
     */
    EncomiendaRegistradaResultado registrarEncomienda(RegistrarEncomiendaComando comando);
}
