package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RutasFlotaViewModelTest {

    @Test
    @DisplayName("mapearFiltroEstadoApi: convierte etiquetas de UI a códigos de API")
    void testMapearFiltroEstadoApi() {
        assertThat(RutasFlotaViewModel.mapearFiltroEstadoApi("Programado")).isEqualTo("PROGRAMADO");
        assertThat(RutasFlotaViewModel.mapearFiltroEstadoApi("En ruta")).isEqualTo("EN_RUTA");
        assertThat(RutasFlotaViewModel.mapearFiltroEstadoApi("EN_RUTA")).isEqualTo("EN_RUTA");
        assertThat(RutasFlotaViewModel.mapearFiltroEstadoApi("Finalizado")).isEqualTo("FINALIZADO");
        assertThat(RutasFlotaViewModel.mapearFiltroEstadoApi("Todos los estados")).isEqualTo("TODOS");
        assertThat(RutasFlotaViewModel.mapearFiltroEstadoApi(null)).isEqualTo("TODOS");
    }

    @Test
    @DisplayName("etiquetaEstadoViaje: formatea códigos API a texto amigable")
    void testEtiquetaEstadoViaje() {
        assertThat(RutasFlotaViewModel.etiquetaEstadoViaje("PROGRAMADO")).isEqualTo("Programado");
        assertThat(RutasFlotaViewModel.etiquetaEstadoViaje("EN_RUTA")).isEqualTo("En ruta");
        assertThat(RutasFlotaViewModel.etiquetaEstadoViaje("FINALIZADO")).isEqualTo("Finalizado");
        assertThat(RutasFlotaViewModel.etiquetaEstadoViaje(null)).isEqualTo("--");
    }

    @Test
    @DisplayName("formatearCombos: genera texto legible sin llamar toString() crudo")
    void testFormatearCombos() {
        AgenciaResumenDto ori = new AgenciaResumenDto("suc-1", "Lima Central");
        AgenciaResumenDto des = new AgenciaResumenDto("suc-2", "Arequipa Terminal");
        RutaDto ruta = new RutaDto("r-1", ori, des, new BigDecimal("1010.50"));

        assertThat(RutasFlotaViewModel.formatearRutaCombo(ruta))
                .isEqualTo("Lima Central ➔ Arequipa Terminal (1010.50 km)");
        assertThat(RutasFlotaViewModel.formatearRutaCombo(null))
                .isEqualTo("Seleccione una ruta...");

        VehiculoDto v = new VehiculoDto("v-1", "V1A-852", "Volvo FH16", new BigDecimal("25000.00"));
        assertThat(RutasFlotaViewModel.formatearVehiculoCombo(v))
                .isEqualTo("V1A-852 - Volvo FH16 (25000.00 kg)");
        assertThat(RutasFlotaViewModel.formatearVehiculoCombo(null))
                .isEqualTo("Seleccione un vehículo...");

        EmpleadoDto emp = new EmpleadoDto("e-1", "76543210", "Juan", "Chofer Perez", "juan@m.pe", "TN4", "Chofer", "s-1", "Lima", true);
        assertThat(RutasFlotaViewModel.formatearChoferCombo(emp))
                .isEqualTo("Juan Chofer Perez (76543210)");
        assertThat(RutasFlotaViewModel.formatearChoferCombo(null))
                .isEqualTo("Seleccione un chofer...");
    }

    @Test
    @DisplayName("fechasLima: construcción, serialización y presentación con zonas explícitas e inyectables")
    void testFechasLima() {
        LocalDate fecha = LocalDate.of(2026, 10, 25);
        OffsetDateTime odt = RutasFlotaViewModel.construirOffsetDateTimeLima(fecha, 20, 30);

        assertThat(odt).isNotNull();
        assertThat(odt.getOffset()).isEqualTo(ZoneOffset.ofHours(-5));
        assertThat(odt.getYear()).isEqualTo(2026);
        assertThat(odt.getMonthValue()).isEqualTo(10);
        assertThat(odt.getDayOfMonth()).isEqualTo(25);
        assertThat(odt.getHour()).isEqualTo(20);
        assertThat(odt.getMinute()).isEqualTo(30);

        String iso = RutasFlotaViewModel.formatearFechaHoraIso(odt);
        assertThat(iso).contains("2026-10-25T20:30:00-05:00");

        // Presentación en zona explícita Lima (-05:00): 25/10/2026 20:30
        String textoLima = RutasFlotaViewModel.formatearFechaHoraTexto(iso, RutasFlotaViewModel.ZONA_LIMA);
        assertThat(textoLima).isEqualTo("25/10/2026 20:30");

        // Presentación en zona explícita UTC (+00:00): 26/10/2026 01:30
        String textoUtc = RutasFlotaViewModel.formatearFechaHoraTexto(iso, ZoneOffset.UTC);
        assertThat(textoUtc).isEqualTo("26/10/2026 01:30");

        // Instancia con zona explícita Tokio (+09:00): 26/10/2026 10:30
        RutasFlotaViewModel vmTokyo = RutasFlotaViewModel.conZona(ZoneId.of("Asia/Tokyo"));
        assertThat(vmTokyo.formatearFechaHoraTextoInstancia(iso)).isEqualTo("26/10/2026 10:30");

        // Presentación por defecto del sistema (producción): debe coincidir dinámicamente con la zona de la JVM
        String textoSistema = RutasFlotaViewModel.formatearFechaHoraTexto(iso);
        String esperadoSistema = odt.atZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        assertThat(textoSistema).isEqualTo(esperadoSistema);
    }

    @Test
    @DisplayName("validarVehiculo: valida placa (5-10 chars), modelo y capacidad > 0")
    void testValidarVehiculo() {
        // Válido
        List<String> erroresValido = RutasFlotaViewModel.validarVehiculo("V1A-852", "Volvo FH16", new BigDecimal("25000.00"));
        assertThat(erroresValido).isEmpty();

        // Placa inválida (corta o caracteres raros)
        List<String> erroresPlaca = RutasFlotaViewModel.validarVehiculo("AB", "Volvo", new BigDecimal("1000"));
        assertThat(erroresPlaca).isNotEmpty();
        assertThat(erroresPlaca.get(0)).contains("placa");

        // Modelo vacío
        List<String> erroresModelo = RutasFlotaViewModel.validarVehiculo("ABC-123", "", new BigDecimal("1000"));
        assertThat(erroresModelo).isNotEmpty();
        assertThat(erroresModelo.get(0)).contains("modelo");

        // Capacidad <= 0
        List<String> erroresCap = RutasFlotaViewModel.validarVehiculo("ABC-123", "Volvo", BigDecimal.ZERO);
        assertThat(erroresCap).isNotEmpty();
        assertThat(erroresCap.get(0)).contains("capacidad");
    }

    @Test
    @DisplayName("validarViaje: valida presencia de catálogo y que llegada sea posterior a salida")
    void testValidarViaje() {
        AgenciaResumenDto ori = new AgenciaResumenDto("s1", "Lima");
        AgenciaResumenDto des = new AgenciaResumenDto("s2", "Arequipa");
        RutaDto ruta = new RutaDto("r1", ori, des, new BigDecimal("1000"));
        VehiculoDto veh = new VehiculoDto("v1", "V1A-852", "Volvo", new BigDecimal("20000"));
        EmpleadoDto chofer = new EmpleadoDto("c1", "12345678", "Carlos", "Chofer", "c@m.pe", "TN4", "Chofer", "s1", "Lima", true);

        LocalDate salida = LocalDate.of(2026, 10, 25);
        LocalDate llegada = LocalDate.of(2026, 10, 26);

        // Válido: llegada al día siguiente
        List<String> erroresValido = RutasFlotaViewModel.validarViaje(ruta, veh, chofer, salida, 20, 0, llegada, 8, 0);
        assertThat(erroresValido).isEmpty();

        // Inconsistencia: llegada anterior a la salida
        List<String> erroresFechas = RutasFlotaViewModel.validarViaje(ruta, veh, chofer, salida, 20, 0, salida, 18, 0);
        assertThat(erroresFechas).isNotEmpty();
        assertThat(erroresFechas.get(0)).contains("posterior");

        // Falta chofer
        List<String> erroresSinChofer = RutasFlotaViewModel.validarViaje(ruta, veh, null, salida, 20, 0, llegada, 8, 0);
        assertThat(erroresSinChofer).isNotEmpty();
        assertThat(erroresSinChofer.get(0)).contains("chofer");
    }

    @Test
    @DisplayName("habilitacionAcciones: reglas de botones según estado de viaje y rol RBAC")
    void testHabilitacionAcciones() {
        ViajeDto programado = new ViajeDto("v1", "r1", "s1", "s2", 100.0, "veh1", "ABC", "emp1", "Juan", "2026-10-25T20:00:00-05:00", "2026-10-26T08:00:00-05:00", "PROGRAMADO", null, "ABC", "Volvo");
        ViajeDto enRuta = new ViajeDto("v2", "r1", "s1", "s2", 100.0, "veh1", "ABC", "emp1", "Juan", "2026-10-25T20:00:00-05:00", "2026-10-26T08:00:00-05:00", "EN_RUTA", null, "ABC", "Volvo");
        ViajeDto finalizado = new ViajeDto("v3", "r1", "s1", "s2", 100.0, "veh1", "ABC", "emp1", "Juan", "2026-10-25T20:00:00-05:00", "2026-10-26T08:00:00-05:00", "FINALIZADO", null, "ABC", "Volvo");

        // Iniciar: solo PROGRAMADO
        assertThat(RutasFlotaViewModel.puedeIniciarViaje(programado)).isTrue();
        assertThat(RutasFlotaViewModel.puedeIniciarViaje(enRuta)).isFalse();
        assertThat(RutasFlotaViewModel.puedeIniciarViaje(finalizado)).isFalse();

        // Finalizar: solo EN_RUTA
        assertThat(RutasFlotaViewModel.puedeFinalizarViaje(programado)).isFalse();
        assertThat(RutasFlotaViewModel.puedeFinalizarViaje(enRuta)).isTrue();
        assertThat(RutasFlotaViewModel.puedeFinalizarViaje(finalizado)).isFalse();

        // Manifiesto: disponible en cualquier viaje existente
        assertThat(RutasFlotaViewModel.puedeVerManifiesto(programado)).isTrue();
        assertThat(RutasFlotaViewModel.puedeVerManifiesto(null)).isFalse();

        // Creación y flota: exclusivo TN3 Administrador
        assertThat(RutasFlotaViewModel.puedeCrearViaje("TN3")).isTrue();
        assertThat(RutasFlotaViewModel.puedeCrearViaje("ROLE_ADMINISTRADOR")).isTrue();
        assertThat(RutasFlotaViewModel.puedeCrearViaje("TN4")).isFalse();
        assertThat(RutasFlotaViewModel.puedeGestionarFlota("TN3")).isTrue();
        assertThat(RutasFlotaViewModel.puedeGestionarFlota("TN4")).isFalse();
    }

    @Test
    @DisplayName("traduccionErrores: traduce códigos de error a mensajes claros y erradica 'null'")
    void testTraduccionErrores() {
        assertThat(RutasFlotaViewModel.traducirCodigoError("PLACA_DUPLICADA", null))
                .isEqualTo("Ya existe un vehículo con esa placa");
        assertThat(RutasFlotaViewModel.traducirCodigoError("CHOFER_INVALIDO", null))
                .isEqualTo("El colaborador elegido no es un chofer activo");
        assertThat(RutasFlotaViewModel.traducirCodigoError("CHOFER_OCUPADO", null))
                .isEqualTo("El chofer ya tiene un viaje en ruta");
        assertThat(RutasFlotaViewModel.traducirCodigoError("VEHICULO_OCUPADO", null))
                .isEqualTo("El vehículo ya está en un viaje en ruta");
        assertThat(RutasFlotaViewModel.traducirCodigoError("FECHA_INVALIDA", null))
                .isEqualTo("Revisa las fechas del viaje");
        assertThat(RutasFlotaViewModel.traducirCodigoError("TRANSICION_INVALIDA", null))
                .isEqualTo("El viaje ya cambió de estado: actualiza la lista");
        assertThat(RutasFlotaViewModel.traducirCodigoError("ACCESO_DENEGADO", null))
                .isEqualTo("No tienes permiso para esta acción");
        assertThat(RutasFlotaViewModel.traducirCodigoError("SIN_CONEXION", null))
                .isEqualTo("Requiere conexión");

        // Limpiar mensaje
        ApiExcepcion ex = new ApiExcepcion(409, "CHOFER_OCUPADO", null, null);
        assertThat(RutasFlotaViewModel.limpiarMensajeError(ex)).isEqualTo("El chofer ya tiene un viaje en ruta");
        assertThat(RutasFlotaViewModel.limpiarMensajeError(new RuntimeException("Connection refused")))
                .isEqualTo("Requiere conexión");
        assertThat(RutasFlotaViewModel.limpiarMensajeError(null)).doesNotContain("null");
    }

    @Test
    @DisplayName("manifiestoCalculos: suma pesos y genera resumen")
    void testManifiestoCalculos() {
        List<ManifiestoEncomiendaDto> items = List.of(
                new ManifiestoEncomiendaDto("MV-01", "EN_RUTA", "Arequipa", new BigDecimal("12.50")),
                new ManifiestoEncomiendaDto("MV-02", "EN_RUTA", "Arequipa", new BigDecimal("7.50"))
        );

        BigDecimal total = RutasFlotaViewModel.calcularPesoTotalManifiesto(items);
        assertThat(total).isEqualByComparingTo("20.00");

        String resumen = RutasFlotaViewModel.textoResumenManifiesto(items);
        assertThat(resumen).contains("Total encomiendas: 2");
        assertThat(resumen).contains("20.00 kg");
    }

    @Test
    @DisplayName("manifiesto: mapeo de etiquetas legibles de estado logístico")
    void testEtiquetaEstadoLogistico() {
        assertThat(RutasFlotaViewModel.etiquetaEstadoLogistico("REGISTRADO")).isEqualTo("Registrado");
        assertThat(RutasFlotaViewModel.etiquetaEstadoLogistico("RECEPCIONADO_ORIGEN")).isEqualTo("Recepción origen");
        assertThat(RutasFlotaViewModel.etiquetaEstadoLogistico("EN_RUTA")).isEqualTo("En ruta");
        assertThat(RutasFlotaViewModel.etiquetaEstadoLogistico("EN_ALMACEN_DESTINO")).isEqualTo("Almacén destino");
        assertThat(RutasFlotaViewModel.etiquetaEstadoLogistico("ENTREGADO")).isEqualTo("Entregado");
        assertThat(RutasFlotaViewModel.etiquetaEstadoLogistico(null)).isEqualTo("--");
        assertThat(RutasFlotaViewModel.etiquetaEstadoLogistico("OTRO")).isEqualTo("OTRO");
    }

    @Test
    @DisplayName("manifiesto: formateo de título amigable con origen, destino y placa")
    void testFormatearTituloManifiesto() {
        RutaDto ruta = new RutaDto("R-1", new AgenciaResumenDto("SUC-1", "Lima Central"), new AgenciaResumenDto("SUC-2", "Arequipa"), new BigDecimal("1000.0"));
        ViajeDto viaje = new ViajeDto("V-123", "R-1", "SUC-1", "SUC-2", 1000.0, "VEH-1", "V1A-999", "CHO-1", "Carlos", "2026-10-25T10:00:00Z", "2026-10-26T10:00:00Z", "PROGRAMADO", ruta, "V1A-999", "Volvo");

        String titulo = RutasFlotaViewModel.formatearTituloManifiesto(viaje);
        assertThat(titulo).isEqualTo("Manifiesto · Lima Central → Arequipa · V1A-999");

        ViajeDto viajeSinRuta = new ViajeDto("V-456", "R-2", "Trujillo", "Chiclayo", 200.0, "VEH-2", null, "CHO-2", "Pedro", "2026-10-25T10:00:00Z", "2026-10-26T10:00:00Z", "PROGRAMADO", null, null, null);
        String tituloFallback = RutasFlotaViewModel.formatearTituloManifiesto(viajeSinRuta);
        assertThat(tituloFallback).isEqualTo("Manifiesto · Trujillo → Chiclayo · V-456");

        assertThat(RutasFlotaViewModel.formatearTituloManifiesto(null)).isEqualTo("Manifiesto de Carga");
    }
}
