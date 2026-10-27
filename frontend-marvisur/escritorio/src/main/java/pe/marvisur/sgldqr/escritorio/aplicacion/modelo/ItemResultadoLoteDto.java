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
        return getMotivoLegible(null);
    }

    public String getMotivoLegible(String accionLote) {
        if (motivo == null || motivo.isBlank()) {
            return esExitoso() ? "Transición exitosa" : (esYaRegistrado() ? "Ya procesado previamente" : "");
        }
        return traducirMotivo(motivo, accionLote);
    }

    public static String traducirMotivo(String motivo, String accionLote) {
        if (motivo == null || motivo.isBlank()) {
            return "";
        }
        String clave = motivo.trim().toUpperCase();
        return switch (clave) {
            case "ESTADO_INVALIDO" -> traducirEstadoInvalido(accionLote);
            case "QR1_INEXISTENTE" -> "Código QR1 no encontrado en el sistema";
            case "SUCURSAL_NO_CORRESPONDE" -> "No pertenece a esta sucursal de origen";
            case "DESTINO_NO_COINCIDE" -> "El destino del viaje no coincide con el paquete";
            case "YA_DESPACHADA" -> "La encomienda ya fue asignada a viaje";
            case "VIAJE_INEXISTENTE" -> "Viaje especificado no encontrado o finalizado";
            default -> motivo;
        };
    }

    private static String traducirEstadoInvalido(String accionLote) {
        if (accionLote == null) {
            return "Estado logístico no admite esta acción";
        }
        String acc = accionLote.trim().toUpperCase();
        if (acc.contains("RECEPCIONAR_ORIGEN") || acc.contains("ORIGEN")) {
            return "«Recepcionar en origen» exige envíos en estado Registrado";
        }
        if (acc.contains("DESPACHAR")) {
            return "«Despachar a viaje» exige En almacén origen";
        }
        if (acc.contains("RECEPCIONAR_DESTINO") || acc.contains("DESTINO")) {
            return "«Recepcionar en destino» exige En ruta (y que quien recepciona sea de la agencia de destino)";
        }
        return "Estado logístico no admite esta acción";
    }
}
