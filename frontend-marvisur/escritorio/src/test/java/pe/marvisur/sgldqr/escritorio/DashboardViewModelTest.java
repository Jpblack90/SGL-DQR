package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ApiExcepcion;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.DashboardViewModel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardViewModelTest {

    @Test
    @DisplayName("formatearMonto: produce S/ con dos decimales y punto decimal sin depender del locale del sistema")
    void testFormatearMonto() {
        assertThat(DashboardViewModel.formatearMonto(new BigDecimal("17.50"))).isEqualTo("S/ 17.50");
        assertThat(DashboardViewModel.formatearMonto(new BigDecimal("1850.5"))).isEqualTo("S/ 1850.50");
        assertThat(DashboardViewModel.formatearMonto(BigDecimal.ZERO)).isEqualTo("S/ 0.00");
        assertThat(DashboardViewModel.formatearMonto(null)).isEqualTo("S/ 0.00");
    }

    @Test
    @DisplayName("formatearHoras: produce formato sufijo 'h'")
    void testFormatearHoras() {
        assertThat(DashboardViewModel.formatearHoras(0)).isEqualTo("0 h");
        assertThat(DashboardViewModel.formatearHoras(48)).isEqualTo("48 h");
        assertThat(DashboardViewModel.formatearHoras(120)).isEqualTo("120 h");
    }

    @Test
    @DisplayName("formatearLatencia: muestra '< 1 ms' si es 0 o menor a 1, o número con 1 decimal si es >= 1")
    void testFormatearLatencia() {
        assertThat(DashboardViewModel.formatearLatenciaPromedio(142.5)).isEqualTo("142.5 ms");
        assertThat(DashboardViewModel.formatearLatenciaPromedio(0.4)).isEqualTo("< 1 ms");
        assertThat(DashboardViewModel.formatearLatenciaPromedio(0.0)).isEqualTo("< 1 ms");
        assertThat(DashboardViewModel.formatearLatenciaPromedio(null)).isEqualTo("< 1 ms");

        assertThat(DashboardViewModel.formatearLatenciaMaxima(310)).isEqualTo("310.0 ms");
        assertThat(DashboardViewModel.formatearLatenciaMaxima(1)).isEqualTo("1.0 ms");
        assertThat(DashboardViewModel.formatearLatenciaMaxima(0)).isEqualTo("< 1 ms");
        assertThat(DashboardViewModel.formatearLatenciaMaxima(null)).isEqualTo("< 1 ms");
    }

    @Test
    @DisplayName("esLatenciaCritica: resalta en true solo si latenciaMaxima supera los 1000 ms (RNF-03)")
    void testEsLatenciaCritica() {
        assertThat(DashboardViewModel.esLatenciaCritica(1001)).isTrue();
        assertThat(DashboardViewModel.esLatenciaCritica(2500)).isTrue();
        assertThat(DashboardViewModel.esLatenciaCritica(1000)).isFalse();
        assertThat(DashboardViewModel.esLatenciaCritica(310)).isFalse();
        assertThat(DashboardViewModel.esLatenciaCritica(0)).isFalse();
        assertThat(DashboardViewModel.esLatenciaCritica(null)).isFalse();
    }

    @Test
    @DisplayName("extraerDatosGraficoEstados: caso (a) fixture JSON de API construye 5 categorías en orden [1, 0, 0, 1, 1]")
    void testExtraerDatosGraficoEstados_CasoA() {
        // Fixture exacto del caso (a): {REGISTRADO:1, EN_ALMACEN_ORIGEN:0, EN_RUTA:0, EN_ALMACEN_DESTINO:1, ENTREGADO:1}
        Map<String, Long> porEstadoLogistico = Map.of(
                "REGISTRADO", 1L,
                "EN_ALMACEN_ORIGEN", 0L,
                "EN_RUTA", 0L,
                "EN_ALMACEN_DESTINO", 1L,
                "ENTREGADO", 1L
        );

        List<DashboardViewModel.ItemGraficoEstado> items = DashboardViewModel.extraerDatosGraficoEstados(porEstadoLogistico);

        assertThat(items).hasSize(5);

        // Claves exactas en orden estándar
        assertThat(items.stream().map(DashboardViewModel.ItemGraficoEstado::clave).toList())
                .containsExactly("REGISTRADO", "EN_ALMACEN_ORIGEN", "EN_RUTA", "EN_ALMACEN_DESTINO", "ENTREGADO");

        // Etiquetas amigables en español
        assertThat(items.stream().map(DashboardViewModel.ItemGraficoEstado::etiqueta).toList())
                .containsExactly("Registrado", "Almacén origen", "En ruta", "Almacén destino", "Entregado");

        // Valores exactamente en orden [1, 0, 0, 1, 1]
        assertThat(items.stream().map(DashboardViewModel.ItemGraficoEstado::cantidad).toList())
                .containsExactly(1L, 0L, 0L, 1L, 1L);
    }

    @Test
    @DisplayName("extraerDatosGraficoEstados: rellena con 0 si el mapa es nulo o faltan claves")
    void testExtraerDatosGraficoEstados_ValoresPorDefecto() {
        List<DashboardViewModel.ItemGraficoEstado> vacio = DashboardViewModel.extraerDatosGraficoEstados(null);
        assertThat(vacio).hasSize(5);
        assertThat(vacio.stream().map(DashboardViewModel.ItemGraficoEstado::cantidad).toList())
                .containsExactly(0L, 0L, 0L, 0L, 0L);

        Map<String, Long> parcial = Map.of("EN_ALMACEN_DESTINO", 3L);
        List<DashboardViewModel.ItemGraficoEstado> itemsParcial = DashboardViewModel.extraerDatosGraficoEstados(parcial);
        assertThat(itemsParcial).hasSize(5);
        assertThat(itemsParcial.stream().map(DashboardViewModel.ItemGraficoEstado::cantidad).toList())
                .containsExactly(0L, 0L, 0L, 3L, 0L);
    }

    @Test
    @DisplayName("etiquetaEstadoLogistico: traduce códigos técnicos a nombres amigables en español")
    void testEtiquetaEstadoLogistico() {
        assertThat(DashboardViewModel.etiquetaEstadoLogistico("REGISTRADA")).isEqualTo("Registrado");
        assertThat(DashboardViewModel.etiquetaEstadoLogistico("REGISTRADO")).isEqualTo("Registrado");
        assertThat(DashboardViewModel.etiquetaEstadoLogistico("EN_ALMACEN_ORIGEN")).isEqualTo("Almacén origen");
        assertThat(DashboardViewModel.etiquetaEstadoLogistico("EN_RUTA")).isEqualTo("En ruta");
        assertThat(DashboardViewModel.etiquetaEstadoLogistico("EN_TRANSITO")).isEqualTo("En ruta");
        assertThat(DashboardViewModel.etiquetaEstadoLogistico("EN_ALMACEN_DESTINO")).isEqualTo("Almacén destino");
        assertThat(DashboardViewModel.etiquetaEstadoLogistico("ENTREGADA")).isEqualTo("Entregado");
        assertThat(DashboardViewModel.etiquetaEstadoLogistico("ENTREGADO")).isEqualTo("Entregado");
        assertThat(DashboardViewModel.etiquetaEstadoLogistico("BLOQUEADA")).isEqualTo("Bloqueada");
        assertThat(DashboardViewModel.etiquetaEstadoLogistico(null)).isEqualTo("Desconocido");
        assertThat(DashboardViewModel.etiquetaEstadoLogistico("OTRO_ESTADO")).isEqualTo("OTRO_ESTADO");
    }

    @Test
    @DisplayName("constantes: placeholders de lista vacía, pie de página y texto umbral")
    void testConstantesYTextos() {
        assertThat(DashboardViewModel.TEXTO_TABLA_ESTANCADOS_VACIA).isEqualTo("Sin envíos estancados");
        assertThat(DashboardViewModel.NOTA_PIE_PAGINA).isEqualTo("Recaudado cuenta por fecha de registro del envío.");
        assertThat(DashboardViewModel.textoUmbralEstancado(48)).isEqualTo("Umbral: 48 horas");
        assertThat(DashboardViewModel.MENSAJE_SIN_CONEXION).isEqualTo("Requiere conexión");
        assertThat(DashboardViewModel.COLOR_AMBAR_LATENCIA_CRITICA).isEqualTo("#F59E0B");
    }

    @Test
    @DisplayName("formatearTextoActualizacion: estado actualizado vs desactualizado")
    void testFormatearTextoActualizacion() {
        OffsetDateTime fecha = OffsetDateTime.of(2026, 10, 24, 18, 30, 0, 0, ZoneOffset.ofHours(-5));

        String textoActualizado = DashboardViewModel.formatearTextoActualizacion(fecha, false, null);
        assertThat(textoActualizado).startsWith("Actualizado: ");

        String textoDesactualizado = DashboardViewModel.formatearTextoActualizacion(fecha, true, "Error de red");
        assertThat(textoDesactualizado).startsWith("Desactualizado (último: ");
        assertThat(textoDesactualizado).contains("Error de red");
    }

    @Test
    @DisplayName("limpiarMensajeError: traduce 403 a mensaje amigable y elimina 'null'")
    void testLimpiarMensajeError() {
        ApiExcepcion ex403 = new ApiExcepcion(403, "ACCESO_DENEGADO", "Acceso denegado", null);
        assertThat(DashboardViewModel.limpiarMensajeError(ex403))
                .isEqualTo(DashboardViewModel.MENSAJE_ACCESO_DENEGADO);

        ApiExcepcion ex401 = new ApiExcepcion(401, "NO_AUTORIZADO", "No autorizado", null);
        assertThat(DashboardViewModel.limpiarMensajeError(ex401))
                .isEqualTo("Sesión expirada o no autorizada");

        RuntimeException exNull = new RuntimeException((String) null);
        assertThat(DashboardViewModel.limpiarMensajeError(exNull))
                .isEqualTo("Error de comunicación con el servidor central");

        RuntimeException exPalabraNull = new RuntimeException("Error: null en conexión");
        assertThat(DashboardViewModel.limpiarMensajeError(exPalabraNull))
                .doesNotContain("null");

        assertThat(DashboardViewModel.limpiarMensajeError(null))
                .isEqualTo("Error inesperado en el dashboard");
    }
}
