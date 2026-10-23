package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Datos de un viaje programado para el despacho de lotes.
 */
public record ViajeDto(
        String idViaje,
        String idRuta,
        String idSucursalOrigen,
        String idSucursalDestino,
        Double distanciaKm,
        String idVehiculo,
        String placaVehiculo,
        String idChofer,
        String nombreChofer,
        String fechaSalida,
        String fechaLlegadaEstimada,
        String estadoViaje
) {
    @Override
    public String toString() {
        return (idSucursalOrigen != null ? idSucursalOrigen : "ORIGEN") +
                " ➔ " + (idSucursalDestino != null ? idSucursalDestino : "DESTINO") +
                " [" + (placaVehiculo != null ? placaVehiculo : "S/P") +
                (nombreChofer != null ? " - " + nombreChofer : "") + "]";
    }
}
