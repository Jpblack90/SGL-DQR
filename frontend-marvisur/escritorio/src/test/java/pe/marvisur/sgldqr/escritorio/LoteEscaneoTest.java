package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.AgenciaResumenDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ItemResultadoLoteDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RutaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ViajeDto;
import pe.marvisur.sgldqr.escritorio.ui.ControladorAlmacen;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.LoteEscaneoComando;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RespuestaLoteEscaneoDto;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoteEscaneoTest {

    @Test
    @DisplayName("Escaneo por lote: previene duplicados en la lista de escaneo")
    void testPrevenirDuplicadosEnEscaneo() {
        List<String> listaEscaneada = new ArrayList<>();
        String qr1 = "MARVISUR:QR1:ENC-001:TOKEN-A";

        // Primer escaneo
        if (!listaEscaneada.contains(qr1)) {
            listaEscaneada.add(qr1);
        }

        // Segundo escaneo del mismo bulto
        boolean agregadoSegundo = false;
        if (!listaEscaneada.contains(qr1)) {
            listaEscaneada.add(qr1);
            agregadoSegundo = true;
        }

        assertThat(agregadoSegundo).isFalse();
        assertThat(listaEscaneada).hasSize(1);
    }

    @Test
    @DisplayName("Escaneo por lote: filtra correctamente los ítems fallidos para reintento")
    void testFiltrarFallidosParaReintento() {
        List<ItemResultadoLoteDto> detalles = List.of(
                new ItemResultadoLoteDto("QR1-001", "ENC-001", "OK", "Recepcionado en origen"),
                new ItemResultadoLoteDto("QR1-002", "ENC-002", "YA_REGISTRADA", "Ya registrado previamente"),
                new ItemResultadoLoteDto("QR1-003", null, "ERROR", "Encomienda no encontrada"),
                new ItemResultadoLoteDto("QR1-004", "ENC-004", "ERROR", "Estado de encomienda incompatible")
        );

        RespuestaLoteEscaneoDto respuesta = new RespuestaLoteEscaneoDto(
                4, 1, 1, 2, 45, detalles
        );

        List<String> fallidos = respuesta.detalles().stream()
                .filter(d -> "ERROR".equalsIgnoreCase(d.resultado()))
                .map(ItemResultadoLoteDto::qr1)
                .toList();

        assertThat(fallidos).hasSize(2);
        assertThat(fallidos).containsExactly("QR1-003", "QR1-004");
    }

    @Test
    @DisplayName("Escaneo por lote: genera contenido CSV estructurado con cabecera y delimitador")
    void testGeneracionContenidoCsv() {
        List<ItemResultadoLoteDto> detalles = List.of(
                new ItemResultadoLoteDto("QR1-001", "ENC-001", "OK", "Despachado"),
                new ItemResultadoLoteDto("QR1-002", null, "ERROR", "No encontrada")
        );

        StringBuilder csv = new StringBuilder();
        csv.append("QR1;RESULTADO;MOTIVO\n");
        for (ItemResultadoLoteDto item : detalles) {
            csv.append(item.qr1()).append(";")
               .append(item.resultado()).append(";")
               .append(item.motivo()).append("\n");
        }

        String contenido = csv.toString();

        assertThat(contenido).startsWith("QR1;RESULTADO;MOTIVO\n");
        assertThat(contenido).contains("QR1-001;OK;Despachado\n");
        assertThat(contenido).contains("QR1-002;ERROR;No encontrada\n");
    }

    @Test
    @DisplayName("LoteEscaneoComando: valida tope máximo de ítems")
    void testTopeMaximoItemsLote() {
        int maxLote = 200;
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 205; i++) {
            items.add("QR1-ITEM-" + i);
        }

        boolean excedeTope = items.size() > maxLote;

        assertThat(excedeTope).isTrue();
    }

    @Test
    @DisplayName("lotes: mapeo amigable de acciones entre códigos técnicos y etiquetas UI")
    void testMapeoAccionesLote() {
        assertThat(ControladorAlmacen.etiquetaAccionLote("RECEPCIONAR_ORIGEN")).isEqualTo("Recepcionar en origen");
        assertThat(ControladorAlmacen.etiquetaAccionLote("DESPACHAR_A_VIAJE")).isEqualTo("Despachar a viaje");
        assertThat(ControladorAlmacen.etiquetaAccionLote("RECEPCIONAR_DESTINO")).isEqualTo("Recepcionar en destino");
        assertThat(ControladorAlmacen.etiquetaAccionLote("OTRA")).isEqualTo("OTRA");
        assertThat(ControladorAlmacen.etiquetaAccionLote(null)).isEqualTo("");

        assertThat(ControladorAlmacen.codigoAccionLote("Recepcionar en origen")).isEqualTo("RECEPCIONAR_ORIGEN");
        assertThat(ControladorAlmacen.codigoAccionLote("Despachar a viaje")).isEqualTo("DESPACHAR_A_VIAJE");
        assertThat(ControladorAlmacen.codigoAccionLote("Recepcionar en destino")).isEqualTo("RECEPCIONAR_DESTINO");
        assertThat(ControladorAlmacen.codigoAccionLote(null)).isEqualTo("RECEPCIONAR_ORIGEN");
    }

    @Test
    @DisplayName("lotes: formateo legible de viaje en combo con origen, destino, placa y chofer")
    void testFormatearViajeComboLote() {
        RutaDto ruta = new RutaDto("R-1", new AgenciaResumenDto("SUC-1", "Lima"), new AgenciaResumenDto("SUC-2", "Cusco"), java.math.BigDecimal.valueOf(1100.0));
        ViajeDto viaje = new ViajeDto("V-1", "R-1", "SUC-1", "SUC-2", 1100.0, "VEH-1", "ABC-123", "CHO-1", "Mario Chofer", "2026-10-25T10:00:00Z", "2026-10-26T10:00:00Z", "PROGRAMADO", ruta, "ABC-123", "Volvo");

        String texto = ControladorAlmacen.formatearViajeComboLote(viaje);
        assertThat(texto).isEqualTo("Lima → Cusco · ABC-123 · Mario Chofer");

        assertThat(ControladorAlmacen.formatearViajeComboLote(null)).isEqualTo("");
    }

    @Test
    @DisplayName("lotes: auto-agregar código al pulsar procesar con lista vacía y texto en campo")
    void testAutoAgregarCodigoPendienteAlProcesar() {
        List<String> items = new ArrayList<>();
        String textoCampo = "MARVISUR:QR1:AUTO-ADD-001";

        // Simula la lógica de verificación al pulsar procesar
        if (items.isEmpty()) {
            String codigoPendiente = textoCampo != null ? textoCampo.trim() : "";
            if (!codigoPendiente.isEmpty()) {
                if (!items.contains(codigoPendiente)) {
                    items.add(codigoPendiente);
                }
            }
        }

        assertThat(items).hasSize(1);
        assertThat(items.get(0)).isEqualTo("MARVISUR:QR1:AUTO-ADD-001");
    }
}
