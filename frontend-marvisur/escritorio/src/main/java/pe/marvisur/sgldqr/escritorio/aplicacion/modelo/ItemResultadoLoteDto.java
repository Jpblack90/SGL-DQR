package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Resultado individual de un ítem en un lote de escaneo.
 */
public record ItemResultadoLoteDto(
        String qr1,
        String idEncomienda,
        String resultado,
        String motivo
) {
    public boolean esExitoso() {
        return "OK".equalsIgnoreCase(resultado);
    }

    public boolean esYaRegistrado() {
        return "YA_REGISTRADA".equalsIgnoreCase(resultado);
    }

    public boolean esError() {
        return "ERROR".equalsIgnoreCase(resultado);
    }

    public String getMotivoLegible() {
        if (motivo == null) {
            return esExitoso() ? "Transición exitosa" : (esYaRegistrado() ? "Ya procesado previamente" : "");
        }
        return switch (motivo) {
            case "QR1_INEXISTENTE" -> "QR1 no encontrado en el sistema";
            case "ESTADO_INVALIDO" -> "Estado logístico no admite esta acción";
            case "SUCURSAL_NO_CORRESPONDE" -> "No pertenece a esta sucursal de origen";
            case "DESTINO_NO_COINCIDE" -> "El destino del viaje no coincide con el paquete";
            case "YA_DESPACHADA" -> "La encomienda ya fue asignada a viaje";
            case "VIAJE_INEXISTENTE" -> "Viaje especificado no encontrado o finalizado";
            default -> motivo;
        };
    }
}
