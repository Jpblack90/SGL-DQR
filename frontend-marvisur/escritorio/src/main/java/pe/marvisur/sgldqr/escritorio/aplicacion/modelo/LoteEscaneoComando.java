package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.util.List;

/**
 * Parámetros para procesar un lote de códigos QR1 en almacén.
 * <p>
 * REGLA DE SEGURIDAD: Los identificadores de QR1 se enmascaran en {@code toString()}
 * para evitar registros masivos de trazabilidad en archivos de log locales.
 * </p>
 */
public record LoteEscaneoComando(
        String accion,
        String idViaje,
        List<String> qr1List
) {
    @Override
    public String toString() {
        int cantidad = qr1List != null ? qr1List.size() : 0;
        return "LoteEscaneoComando{" +
                "accion='" + accion + '\'' +
                ", idViaje='" + idViaje + '\'' +
                ", qr1List=[***" + cantidad + " items***]" +
                '}';
    }
}
