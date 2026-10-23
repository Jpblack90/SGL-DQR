package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EncomiendaRegistradaResultado;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.Qr1Data;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.UsuarioSesion;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SeguridadQr2TokenTest {

    @Test
    @DisplayName("Seguridad: qr2Token sensible se enmascara en EncomiendaRegistradaResultado.toString()")
    void testQr2TokenEnmascaradoEnToString() {
        String tokenSensible = "QR2-DIGITAL-TOKEN-ALTAMENTE-CONFIDENCIAL-987654";

        EncomiendaRegistradaResultado resultado = new EncomiendaRegistradaResultado(
                "UUID-ENC-001",
                "MV-20261022-XXXX",
                "CLI-01",
                "CLI-02",
                "SUC-LIM-01",
                "SUC-AQP-01",
                new BigDecimal("5.0"),
                new BigDecimal("0.01"),
                "Documentos",
                new BigDecimal("17.50"),
                "PAGADO",
                "REGISTRADO",
                "2026-10-22T10:00:00",
                new Qr1Data("UUID-ENC-001", "base64img"),
                tokenSensible,
                "http://localhost:5173/billetera#" + tokenSensible,
                null
        );

        // El método getter provee el valor en memoria para el diálogo
        assertThat(resultado.qr2Token()).isEqualTo(tokenSensible);

        // toString() NUNCA debe contener el token sensible
        String representacionString = resultado.toString();
        assertThat(representacionString).doesNotContain(tokenSensible);
        assertThat(representacionString).contains("qr2Token='***PROTECTED***'");
    }

    @Test
    @DisplayName("Seguridad: JWT de autenticación se enmascara en UsuarioSesion.toString()")
    void testJwtEnmascaradoEnToString() {
        String jwtSensible = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJyZWNlcGNpb25pc3RhQGRlbW8ucGUifQ.abcdef123456";

        UsuarioSesion sesion = new UsuarioSesion(
                "EMP-001",
                "Ana",
                "Torres",
                "ana.torres@marvisur.pe",
                "ROLE_RECEPCIONISTA",
                "SUC-LIM-01",
                jwtSensible
        );

        assertThat(sesion.token()).isEqualTo(jwtSensible);

        String representacion = sesion.toString();
        assertThat(representacion).doesNotContain(jwtSensible);
        assertThat(representacion).contains("token='***PROTECTED***'");
    }
}
