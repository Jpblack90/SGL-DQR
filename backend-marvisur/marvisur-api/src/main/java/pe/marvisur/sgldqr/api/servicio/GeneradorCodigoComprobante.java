package pe.marvisur.sgldqr.api.servicio;

import org.springframework.stereotype.Component;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Generador provisional de código de comprobante para encomiendas según regla P7.
 * Formato: "MV-" + yyyyMMdd + "-" + 8 caracteres aleatorios.
 */
@Component
public class GeneradorCodigoComprobante {

    // TODO: definir numeración oficial de comprobantes con Marvisur

    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int INTENTOS_MAXIMOS = 10;

    private final SecureRandom random = new SecureRandom();
    private final EncomiendaSpringDataRepository repository;

    public GeneradorCodigoComprobante(EncomiendaSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository, "El repository no puede ser nulo");
    }

    public String generarCodigoUnico() {
        String prefijoFecha = "MV-" + FORMATO_FECHA.format(LocalDate.now()) + "-";

        for (int intento = 0; intento < INTENTOS_MAXIMOS; intento++) {
            StringBuilder aleatorio = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                aleatorio.append(CARACTERES.charAt(random.nextInt(CARACTERES.length())));
            }
            String candidato = prefijoFecha + aleatorio;

            if (!repository.existsByCodigoComprobante(candidato)) {
                return candidato;
            }
        }

        throw new IllegalStateException("No se pudo generar un código de comprobante único tras " + INTENTOS_MAXIMOS + " intentos.");
    }
}
