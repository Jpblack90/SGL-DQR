package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Datos de una ruta interprovincial de transporte (RF-05).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RutaDto(
        String id,
        AgenciaResumenDto origen,
        AgenciaResumenDto destino,
        BigDecimal distanciaKm
) {
    public String idRuta() {
        return id;
    }

    public String formatoCombo() {
        String nomOrigen = origen != null && origen.nombreAgencia() != null ? origen.nombreAgencia() : "Origen";
        String nomDestino = destino != null && destino.nombreAgencia() != null ? destino.nombreAgencia() : "Destino";
        String dist = distanciaKm != null ? String.format(java.util.Locale.US, " (%.2f km)", distanciaKm.doubleValue()) : "";
        return nomOrigen + " ➔ " + nomDestino + dist;
    }

    @Override
    public String toString() {
        return formatoCombo();
    }
}
