package pe.marvisur.sgldqr.escritorio.aplicacion.puertos;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CambiarEstadoViajeRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CrearVehiculoRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.CrearViajeRequest;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ManifiestoEncomiendaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RutaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.VehiculoDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ViajeDto;

import java.util.List;

/**
 * Puerto de operaciones para la gestión de flota de vehículos, rutas y viajes interprovinciales (RF-05).
 */
public interface FlotaPort {

    List<VehiculoDto> listarVehiculos();

    VehiculoDto registrarVehiculo(CrearVehiculoRequest request);

    List<RutaDto> listarRutas();

    List<ViajeDto> listarViajes(String estado, String idSucursalOrigen, String idChofer);

    ViajeDto crearViaje(CrearViajeRequest request);

    ViajeDto cambiarEstadoViaje(String idViaje, CambiarEstadoViajeRequest request);

    List<ManifiestoEncomiendaDto> obtenerManifiesto(String idViaje);
}
