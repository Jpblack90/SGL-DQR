package pe.marvisur.sgldqr.api.persistencia.mapeador;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Utilitario para conversión entre LocalDateTime de dominio y OffsetDateTime de persistencia JPA.
 */
public final class FechaMapperHelper {

    private FechaMapperHelper() {
    }

    public static OffsetDateTime aOffsetDateTime(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    public static LocalDateTime aLocalDateTime(OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDateTime();
    }
}
