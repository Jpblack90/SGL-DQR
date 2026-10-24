package pe.marvisur.sgldqr.api.servicio;

import org.springframework.stereotype.Component;
import pe.marvisur.sgldqr.api.dto.dashboard.PeriodoDashboard;
import pe.marvisur.sgldqr.api.dto.dashboard.RangoPeriodo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Servicio utilitario para el cálculo de rangos temporales en zonas horarias específicas (RF-02)
 * y evaluación de la regla de encomiendas estancadas sin movimiento.
 *
 * Dispone de Clock inyectable para facilitar pruebas unitarias deterministas sin base de datos.
 */
@Component
public class CalculadorPeriodoDashboard {

    private final Clock clock;

    public CalculadorPeriodoDashboard(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "El clock no puede ser nulo");
    }

    /**
     * Calcula el rango [inicio, fin) en la zona horaria indicada para el período dado.
     * El cálculo de "HOY" se basa en el inicio del día calendario local en dicha zona horaria,
     * garantizando que momentos cercanos a la medianoche no se desvíen por el desfase UTC del servidor.
     */
    public RangoPeriodo calcularRango(PeriodoDashboard periodo, ZoneId zona) {
        Objects.requireNonNull(periodo, "El periodo no puede ser nulo");
        Objects.requireNonNull(zona, "La zona no puede ser nula");

        ZonedDateTime ahoraEnZona = ZonedDateTime.now(clock.withZone(zona));
        LocalDate hoy = ahoraEnZona.toLocalDate();

        int dias = periodo.getDias();
        OffsetDateTime inicio = hoy.minusDays(dias - 1L)
                .atStartOfDay(zona)
                .toOffsetDateTime();

        OffsetDateTime fin = hoy.plusDays(1L)
                .atStartOfDay(zona)
                .toOffsetDateTime();

        return new RangoPeriodo(inicio, fin);
    }

    /**
     * Calcula la fecha/hora de corte para considerar una encomienda como estancada.
     */
    public OffsetDateTime calcularLimiteEstancado(int umbralHoras) {
        return OffsetDateTime.now(clock).minusHours(umbralHoras);
    }

    /**
     * Evalúa si una encomienda está estancada según la fecha de su último movimiento y el umbral configurado.
     */
    public boolean esEstancada(OffsetDateTime ultimoMovimiento, int umbralHoras) {
        if (ultimoMovimiento == null) {
            return false;
        }
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return esEstancada(ultimoMovimiento, ahora, umbralHoras);
    }

    /**
     * Función pura para evaluar estancamiento entre dos instantes.
     */
    public static boolean esEstancada(OffsetDateTime ultimoMovimiento, OffsetDateTime ahora, int umbralHoras) {
        if (ultimoMovimiento == null || ahora == null) {
            return false;
        }
        return !ultimoMovimiento.isAfter(ahora.minusHours(umbralHoras));
    }

    /**
     * Calcula las horas transcurridas sin movimiento desde el último evento hasta el instante actual del Clock.
     */
    public long calcularHorasSinMovimiento(OffsetDateTime ultimoMovimiento) {
        if (ultimoMovimiento == null) {
            return 0L;
        }
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        return calcularHorasSinMovimiento(ultimoMovimiento, ahora);
    }

    /**
     * Función pura para calcular horas transcurridas entre dos instantes.
     */
    public static long calcularHorasSinMovimiento(OffsetDateTime ultimoMovimiento, OffsetDateTime ahora) {
        if (ultimoMovimiento == null || ahora == null) {
            return 0L;
        }
        long horas = ChronoUnit.HOURS.between(ultimoMovimiento, ahora);
        return Math.max(0L, horas);
    }

    /**
     * Retorna el instante actual proyectado con la zona horaria especificada.
     */
    public OffsetDateTime obtenerAhoraConZona(ZoneId zona) {
        Objects.requireNonNull(zona, "La zona no puede ser nula");
        return ZonedDateTime.now(clock.withZone(zona)).toOffsetDateTime();
    }
}
