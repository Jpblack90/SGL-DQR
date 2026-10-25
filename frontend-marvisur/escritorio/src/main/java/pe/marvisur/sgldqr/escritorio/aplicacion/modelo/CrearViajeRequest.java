package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Petición para programar un nuevo viaje interprovincial (RF-05).
 */
public record CrearViajeRequest(
        String idRuta,
        String idVehiculo,
        String idChofer,
        String fechaSalida,
        String fechaLlegadaEstimada
) {
}
