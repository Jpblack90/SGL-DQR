package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Solicitud de procesamiento masivo por lote de escaneo en almacén.
 */
@Schema(description = "Solicitud de escaneo masivo por lote de etiquetas QR1 en almacén")
public class LoteEscaneoRequest {

    @NotNull(message = "La acción de lote es obligatoria")
    @Schema(description = "Acción a ejecutar sobre el lote de encomiendas", example = "RECEPCIONAR_ORIGEN")
    private AccionLoteEscaneo accion;

    @Schema(description = "Identificador del viaje (obligatorio únicamente para la acción DESPACHAR_A_VIAJE)", example = "770e8400-e29b-41d4-a716-446655440000")
    private String idViaje;

    @NotEmpty(message = "La lista de códigos QR1 no puede estar vacía")
    @Schema(description = "Lista de identificadores o payloads de QR1 leídos en el lote", example = "[\"MV-QR1-1\", \"MV-QR1-2\"]")
    private List<String> qr1List;

    public LoteEscaneoRequest() {
    }

    public LoteEscaneoRequest(AccionLoteEscaneo accion, String idViaje, List<String> qr1List) {
        this.accion = accion;
        this.idViaje = idViaje;
        this.qr1List = qr1List;
    }

    public AccionLoteEscaneo getAccion() {
        return accion;
    }

    public void setAccion(AccionLoteEscaneo accion) {
        this.accion = accion;
    }

    public String getIdViaje() {
        return idViaje;
    }

    public void setIdViaje(String idViaje) {
        this.idViaje = idViaje;
    }

    public List<String> getQr1List() {
        return qr1List;
    }

    public void setQr1List(List<String> qr1List) {
        this.qr1List = qr1List;
    }
}
