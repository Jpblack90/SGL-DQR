package pe.marvisur.sgldqr.api.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Petición de sincronización de un lote de encomiendas registradas offline.
 * Admite tanto un arreglo JSON directo [ {...} ] como un objeto envolvente { "items": [ {...} ] }.
 */
@Schema(description = "Lote de encomiendas registradas sin conexión para sincronización central")
@JsonDeserialize(using = SyncLoteRequest.Deserializador.class)
public class SyncLoteRequest {

    @ArraySchema(schema = @Schema(implementation = SyncEncomiendaItemDto.class))
    private List<SyncEncomiendaItemDto> items = new ArrayList<>();

    public SyncLoteRequest() {
    }

    public SyncLoteRequest(List<SyncEncomiendaItemDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public List<SyncEncomiendaItemDto> getItems() {
        return items;
    }

    public void setItems(List<SyncEncomiendaItemDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public static class Deserializador extends JsonDeserializer<SyncLoteRequest> {
        @Override
        public SyncLoteRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode root = mapper.readTree(p);
            List<SyncEncomiendaItemDto> lista = new ArrayList<>();

            if (root != null) {
                if (root.isArray()) {
                    lista = mapper.convertValue(root, new TypeReference<List<SyncEncomiendaItemDto>>() {});
                } else if (root.isObject() && root.has("items") && root.get("items").isArray()) {
                    lista = mapper.convertValue(root.get("items"), new TypeReference<List<SyncEncomiendaItemDto>>() {});
                }
            }

            return new SyncLoteRequest(lista);
        }
    }
}
