package pe.marvisur.sgldqr.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias de Serialización de Tracking y Eventos Públicos (sin Docker)")
class TrackingSerializacionTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Desactivar escritura de fechas como timestamps numéricos para garantizar formato ISO-8601
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("EventoTrackingPublicoDto: serializa fechaHora con offset UTC 'Z' preservando el instante")
    void testSerializarEventoTrackingConOffset() throws Exception {
        OffsetDateTime fechaUtc = OffsetDateTime.of(2026, 10, 23, 23, 3, 48, 646676000, ZoneOffset.UTC);
        EventoTrackingPublicoDto dto = new EventoTrackingPublicoDto(
                "RECEPCIONADO",
                "Agencia Central Lima",
                fechaUtc
        );

        String json = objectMapper.writeValueAsString(dto);
        JsonNode nodo = objectMapper.readTree(json);

        assertThat(nodo.has("fechaHora")).isTrue();
        String fechaHoraStr = nodo.get("fechaHora").asText();
        // Debe contener la 'Z' de UTC y la hora exacta en UTC
        assertThat(fechaHoraStr).contains("Z");
        assertThat(fechaHoraStr).startsWith("2026-10-23T23:03:48");

        // Deserialización consistente a OffsetDateTime
        EventoTrackingPublicoDto deserializado = objectMapper.readValue(json, EventoTrackingPublicoDto.class);
        assertThat(deserializado.fechaHora().toInstant()).isEqualTo(fechaUtc.toInstant());
        assertThat(deserializado.estadoEvento()).isEqualTo("RECEPCIONADO");
    }

    @Test
    @DisplayName("RastreoPublicoDto con evento PAGO_REGISTRADO: serializa correctamente el nuevo estado de cobro")
    void testSerializarRastreoConPagoRegistrado() throws Exception {
        OffsetDateTime fechaPago = OffsetDateTime.of(2026, 10, 23, 20, 15, 0, 0, ZoneOffset.UTC);
        EventoTrackingPublicoDto eventoPago = new EventoTrackingPublicoDto(
                "PAGO_REGISTRADO",
                "Agencia Arequipa",
                fechaPago
        );

        RastreoPublicoDto rastreo = new RastreoPublicoDto(
                "MV-AQP-20261023-0001",
                "LISTO_PARA_RETIRO",
                "PAGADO",
                new BigDecimal("25.50"),
                "Agencia Lima",
                "Lima",
                "Agencia Arequipa",
                "Arequipa",
                "J*** P****",
                "M*** G****",
                List.of(eventoPago)
        );

        String json = objectMapper.writeValueAsString(rastreo);
        JsonNode nodo = objectMapper.readTree(json);

        assertThat(nodo.get("lineaDeTiempo").isArray()).isTrue();
        assertThat(nodo.get("lineaDeTiempo").size()).isEqualTo(1);
        JsonNode eventoNodo = nodo.get("lineaDeTiempo").get(0);
        assertThat(eventoNodo.get("estadoEvento").asText()).isEqualTo("PAGO_REGISTRADO");
        assertThat(eventoNodo.get("fechaHora").asText()).contains("Z");
    }

    @Test
    @DisplayName("RespuestaPreRegistroPublicoDto: vigencia conserva LocalDateTime sin romperse")
    void testVigenciaPreRegistroConservaLocalDateTime() throws Exception {
        LocalDateTime expiracion = LocalDateTime.of(2026, 10, 25, 18, 0, 0);
        RespuestaPreRegistroPublicoDto preRegistro = new RespuestaPreRegistroPublicoDto(
                "EXP-987654",
                expiracion,
                new BigDecimal("15.00"),
                new BigDecimal("2.50"),
                "Agencia Cusco",
                "C*** R****",
                "A*** M****"
        );

        String json = objectMapper.writeValueAsString(preRegistro);
        JsonNode nodo = objectMapper.readTree(json);

        assertThat(nodo.has("fechaExpiracion")).isTrue();
        assertThat(nodo.get("fechaExpiracion").asText()).isEqualTo("2026-10-25T18:00:00");
    }
}
