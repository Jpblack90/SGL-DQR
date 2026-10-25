package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Datos de un viaje programado para el despacho de lotes y gestión de flota (RF-05).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
        String estadoViaje,
        RutaDto ruta,
        String placa,
        String modelo
) {
    public String placaEfectiva() {
        return (placa != null && !placa.isBlank()) ? placa : (placaVehiculo != null ? placaVehiculo : "S/P");
    }

    public String rutaFormateada() {
        if (ruta != null && ruta.origen() != null && ruta.destino() != null) {
            return ruta.origen().nombreAgencia() + " ➔ " + ruta.destino().nombreAgencia();
        }
        return (idSucursalOrigen != null ? idSucursalOrigen : "ORIGEN") +
                " ➔ " + (idSucursalDestino != null ? idSucursalDestino : "DESTINO");
    }

    @Override
    public String toString() {
        return rutaFormateada() + " [" + placaEfectiva() +
                (nombreChofer != null ? " - " + nombreChofer : "") + "]";
    }
}
