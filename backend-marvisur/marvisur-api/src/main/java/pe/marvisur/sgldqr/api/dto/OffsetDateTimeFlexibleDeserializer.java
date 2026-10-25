package pe.marvisur.sgldqr.api.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Deserializador flexible para campos OffsetDateTime.
 * Permite procesar tanto cadenas ISO 8601 completas con offset (ej. 2026-10-25T20:00:00-05:00 o Z)
 * como cadenas LocalDateTime sin offset (ej. 2026-10-25T20:00:00) asignándoles la zona horaria oficial America/Lima.
 */
public class OffsetDateTimeFlexibleDeserializer extends JsonDeserializer<OffsetDateTime> {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        try {
            return OffsetDateTime.parse(trimmed);
        } catch (DateTimeParseException e) {
            LocalDateTime ldt = LocalDateTime.parse(trimmed);
            return ldt.atZone(ZONA_LIMA).toOffsetDateTime();
        }
    }
}
