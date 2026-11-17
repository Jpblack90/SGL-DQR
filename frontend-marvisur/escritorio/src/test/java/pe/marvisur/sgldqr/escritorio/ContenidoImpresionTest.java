package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.SucursalDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ContenidoImpresion;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContenidoImpresionTest {

    private static final List<SucursalDto> SUCURSALES = List.of(
            new SucursalDto("SUC-LIM-01", "Agencia Lima Central", "Lima", "Av. Lima 123"),
            new SucursalDto("SUC-AQP-01", "Agencia Arequipa", "Arequipa", "Calle Jacinto Ibañez 315"));

    @Test
    @DisplayName("nombreAgencia: traduce el código a nombre y conserva el código si no está en el catálogo")
    void nombreAgencia() {
        assertThat(ContenidoImpresion.nombreAgencia("SUC-LIM-01", SUCURSALES)).isEqualTo("Agencia Lima Central");
        assertThat(ContenidoImpresion.nombreAgencia("suc-aqp-01", SUCURSALES)).isEqualTo("Agencia Arequipa");
        assertThat(ContenidoImpresion.nombreAgencia("SUC-XYZ-99", SUCURSALES)).isEqualTo("SUC-XYZ-99");
        assertThat(ContenidoImpresion.nombreAgencia("SUC-LIM-01", null)).isEqualTo("SUC-LIM-01");
        assertThat(ContenidoImpresion.nombreAgencia(null, SUCURSALES)).isEqualTo("-");
    }

    @Test
    @DisplayName("basePortal: toma la base del enlace de Billetera sin arrastrar el token")
    void basePortalSinToken() {
        String base = ContenidoImpresion.basePortal("http://192.168.1.10:5173/billetera#TOKEN-SECRETO", null);
        assertThat(base).isEqualTo("http://192.168.1.10:5173");
        assertThat(base).doesNotContain("TOKEN");
        assertThat(ContenidoImpresion.basePortal(null, "https://portal.marvisur.pe/")).isEqualTo("https://portal.marvisur.pe");
        assertThat(ContenidoImpresion.basePortal("sin-billetera", null)).isNull();
    }

    @Test
    @DisplayName("urlRastreo: arma la dirección pública con el comprobante codificado")
    void urlRastreo() {
        assertThat(ContenidoImpresion.urlRastreo("http://localhost:5173", "MV-20261029-87U4YZ9G"))
                .isEqualTo("http://localhost:5173/rastreo?codigo=MV-20261029-87U4YZ9G");
        assertThat(ContenidoImpresion.urlRastreo("http://localhost:5173/", "MV DEMO"))
                .isEqualTo("http://localhost:5173/rastreo?codigo=MV%20DEMO");
        assertThat(ContenidoImpresion.urlRastreo(null, "MV-1")).isNull();
        assertThat(ContenidoImpresion.urlRastreo("http://x", " ")).isNull();
    }

    @Test
    @DisplayName("textoTicketCliente: muestra nombres de agencias y la indicación del QR de rastreo")
    void ticketCliente() {
        String ticket = ContenidoImpresion.textoTicketCliente("MV-1", "16/11/2026 10:00",
                "Agencia Lima Central", "Agencia Huancayo", "Ana María Flores", "Elena Ramos Castro",
                "2.00", "PAGADO", "10.00", true);
        assertThat(ticket)
                .contains("COMPROBANTE: MV-1")
                .contains("ORIGEN:      Agencia Lima Central")
                .contains("DESTINO:     Agencia Huancayo")
                .contains("TOTAL:       S/. 10.00")
                .contains("Escanee el QR para rastrear")
                .doesNotContain("SUC-");

        String sinQr = ContenidoImpresion.textoTicketCliente("MV-1", null, null, null, null, null,
                null, null, null, false);
        assertThat(sinQr).contains("Rastree su envio con el numero").contains("FECHA:       -");
    }

    @Test
    @DisplayName("textoEtiquetaBulto: destino en mayúsculas y marca de reimpresión")
    void etiquetaBulto() {
        String etiqueta = ContenidoImpresion.textoEtiquetaBulto("MV-1", "Agencia Lima Central",
                "Agencia Huancayo", "Elena Ramos Castro", "2.00", "16/11/2026 10:00", false);
        assertThat(etiqueta)
                .contains("ETIQUETA DE BULTO")
                .doesNotContain("REIMPRESA")
                .contains("DESTINO:     AGENCIA HUANCAYO")
                .contains("PESO:        2.00 Kg")
                .contains("QR1 OPERATIVO");

        String reimpresa = ContenidoImpresion.textoEtiquetaBulto("MV-1", "SUC-LIM-01", "SUC-HYO-01",
                "Elena", null, "16/11/2026", true);
        assertThat(reimpresa).contains("(REIMPRESA)").doesNotContain("PESO:");
    }

    @Test
    @DisplayName("enlaceWhatsApp: agrega el código 51 a celulares peruanos y codifica el mensaje")
    void enlaceWhatsApp() {
        String mensaje = ContenidoImpresion.mensajeBilletera("Elena", "MV-1", "http://x/billetera#T");
        String enlace = ContenidoImpresion.enlaceWhatsApp("987 654 324", mensaje);
        assertThat(enlace).startsWith("https://wa.me/51987654324?text=");
        assertThat(enlace).contains("Hola%20Elena").doesNotContain(" ").doesNotContain("+");

        assertThat(ContenidoImpresion.enlaceWhatsApp(null, "hola")).isEqualTo("https://wa.me/?text=hola");
        assertThat(ContenidoImpresion.enlaceWhatsApp("+51 987654324", "x")).startsWith("https://wa.me/51987654324?");
    }

    @Test
    @DisplayName("enlaceCorreo: arma un mailto con asunto y cuerpo codificados")
    void enlaceCorreo() {
        String enlace = ContenidoImpresion.enlaceCorreo("elena.ramos@demo.com", "Su encomienda MV-1", "Hola Elena");
        assertThat(enlace).isEqualTo("mailto:elena.ramos@demo.com?subject=Su%20encomienda%20MV-1&body=Hola%20Elena");
        assertThat(ContenidoImpresion.enlaceCorreo(null, "a", "b")).isEqualTo("mailto:?subject=a&body=b");
    }

    @Test
    @DisplayName("mensajeBilletera: incluye el enlace y advierte no compartirlo")
    void mensajeBilletera() {
        String mensaje = ContenidoImpresion.mensajeBilletera(null, "MV-1", "http://x/billetera#T");
        assertThat(mensaje).startsWith("Hola, ").contains("MV-1").contains("http://x/billetera#T")
                .contains("No lo comparta");
    }
}
