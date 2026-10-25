package pe.marvisur.sgldqr.api.servicio;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import pe.marvisur.sgldqr.api.dto.CambiarEstadoViajeRequest;
import pe.marvisur.sgldqr.api.dto.CrearVehiculoRequest;
import pe.marvisur.sgldqr.api.dto.CrearViajeRequest;
import pe.marvisur.sgldqr.api.dto.ManifiestoEncomiendaDto;
import pe.marvisur.sgldqr.api.dto.RutaDto;
import pe.marvisur.sgldqr.api.dto.VehiculoDto;
import pe.marvisur.sgldqr.api.dto.ViajeDto;
import pe.marvisur.sgldqr.api.error.ExcepcionOperacionInvalida;
import pe.marvisur.sgldqr.api.persistencia.entidad.DespachoLoteDetalleJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.RolJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.RutaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.VehiculoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.DespachoLoteDetalleSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ProgramacionViajeSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RolSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RutaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.VehiculoSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias — FlotaViajeServicio (RF-05)")
class FlotaViajeServicioTest {

    @Mock
    private VehiculoSpringDataRepository vehiculoRepo;
    @Mock
    private RutaSpringDataRepository rutaRepo;
    @Mock
    private ProgramacionViajeSpringDataRepository programacionViajeRepo;
    @Mock
    private EmpleadoSpringDataRepository empleadoRepo;
    @Mock
    private RolSpringDataRepository rolRepo;
    @Mock
    private SucursalSpringDataRepository sucursalRepo;
    @Mock
    private DespachoLoteDetalleSpringDataRepository despachoRepo;
    @Mock
    private EncomiendaSpringDataRepository encomiendaRepo;
    @Mock
    private AuditoriaPort auditoriaPort;

    private FlotaViajeServicio servicio;

    private UsuarioAutenticado usuarioAdmin;
    private UsuarioAutenticado usuarioChofer;

    @BeforeEach
    void setUp() {
        servicio = new FlotaViajeServicio(
                vehiculoRepo,
                rutaRepo,
                programacionViajeRepo,
                empleadoRepo,
                rolRepo,
                sucursalRepo,
                despachoRepo,
                encomiendaRepo,
                auditoriaPort
        );

        usuarioAdmin = new UsuarioAutenticado(
                "EMP-ADMIN-01", "Admin", "General", "admin@marvisur.pe",
                ConstantesSeguridad.AUTH_ADMINISTRADOR, "SUC-LIM-01"
        );

        usuarioChofer = new UsuarioAutenticado(
                "EMP-CHOFER-01", "Carlos", "Quispe", "chofer1@marvisur.pe",
                ConstantesSeguridad.AUTH_CHOFER, "SUC-LIM-01"
        );
    }

    // =========================================================================
    // 1. VEHÍCULOS
    // =========================================================================

    @Test
    @DisplayName("crearVehiculo: éxito persiste entidad, audita VEHICULO_REGISTRADO y retorna DTO")
    void crearVehiculoExitoso() {
        CrearVehiculoRequest req = new CrearVehiculoRequest("V1A-852", "Volvo FH16", new BigDecimal("25000.00"));
        when(vehiculoRepo.existsByPlacaIgnoreCase("V1A-852")).thenReturn(false);

        VehiculoDto res = servicio.crearVehiculo(req, "EMP-ADMIN-01", "127.0.0.1");

        assertThat(res).isNotNull();
        assertThat(res.getPlaca()).isEqualTo("V1A-852");
        assertThat(res.getModelo()).isEqualTo("Volvo FH16");
        assertThat(res.getCapacidadCargaKg()).isEqualByComparingTo("25000.00");

        verify(vehiculoRepo).save(any(VehiculoJpaEntity.class));
        verify(auditoriaPort).registrar(eq("EMP-ADMIN-01"), eq("VEHICULO_REGISTRADO"), eq("vehiculos"), eq(res.getIdVehiculo()), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("crearVehiculo: placa duplicada lanza 409 Conflict con código PLACA_DUPLICADA")
    void crearVehiculoPlacaDuplicada() {
        CrearVehiculoRequest req = new CrearVehiculoRequest("V1A-852", "Volvo FH16", new BigDecimal("25000.00"));
        when(vehiculoRepo.existsByPlacaIgnoreCase("V1A-852")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crearVehiculo(req, "EMP-ADMIN-01", "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> {
                    ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("PLACA_DUPLICADA");
                });

        verify(vehiculoRepo, never()).save(any());
        verify(auditoriaPort, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("crearVehiculo: placa con espacios o longitud fuera de rango lanza 400 Bad Request")
    void crearVehiculoPlacaInvalida() {
        CrearVehiculoRequest reqEspacios = new CrearVehiculoRequest("V1 A-852", "Volvo", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> servicio.crearVehiculo(reqEspacios, "EMP-ADMIN-01", "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        CrearVehiculoRequest reqCorta = new CrearVehiculoRequest("ABC", "Volvo", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> servicio.crearVehiculo(reqCorta, "EMP-ADMIN-01", "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("crearVehiculo: capacidad de carga menor o igual a cero lanza 400 Bad Request")
    void crearVehiculoCapacidadInvalida() {
        CrearVehiculoRequest reqCero = new CrearVehiculoRequest("V1A-852", "Volvo", BigDecimal.ZERO);
        assertThatThrownBy(() -> servicio.crearVehiculo(reqCero, "EMP-ADMIN-01", "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        CrearVehiculoRequest reqNegativo = new CrearVehiculoRequest("V1A-852", "Volvo", new BigDecimal("-100.00"));
        assertThatThrownBy(() -> servicio.crearVehiculo(reqNegativo, "EMP-ADMIN-01", "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("listarVehiculos: retorna lista ordenada de vehículos")
    void listarVehiculosExitoso() {
        VehiculoJpaEntity v1 = new VehiculoJpaEntity("veh-01", "ABC-123", "Scania", new BigDecimal("30000.00"));
        VehiculoJpaEntity v2 = new VehiculoJpaEntity("veh-02", "V1A-852", "Volvo", new BigDecimal("25000.00"));
        when(vehiculoRepo.findAll(any(Sort.class))).thenReturn(List.of(v1, v2));

        List<VehiculoDto> res = servicio.listarVehiculos();
        assertThat(res).hasSize(2);
        assertThat(res.get(0).getPlaca()).isEqualTo("ABC-123");
        assertThat(res.get(1).getPlaca()).isEqualTo("V1A-852");
    }

    // =========================================================================
    // 2. RUTAS
    // =========================================================================

    @Test
    @DisplayName("listarRutas: resuelve y formatea agencias de origen y destino")
    void listarRutasExitoso() {
        RutaJpaEntity r1 = new RutaJpaEntity("ruta-01", "SUC-LIM-01", "SUC-AQP-01", new BigDecimal("1010.50"));
        when(rutaRepo.findAll()).thenReturn(List.of(r1));

        SucursalJpaEntity sucLima = new SucursalJpaEntity("SUC-LIM-01", "Agencia Lima Central", "Av. Grau 450", "Lima");
        SucursalJpaEntity sucAqp = new SucursalJpaEntity("SUC-AQP-01", "Agencia Arequipa Terminal", "Av. Industrial 123", "Arequipa");
        when(sucursalRepo.findAllById(any())).thenReturn(List.of(sucLima, sucAqp));

        List<RutaDto> rutas = servicio.listarRutas();
        assertThat(rutas).hasSize(1);
        RutaDto ruta = rutas.get(0);
        assertThat(ruta.id()).isEqualTo("ruta-01");
        assertThat(ruta.origen().nombreAgencia()).isEqualTo("Agencia Lima Central");
        assertThat(ruta.destino().nombreAgencia()).isEqualTo("Agencia Arequipa Terminal");
        assertThat(ruta.distanciaKm()).isEqualByComparingTo("1010.50");
    }

    // =========================================================================
    // 3. PROGRAMACIÓN DE VIAJES
    // =========================================================================

    @Test
    @DisplayName("crearViaje: éxito programa viaje en estado PROGRAMADO y audita VIAJE_CREADO")
    void crearViajeExitoso() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.of("America/Lima")).plusMinutes(10);
        OffsetDateTime llegada = ahora.plusHours(14);
        CrearViajeRequest req = new CrearViajeRequest("ruta-01", "veh-01", "EMP-04", ahora, llegada);

        RutaJpaEntity ruta = new RutaJpaEntity("ruta-01", "SUC-LIM-01", "SUC-AQP-01", new BigDecimal("1010.50"));
        VehiculoJpaEntity veh = new VehiculoJpaEntity("veh-01", "V1A-852", "Volvo FH16", new BigDecimal("25000.00"));
        EmpleadoJpaEntity chofer = new EmpleadoJpaEntity("EMP-04", ConstantesSeguridad.ID_ROL_CHOFER, "SUC-LIM-01",
                "44556677", "Juan", "Chofer", "chofer@marvisur.pe", "hash", true);

        when(rutaRepo.findById("ruta-01")).thenReturn(Optional.of(ruta));
        when(vehiculoRepo.findById("veh-01")).thenReturn(Optional.of(veh));
        when(empleadoRepo.findById("EMP-04")).thenReturn(Optional.of(chofer));
        when(programacionViajeRepo.existsByIdChoferAndEstadoViaje("EMP-04", "EN_RUTA")).thenReturn(false);
        when(programacionViajeRepo.existsByIdVehiculoAndEstadoViaje("veh-01", "EN_RUTA")).thenReturn(false);

        ViajeDto res = servicio.crearViaje(req, usuarioAdmin, "127.0.0.1");

        assertThat(res).isNotNull();
        assertThat(res.getEstadoViaje()).isEqualTo(EstadoViaje.PROGRAMADO);
        assertThat(res.getPlaca()).isEqualTo("V1A-852");
        assertThat(res.getModelo()).isEqualTo("Volvo FH16");

        verify(programacionViajeRepo).save(any(ProgramacionViajeJpaEntity.class));
        verify(auditoriaPort).registrar(eq("EMP-ADMIN-01"), eq("VIAJE_CREADO"), eq("programacion_viajes"), any(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("crearViaje: chofer inactivo o no existente lanza 409 CHOFER_INVALIDO")
    void crearViajeChoferInactivoONoExiste() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.of("America/Lima")).plusMinutes(10);
        CrearViajeRequest req = new CrearViajeRequest("ruta-01", "veh-01", "EMP-INEXISTENTE", ahora, ahora.plusHours(10));

        when(rutaRepo.findById("ruta-01")).thenReturn(Optional.of(new RutaJpaEntity()));
        when(vehiculoRepo.findById("veh-01")).thenReturn(Optional.of(new VehiculoJpaEntity()));
        when(empleadoRepo.findById("EMP-INEXISTENTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crearViaje(req, usuarioAdmin, "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getCodigo()).isEqualTo("CHOFER_INVALIDO"));

        // Chofer inactivo
        EmpleadoJpaEntity choferInactivo = new EmpleadoJpaEntity("EMP-04", ConstantesSeguridad.ID_ROL_CHOFER, "SUC-LIM-01",
                "44556677", "Juan", "Chofer", "chofer@marvisur.pe", "hash", false);
        when(empleadoRepo.findById("EMP-04")).thenReturn(Optional.of(choferInactivo));
        CrearViajeRequest reqInactivo = new CrearViajeRequest("ruta-01", "veh-01", "EMP-04", ahora, ahora.plusHours(10));

        assertThatThrownBy(() -> servicio.crearViaje(reqInactivo, usuarioAdmin, "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getCodigo()).isEqualTo("CHOFER_INVALIDO"));
    }

    @Test
    @DisplayName("crearViaje: empleado sin rol TN4 Chofer lanza 409 CHOFER_INVALIDO")
    void crearViajeEmpleadoSinRolChofer() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.of("America/Lima")).plusMinutes(10);
        CrearViajeRequest req = new CrearViajeRequest("ruta-01", "veh-01", "EMP-REC", ahora, ahora.plusHours(10));

        when(rutaRepo.findById("ruta-01")).thenReturn(Optional.of(new RutaJpaEntity()));
        when(vehiculoRepo.findById("veh-01")).thenReturn(Optional.of(new VehiculoJpaEntity()));

        EmpleadoJpaEntity recepcionista = new EmpleadoJpaEntity("EMP-REC", "ROL-TN1-RECEPCION", "SUC-LIM-01",
                "44556677", "Pedro", "Recepcionista", "rec@marvisur.pe", "hash", true);
        when(empleadoRepo.findById("EMP-REC")).thenReturn(Optional.of(recepcionista));
        when(rolRepo.findById("ROL-TN1-RECEPCION")).thenReturn(Optional.of(new RolJpaEntity("ROL-TN1-RECEPCION", "Recepcionista", "")));

        assertThatThrownBy(() -> servicio.crearViaje(req, usuarioAdmin, "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getCodigo()).isEqualTo("CHOFER_INVALIDO"));
    }

    @Test
    @DisplayName("crearViaje: chofer ya en viaje EN_RUTA lanza 409 CHOFER_OCUPADO")
    void crearViajeChoferOcupado() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.of("America/Lima")).plusMinutes(10);
        CrearViajeRequest req = new CrearViajeRequest("ruta-01", "veh-01", "EMP-04", ahora, ahora.plusHours(10));

        when(rutaRepo.findById("ruta-01")).thenReturn(Optional.of(new RutaJpaEntity()));
        when(vehiculoRepo.findById("veh-01")).thenReturn(Optional.of(new VehiculoJpaEntity()));
        EmpleadoJpaEntity chofer = new EmpleadoJpaEntity("EMP-04", ConstantesSeguridad.ID_ROL_CHOFER, "SUC-LIM-01",
                "44556677", "Juan", "Chofer", "chofer@marvisur.pe", "hash", true);
        when(empleadoRepo.findById("EMP-04")).thenReturn(Optional.of(chofer));

        when(programacionViajeRepo.existsByIdChoferAndEstadoViaje("EMP-04", "EN_RUTA")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crearViaje(req, usuarioAdmin, "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getCodigo()).isEqualTo("CHOFER_OCUPADO"));
    }

    @Test
    @DisplayName("crearViaje: vehículo ya en viaje EN_RUTA lanza 409 VEHICULO_OCUPADO")
    void crearViajeVehiculoOcupado() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.of("America/Lima")).plusMinutes(10);
        CrearViajeRequest req = new CrearViajeRequest("ruta-01", "veh-01", "EMP-04", ahora, ahora.plusHours(10));

        when(rutaRepo.findById("ruta-01")).thenReturn(Optional.of(new RutaJpaEntity()));
        when(vehiculoRepo.findById("veh-01")).thenReturn(Optional.of(new VehiculoJpaEntity()));
        EmpleadoJpaEntity chofer = new EmpleadoJpaEntity("EMP-04", ConstantesSeguridad.ID_ROL_CHOFER, "SUC-LIM-01",
                "44556677", "Juan", "Chofer", "chofer@marvisur.pe", "hash", true);
        when(empleadoRepo.findById("EMP-04")).thenReturn(Optional.of(chofer));

        when(programacionViajeRepo.existsByIdChoferAndEstadoViaje("EMP-04", "EN_RUTA")).thenReturn(false);
        when(programacionViajeRepo.existsByIdVehiculoAndEstadoViaje("veh-01", "EN_RUTA")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crearViaje(req, usuarioAdmin, "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getCodigo()).isEqualTo("VEHICULO_OCUPADO"));
    }

    @Test
    @DisplayName("crearViaje: llegada anterior a salida o salida en el pasado lanza 400 FECHA_INVALIDA")
    void crearViajeFechasInvalidas() {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.of("America/Lima")).plusHours(1);
        OffsetDateTime salidaPasada = OffsetDateTime.now(ZoneId.of("America/Lima")).minusMinutes(10);

        when(rutaRepo.findById("ruta-01")).thenReturn(Optional.of(new RutaJpaEntity()));
        when(vehiculoRepo.findById("veh-01")).thenReturn(Optional.of(new VehiculoJpaEntity()));
        EmpleadoJpaEntity chofer = new EmpleadoJpaEntity("EMP-04", ConstantesSeguridad.ID_ROL_CHOFER, "SUC-LIM-01",
                "44556677", "Juan", "Chofer", "chofer@marvisur.pe", "hash", true);
        when(empleadoRepo.findById("EMP-04")).thenReturn(Optional.of(chofer));

        // Llegada anterior a salida
        CrearViajeRequest reqLlegadaAntes = new CrearViajeRequest("ruta-01", "veh-01", "EMP-04", ahora, ahora.minusHours(2));
        assertThatThrownBy(() -> servicio.crearViaje(reqLlegadaAntes, usuarioAdmin, "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getCodigo()).isEqualTo("FECHA_INVALIDA"));

        // Salida anterior a 5 minutos en el pasado
        CrearViajeRequest reqSalidaPasada = new CrearViajeRequest("ruta-01", "veh-01", "EMP-04", salidaPasada, ahora.plusHours(10));
        assertThatThrownBy(() -> servicio.crearViaje(reqSalidaPasada, usuarioAdmin, "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> assertThat(((ExcepcionOperacionInvalida) ex).getCodigo()).isEqualTo("FECHA_INVALIDA"));
    }

    // =========================================================================
    // 4. CAMBIO DE ESTADO DE VIAJES
    // =========================================================================

    @Test
    @DisplayName("cambiarEstadoViaje: PROGRAMADO -> EN_RUTA registra VIAJE_INICIADO")
    void cambiarEstadoProgramadoAEnRuta() {
        ProgramacionViajeJpaEntity viaje = new ProgramacionViajeJpaEntity(
                "viaje-01", "ruta-01", "veh-01", "EMP-CHOFER-01",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(12), "PROGRAMADO"
        );
        when(programacionViajeRepo.findById("viaje-01")).thenReturn(Optional.of(viaje));

        CambiarEstadoViajeRequest req = new CambiarEstadoViajeRequest(EstadoViaje.EN_RUTA);
        ViajeDto res = servicio.cambiarEstadoViaje("viaje-01", req, usuarioChofer, "127.0.0.1");

        assertThat(res.getEstadoViaje()).isEqualTo(EstadoViaje.EN_RUTA);
        verify(programacionViajeRepo).save(viaje);
        verify(auditoriaPort).registrar(eq("EMP-CHOFER-01"), eq("VIAJE_INICIADO"), eq("programacion_viajes"), eq("viaje-01"), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("cambiarEstadoViaje: EN_RUTA -> FINALIZADO registra VIAJE_FINALIZADO")
    void cambiarEstadoEnRutaAFinalizado() {
        ProgramacionViajeJpaEntity viaje = new ProgramacionViajeJpaEntity(
                "viaje-01", "ruta-01", "veh-01", "EMP-CHOFER-01",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(12), "EN_RUTA"
        );
        when(programacionViajeRepo.findById("viaje-01")).thenReturn(Optional.of(viaje));

        CambiarEstadoViajeRequest req = new CambiarEstadoViajeRequest(EstadoViaje.FINALIZADO);
        ViajeDto res = servicio.cambiarEstadoViaje("viaje-01", req, usuarioChofer, "127.0.0.1");

        assertThat(res.getEstadoViaje()).isEqualTo(EstadoViaje.FINALIZADO);
        verify(programacionViajeRepo).save(viaje);
        verify(auditoriaPort).registrar(eq("EMP-CHOFER-01"), eq("VIAJE_FINALIZADO"), eq("programacion_viajes"), eq("viaje-01"), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("cambiarEstadoViaje: transición inválida (PROGRAMADO -> FINALIZADO) lanza 409 TRANSICION_INVALIDA")
    void cambiarEstadoTransicionInvalida() {
        ProgramacionViajeJpaEntity viaje = new ProgramacionViajeJpaEntity(
                "viaje-01", "ruta-01", "veh-01", "EMP-CHOFER-01",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(12), "PROGRAMADO"
        );
        when(programacionViajeRepo.findById("viaje-01")).thenReturn(Optional.of(viaje));

        CambiarEstadoViajeRequest req = new CambiarEstadoViajeRequest(EstadoViaje.FINALIZADO);

        assertThatThrownBy(() -> servicio.cambiarEstadoViaje("viaje-01", req, usuarioChofer, "127.0.0.1"))
                .isInstanceOf(ExcepcionOperacionInvalida.class)
                .satisfies(ex -> {
                    ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("TRANSICION_INVALIDA");
                    assertThat(e.getMessage()).contains("PROGRAMADO").contains("FINALIZADO");
                });

        verify(programacionViajeRepo, never()).save(any());
        verify(auditoriaPort, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("cambiarEstadoViaje: chofer intenta modificar viaje de otro chofer lanza 403 AccessDeniedException")
    void cambiarEstadoChoferOtroViaje() {
        ProgramacionViajeJpaEntity viaje = new ProgramacionViajeJpaEntity(
                "viaje-01", "ruta-01", "veh-01", "EMP-OTRO-CHOFER",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(12), "PROGRAMADO"
        );
        when(programacionViajeRepo.findById("viaje-01")).thenReturn(Optional.of(viaje));

        CambiarEstadoViajeRequest req = new CambiarEstadoViajeRequest(EstadoViaje.EN_RUTA);

        assertThatThrownBy(() -> servicio.cambiarEstadoViaje("viaje-01", req, usuarioChofer, "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class);

        verify(programacionViajeRepo, never()).save(any());
    }

    // =========================================================================
    // 5. RBAC EN LISTADO Y MANIFIESTO
    // =========================================================================

    @Test
    @DisplayName("listarViajes: chofer TN4 siempre filtra por su propio idEmpleado")
    @SuppressWarnings("unchecked")
    void listarViajesChoferFiltraPorSuId() {
        when(programacionViajeRepo.findAll(any(Specification.class), any(Sort.class))).thenReturn(Collections.emptyList());

        servicio.listarViajes(null, null, "EMP-OTRO-CHOFER", usuarioChofer);

        // Verifica que no lance error y use la especificación con el ID del chofer autenticado
        verify(programacionViajeRepo).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    @DisplayName("obtenerManifiesto: éxito devuelve manifiesto con cero datos personales")
    void obtenerManifiestoExitoso() {
        ProgramacionViajeJpaEntity viaje = new ProgramacionViajeJpaEntity(
                "viaje-01", "ruta-01", "veh-01", "EMP-CHOFER-01",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(12), "EN_RUTA"
        );
        when(programacionViajeRepo.findById("viaje-01")).thenReturn(Optional.of(viaje));

        DespachoLoteDetalleJpaEntity det = new DespachoLoteDetalleJpaEntity("det-01", "viaje-01", "enc-01", OffsetDateTime.now());
        when(despachoRepo.findByIdViaje("viaje-01")).thenReturn(List.of(det));

        EncomiendaJpaEntity enc = new EncomiendaJpaEntity();
        enc.setIdEncomienda("enc-01");
        enc.setCodigoComprobante("MV-20261021-A1B2C3D4");
        enc.setEstadoLogistico("EN_RUTA");
        enc.setIdSucursalDestino("SUC-AQP-01");
        enc.setPesoKg(new BigDecimal("12.50"));
        when(encomiendaRepo.findAllById(Set.of("enc-01"))).thenReturn(List.of(enc));

        SucursalJpaEntity sucDestino = new SucursalJpaEntity("SUC-AQP-01", "Agencia Arequipa Terminal", "Av. Industrial 123", "Arequipa");
        when(sucursalRepo.findAllById(Set.of("SUC-AQP-01"))).thenReturn(List.of(sucDestino));

        List<ManifiestoEncomiendaDto> res = servicio.obtenerManifiesto("viaje-01", usuarioChofer);

        assertThat(res).hasSize(1);
        ManifiestoEncomiendaDto item = res.get(0);
        assertThat(item.codigoComprobante()).isEqualTo("MV-20261021-A1B2C3D4");
        assertThat(item.estadoLogistico()).isEqualTo("EN_RUTA");
        assertThat(item.nombreAgenciaDestino()).isEqualTo("Agencia Arequipa Terminal");
        assertThat(item.pesoKg()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("obtenerManifiesto: chofer intentando ver manifiesto de otro chofer lanza 403 AccessDeniedException")
    void obtenerManifiestoChoferOtroViaje() {
        ProgramacionViajeJpaEntity viaje = new ProgramacionViajeJpaEntity(
                "viaje-01", "ruta-01", "veh-01", "EMP-OTRO-CHOFER",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(12), "EN_RUTA"
        );
        when(programacionViajeRepo.findById("viaje-01")).thenReturn(Optional.of(viaje));

        assertThatThrownBy(() -> servicio.obtenerManifiesto("viaje-01", usuarioChofer))
                .isInstanceOf(AccessDeniedException.class);
    }
}
