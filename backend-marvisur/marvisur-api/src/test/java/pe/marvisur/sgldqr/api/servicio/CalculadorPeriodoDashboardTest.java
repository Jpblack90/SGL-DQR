package pe.marvisur.sgldqr.api.servicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.api.dto.dashboard.PeriodoDashboard;
import pe.marvisur.sgldqr.api.dto.dashboard.RangoPeriodo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias — CalculadorPeriodoDashboard (RF-02)")
class CalculadorPeriodoDashboardTest {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Test
    @DisplayName("Debe calcular límites de HOY en America/Lima correctamente")
    void debeCalcularLimitesHoyEnAmericaLimaCorrectamente() {
        // 24 de Octubre de 2026 a las 15:30:00 en Lima (-05:00) = 20:30:00 UTC
        Instant instanteFijo = Instant.parse("2026-10-24T20:30:00Z");
        Clock clock = Clock.fixed(instanteFijo, ZONA_LIMA);
        CalculadorPeriodoDashboard calculador = new CalculadorPeriodoDashboard(clock);

        RangoPeriodo rango = calculador.calcularRango(PeriodoDashboard.HOY, ZONA_LIMA);

        assertThat(rango.inicio()).isEqualTo(OffsetDateTime.parse("2026-10-24T00:00:00-05:00"));
        assertThat(rango.fin()).isEqualTo(OffsetDateTime.parse("2026-10-25T00:00:00-05:00"));
        assertThat(Duration.between(rango.inicio(), rango.fin()).toDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("Debe calcular límites de SIETE_DIAS en America/Lima con ventana exacta de 7 días")
    void debeCalcularLimitesSieteDiasEnAmericaLimaCorrectamente() {
        Instant instanteFijo = Instant.parse("2026-10-24T20:30:00Z");
        Clock clock = Clock.fixed(instanteFijo, ZONA_LIMA);
        CalculadorPeriodoDashboard calculador = new CalculadorPeriodoDashboard(clock);

        RangoPeriodo rango = calculador.calcularRango(PeriodoDashboard.SIETE_DIAS, ZONA_LIMA);

        // 7 días calendario incluyendo hoy (24): 18, 19, 20, 21, 22, 23, 24
        assertThat(rango.inicio()).isEqualTo(OffsetDateTime.parse("2026-10-18T00:00:00-05:00"));
        assertThat(rango.fin()).isEqualTo(OffsetDateTime.parse("2026-10-25T00:00:00-05:00"));
        assertThat(Duration.between(rango.inicio(), rango.fin()).toDays()).isEqualTo(7);
        assertThat(Duration.between(rango.inicio(), rango.fin()).toHours()).isEqualTo(168);
    }

    @Test
    @DisplayName("Debe calcular límites de TREINTA_DIAS en America/Lima con ventana exacta de 30 días")
    void debeCalcularLimitesTreintaDiasEnAmericaLimaCorrectamente() {
        Instant instanteFijo = Instant.parse("2026-10-24T20:30:00Z");
        Clock clock = Clock.fixed(instanteFijo, ZONA_LIMA);
        CalculadorPeriodoDashboard calculador = new CalculadorPeriodoDashboard(clock);

        RangoPeriodo rango = calculador.calcularRango(PeriodoDashboard.TREINTA_DIAS, ZONA_LIMA);

        // 30 días calendario incluyendo hoy (24): desde 25 de septiembre hasta fin de 24 de octubre
        assertThat(rango.inicio()).isEqualTo(OffsetDateTime.parse("2026-09-25T00:00:00-05:00"));
        assertThat(rango.fin()).isEqualTo(OffsetDateTime.parse("2026-10-25T00:00:00-05:00"));
        assertThat(Duration.between(rango.inicio(), rango.fin()).toDays()).isEqualTo(30);
        assertThat(Duration.between(rango.inicio(), rango.fin()).toHours()).isEqualTo(720);
    }

    @Test
    @DisplayName("Debe manejar caso cerca de medianoche en Lima que en UTC cae al día siguiente")
    void debeManejarCasoCercaDeMedianocheLimaQueEnUtcCaeAlDiaSiguiente() {
        // En Lima: 2026-10-24 a las 23:30:00 (-05:00)
        // En UTC: 2026-10-25 a las 04:30:00Z (¡ya es 25 de octubre en UTC!)
        Instant instanteMedianoche = Instant.parse("2026-10-25T04:30:00Z");
        Clock clock = Clock.fixed(instanteMedianoche, ZoneOffset.UTC);
        CalculadorPeriodoDashboard calculador = new CalculadorPeriodoDashboard(clock);

        RangoPeriodo rango = calculador.calcularRango(PeriodoDashboard.HOY, ZONA_LIMA);

        // A pesar de que el reloj UTC está en día 25, en Lima sigue siendo 24
        assertThat(rango.inicio()).isEqualTo(OffsetDateTime.parse("2026-10-24T00:00:00-05:00"));
        assertThat(rango.fin()).isEqualTo(OffsetDateTime.parse("2026-10-25T00:00:00-05:00"));

        OffsetDateTime momentoActualLima = OffsetDateTime.ofInstant(instanteMedianoche, ZONA_LIMA);
        assertThat(momentoActualLima).isEqualTo(OffsetDateTime.parse("2026-10-24T23:30:00-05:00"));
        assertThat(momentoActualLima.isAfter(rango.inicio()) || momentoActualLima.isEqual(rango.inicio())).isTrue();
        assertThat(momentoActualLima.isBefore(rango.fin())).isTrue();
    }

    @Test
    @DisplayName("Debe evaluar la regla de estancamiento y cálculo de horas sin movimiento")
    void debeEvaluarReglaDeEstancamientoSegunUmbralHoras() {
        // Instante base: 2026-10-24T12:00:00-05:00
        Instant instanteBase = Instant.parse("2026-10-24T17:00:00Z");
        Clock clock = Clock.fixed(instanteBase, ZONA_LIMA);
        CalculadorPeriodoDashboard calculador = new CalculadorPeriodoDashboard(clock);

        int umbralHoras = 48;
        OffsetDateTime limiteEsperado = OffsetDateTime.parse("2026-10-22T12:00:00-05:00");
        assertThat(calculador.calcularLimiteEstancado(umbralHoras)).isEqualTo(limiteEsperado);

        // Caso 1: Encomienda con último movimiento hace 52 horas (estancada)
        OffsetDateTime mov52HorasAtras = OffsetDateTime.parse("2026-10-22T08:00:00-05:00");
        assertThat(calculador.esEstancada(mov52HorasAtras, umbralHoras)).isTrue();
        assertThat(calculador.calcularHorasSinMovimiento(mov52HorasAtras)).isEqualTo(52L);

        // Caso 2: Encomienda con último movimiento hace 24 horas (activa, no estancada)
        OffsetDateTime mov24HorasAtras = OffsetDateTime.parse("2026-10-23T12:00:00-05:00");
        assertThat(calculador.esEstancada(mov24HorasAtras, umbralHoras)).isFalse();
        assertThat(calculador.calcularHorasSinMovimiento(mov24HorasAtras)).isEqualTo(24L);

        // Caso 3: Encomienda exactamente en el umbral de 48 horas (estancada)
        OffsetDateTime mov48HorasAtras = OffsetDateTime.parse("2026-10-22T12:00:00-05:00");
        assertThat(calculador.esEstancada(mov48HorasAtras, umbralHoras)).isTrue();
        assertThat(calculador.calcularHorasSinMovimiento(mov48HorasAtras)).isEqualTo(48L);

        // Caso 4: Movimiento nulo
        assertThat(calculador.esEstancada(null, umbralHoras)).isFalse();
        assertThat(calculador.calcularHorasSinMovimiento(null)).isEqualTo(0L);
    }
}
