package pe.marvisur.sgldqr.api.persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import pe.marvisur.sgldqr.api.persistencia.postgres.AuditoriaPostgresAdapter;
import pe.marvisur.sgldqr.api.persistencia.postgres.ClientePostgresAdapter;
import pe.marvisur.sgldqr.api.persistencia.postgres.EmpleadoPostgresAdapter;
import pe.marvisur.sgldqr.api.persistencia.postgres.EncomiendaPostgresAdapter;
import pe.marvisur.sgldqr.api.persistencia.postgres.HistorialTrackingPostgresAdapter;
import pe.marvisur.sgldqr.api.persistencia.postgres.ParDobleQRPostgresAdapter;
import pe.marvisur.sgldqr.api.persistencia.postgres.SucursalPostgresAdapter;
import pe.marvisur.sgldqr.dominio.modelo.AuditoriaLog;
import pe.marvisur.sgldqr.dominio.modelo.Cliente;
import pe.marvisur.sgldqr.dominio.modelo.Empleado;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoTokenQR2;
import pe.marvisur.sgldqr.dominio.modelo.HistorialTracking;
import pe.marvisur.sgldqr.dominio.modelo.MotivoInactivacion;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.ResultadoValidacion;
import pe.marvisur.sgldqr.dominio.modelo.Sucursal;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;
import pe.marvisur.sgldqr.dominio.modelo.TokenQR2Estatus;
import pe.marvisur.sgldqr.dominio.modelo.ValidacionCruzadaLog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de integración de persistencia para marvisur-api:
 * 1. Guarda y lee cada agregado a través de su adaptador y confirma que los datos coinciden con el dominio.
 * 2. Prueba de restricciones UNIQUE: intenta duplicar dni, numero_documento y codigo_comprobante.
 * 3. Prueba de CHECK: intenta insertar un estado fuera de lista; confirma que la base lo rechaza.
 * 4. Confirma que spring.jpa.hibernate.ddl-auto=validate arranca sin quejas contra el esquema de Flyway.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Persistencia PostgreSQL — Adaptadores, Restricciones UNIQUE y CHECK")
class PersistenciaPostgresTest {

    @Autowired
    private SucursalPostgresAdapter sucursalAdapter;

    @Autowired
    private EmpleadoPostgresAdapter empleadoAdapter;

    @Autowired
    private ClientePostgresAdapter clienteAdapter;

    @Autowired
    private EncomiendaPostgresAdapter encomiendaAdapter;

    @Autowired
    private ParDobleQRPostgresAdapter parDobleQRAdapter;

    @Autowired
    private AuditoriaPostgresAdapter auditoriaAdapter;

    @Autowired
    private HistorialTrackingPostgresAdapter trackingAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Agregado Sucursal: guardar y leer a través de SucursalPostgresAdapter")
    void testGuardarYLeerSucursal() {
        String idSucursal = UUID.randomUUID().toString();
        Sucursal sucursal = new Sucursal(idSucursal, "Agencia Arequipa Norte", "Av. Ejército 123", "Arequipa");

        sucursalAdapter.guardar(sucursal);

        Optional<Sucursal> recuperada = sucursalAdapter.buscarPorId(idSucursal);
        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().getIdSucursal()).isEqualTo(idSucursal);
        assertThat(recuperada.get().getNombreAgencia()).isEqualTo("Agencia Arequipa Norte");
        assertThat(recuperada.get().getDireccion()).isEqualTo("Av. Ejército 123");
        assertThat(recuperada.get().getCiudad()).isEqualTo("Arequipa");
    }

    @Test
    @DisplayName("Agregado Empleado: guardar y leer a través de EmpleadoPostgresAdapter")
    void testGuardarYLeerEmpleado() {
        String idEmpleado = UUID.randomUUID().toString();
        String dni = "72" + (int) (Math.random() * 900000 + 100000);
        String correo = "emp_" + idEmpleado.substring(0, 8) + "@marvisur.pe";

        Empleado empleado = new Empleado(
                idEmpleado,
                "ROL-TN1-RECEPCION",
                "SUC-LIM-01",
                dni,
                "Carlos",
                "Pérez Gómez",
                correo,
                "$2b$12$hashProvisionalDePrueba1234567890",
                true
        );

        empleadoAdapter.guardar(empleado);

        Optional<Empleado> porId = empleadoAdapter.buscarPorId(idEmpleado);
        assertThat(porId).isPresent();
        assertThat(porId.get().getDni()).isEqualTo(dni);
        assertThat(porId.get().getCorreo()).isEqualTo(correo);
        assertThat(porId.get().getIdRol()).isEqualTo("ROL-TN1-RECEPCION");

        Optional<Empleado> porDni = empleadoAdapter.buscarPorDni(dni);
        assertThat(porDni).isPresent();
        assertThat(porDni.get().getIdEmpleado()).isEqualTo(idEmpleado);

        Optional<Empleado> porCorreo = empleadoAdapter.buscarPorCorreo(correo);
        assertThat(porCorreo).isPresent();
        assertThat(porCorreo.get().getIdEmpleado()).isEqualTo(idEmpleado);
    }

    @Test
    @DisplayName("Agregado Cliente: guardar y leer a través de ClientePostgresAdapter")
    void testGuardarYLeerCliente() {
        String idCliente = UUID.randomUUID().toString();
        String numeroDoc = "45" + (int) (Math.random() * 900000 + 100000);

        Cliente cliente = new Cliente(
                idCliente,
                TipoDocumento.DNI,
                numeroDoc,
                "María Elena Rojas",
                "987654321",
                "maria.rojas@gmail.com"
        );

        clienteAdapter.guardar(cliente);

        Optional<Cliente> porId = clienteAdapter.buscarPorId(idCliente);
        assertThat(porId).isPresent();
        assertThat(porId.get().getNumeroDocumento()).isEqualTo(numeroDoc);
        assertThat(porId.get().getNombresRazonSocial()).isEqualTo("María Elena Rojas");
        assertThat(porId.get().getTelefono()).isEqualTo("987654321");

        Optional<Cliente> porDoc = clienteAdapter.buscarPorNumeroDocumento(numeroDoc);
        assertThat(porDoc).isPresent();
        assertThat(porDoc.get().getIdCliente()).isEqualTo(idCliente);
    }

    @Test
    @DisplayName("Agregado Encomienda: guardar y leer a través de EncomiendaPostgresAdapter")
    void testGuardarYLeerEncomienda() {
        // Clientes remitente y destinatario
        String idRemitente = UUID.randomUUID().toString();
        String idDestinatario = UUID.randomUUID().toString();
        clienteAdapter.guardar(new Cliente(idRemitente, TipoDocumento.DNI, "11" + (int)(Math.random() * 900000 + 100000), "Remitente Test", "911111111", "remit@test.com"));
        clienteAdapter.guardar(new Cliente(idDestinatario, TipoDocumento.DNI, "22" + (int)(Math.random() * 900000 + 100000), "Destinatario Test", "922222222", "dest@test.com"));

        String idEncomienda = UUID.randomUUID().toString();
        String codigoComp = "BOL-" + idEncomienda.substring(0, 8).toUpperCase();
        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        Encomienda encomienda = new Encomienda(
                idEncomienda,
                codigoComp,
                idRemitente,
                idDestinatario,
                "SUC-LIM-01",
                "SUC-LIM-01",
                new BigDecimal("12.50"),
                new BigDecimal("0.0450"),
                "Caja de repuestos electrónicos",
                new BigDecimal("45.00"),
                EstadoPago.PAGADO,
                EstadoLogistico.REGISTRADO,
                ahora
        );

        encomiendaAdapter.guardar(encomienda);

        Optional<Encomienda> porId = encomiendaAdapter.buscarPorId(idEncomienda);
        assertThat(porId).isPresent();
        assertThat(porId.get().getIdEncomienda()).isEqualTo(idEncomienda);
        assertThat(porId.get().getCodigoComprobante()).isEqualTo(codigoComp);
        assertThat(porId.get().getPesoKg()).isEqualByComparingTo("12.50");
        assertThat(porId.get().getMontoTotal()).isEqualByComparingTo("45.00");
        assertThat(porId.get().getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(porId.get().getEstadoLogistico()).isEqualTo(EstadoLogistico.REGISTRADO);

        Optional<Encomienda> porCodigo = encomiendaAdapter.buscarPorCodigoComprobante(codigoComp);
        assertThat(porCodigo).isPresent();
        assertThat(porCodigo.get().getIdEncomienda()).isEqualTo(idEncomienda);

        List<Encomienda> listaPorSucursal = encomiendaAdapter.buscarPorSucursalDestinoYEstado("SUC-LIM-01", EstadoLogistico.REGISTRADO);
        assertThat(listaPorSucursal).isNotEmpty();
    }

    @Test
    @DisplayName("Agregado ParDobleQR, Tokens y Logs: ciclo completo a través de ParDobleQRPostgresAdapter")
    void testGuardarYLeerParDobleQRYValidacionLog() {
        // Encomienda requerida para FK
        String idRemitente = UUID.randomUUID().toString();
        String idDestinatario = UUID.randomUUID().toString();
        clienteAdapter.guardar(new Cliente(idRemitente, TipoDocumento.DNI, "33" + (int)(Math.random() * 900000 + 100000), "Rem QR", "933333333", "remqr@test.com"));
        clienteAdapter.guardar(new Cliente(idDestinatario, TipoDocumento.DNI, "44" + (int)(Math.random() * 900000 + 100000), "Dest QR", "944444444", "destqr@test.com"));

        String idEncomienda = UUID.randomUUID().toString();
        encomiendaAdapter.guardar(new Encomienda(
                idEncomienda,
                "BOL-QR-" + idEncomienda.substring(0, 8).toUpperCase(),
                idRemitente,
                idDestinatario,
                "SUC-LIM-01",
                "SUC-LIM-01",
                new BigDecimal("5.00"),
                null,
                "Documentos notariales",
                new BigDecimal("15.00"),
                EstadoPago.PAGADO,
                EstadoLogistico.REGISTRADO,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        ));

        // 1. Guardar ParDobleQR con token activo
        String idPareja = UUID.randomUUID().toString();
        String uuidQr1 = UUID.randomUUID().toString();
        String hashQr2 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        LocalDateTime fechaGen = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        TokenQR2Estatus tokenEstatus = TokenQR2Estatus.crearActivo(idPareja);
        ParDobleQR par = new ParDobleQR(idPareja, idEncomienda, uuidQr1, hashQr2, fechaGen, tokenEstatus);

        parDobleQRAdapter.guardar(par);

        // 2. Buscar por ID y por encomienda
        Optional<ParDobleQR> parRecuperado = parDobleQRAdapter.buscarPorId(idPareja);
        assertThat(parRecuperado).isPresent();
        assertThat(parRecuperado.get().getIdParejaQr()).isEqualTo(idPareja);
        assertThat(parRecuperado.get().getUuidQr1Operativo()).isEqualTo(uuidQr1);
        assertThat(parRecuperado.get().getHashQr2LlaveDigital()).isEqualTo(hashQr2);
        assertThat(parRecuperado.get().getTokenEstatus()).isNotNull();
        assertThat(parRecuperado.get().getTokenEstatus().getEstadoToken()).isEqualTo(EstadoTokenQR2.ACTIVO);

        Optional<ParDobleQR> parPorEnc = parDobleQRAdapter.buscarPorEncomiendaId(idEncomienda);
        assertThat(parPorEnc).isPresent();

        // 3. Invalidar token QR2 de forma irreversible y actualizar
        tokenEstatus.invalidar(MotivoInactivacion.ENTREGA_EXITOSA, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        parDobleQRAdapter.actualizarTokenEstatus(tokenEstatus);

        Optional<TokenQR2Estatus> tokenActualizado = parDobleQRAdapter.buscarTokenEstatusPorParejaId(idPareja);
        assertThat(tokenActualizado).isPresent();
        assertThat(tokenActualizado.get().getEstadoToken()).isEqualTo(EstadoTokenQR2.INVALIDADO);
        assertThat(tokenActualizado.get().getMotivoInactivacion()).isEqualTo(MotivoInactivacion.ENTREGA_EXITOSA);
        assertThat(tokenActualizado.get().getFechaInvalidacion()).isNotNull();

        // 4. Registrar log de validación cruzada
        ValidacionCruzadaLog valLog = ValidacionCruzadaLog.crear(
                idPareja,
                "EMP-ADMIN-01",
                ResultadoValidacion.EXITOSO,
                12,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        );
        parDobleQRAdapter.guardarValidacionLog(valLog);

        List<ValidacionCruzadaLog> logs = parDobleQRAdapter.listarValidacionesPorParejaId(idPareja);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getResultado()).isEqualTo(ResultadoValidacion.EXITOSO);
        assertThat(logs.get(0).getLatenciaProcesamientoMs()).isEqualTo(12);
    }

    @Test
    @DisplayName("Agregado AuditoriaLog: registrar y consultar por registro a través de AuditoriaPostgresAdapter")
    void testGuardarYLeerAuditoriaLog() {
        String idRegistro = UUID.randomUUID().toString();
        AuditoriaLog log = AuditoriaLog.crear(
                "EMP-ADMIN-01",
                "CREACION_ENCOMIENDA",
                "encomiendas",
                idRegistro,
                "192.168.1.100",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        );

        auditoriaAdapter.registrar(log);

        List<AuditoriaLog> logs = auditoriaAdapter.buscarPorRegistro("encomiendas", idRegistro);
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getAccion()).isEqualTo("CREACION_ENCOMIENDA");
        assertThat(logs.get(0).getIpOrigen()).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Agregado HistorialTracking: registrar evento y listar a través de HistorialTrackingPostgresAdapter")
    void testGuardarYLeerHistorialTracking() {
        // Encomienda para FK
        String idRemitente = UUID.randomUUID().toString();
        String idDestinatario = UUID.randomUUID().toString();
        clienteAdapter.guardar(new Cliente(idRemitente, TipoDocumento.DNI, "55" + (int)(Math.random() * 900000 + 100000), "Rem Track", "955555555", "remtr@test.com"));
        clienteAdapter.guardar(new Cliente(idDestinatario, TipoDocumento.DNI, "66" + (int)(Math.random() * 900000 + 100000), "Dest Track", "966666666", "desttr@test.com"));

        String idEncomienda = UUID.randomUUID().toString();
        encomiendaAdapter.guardar(new Encomienda(
                idEncomienda,
                "BOL-TRK-" + idEncomienda.substring(0, 8).toUpperCase(),
                idRemitente,
                idDestinatario,
                "SUC-LIM-01",
                "SUC-LIM-01",
                new BigDecimal("2.00"),
                null,
                "Paquete tracking",
                new BigDecimal("10.00"),
                EstadoPago.PAGADO,
                EstadoLogistico.REGISTRADO,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        ));

        HistorialTracking evento = HistorialTracking.crear(
                idEncomienda,
                "SUC-LIM-01",
                "REGISTRADO",
                "Paquete recibido en counter",
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        );

        trackingAdapter.registrarEvento(evento);

        List<HistorialTracking> timeline = trackingAdapter.listarPorEncomienda(idEncomienda);
        assertThat(timeline).hasSize(1);
        assertThat(timeline.get(0).getEstadoEvento()).isEqualTo("REGISTRADO");
        assertThat(timeline.get(0).getObservacion()).isEqualTo("Paquete recibido en counter");
    }

    @Test
    @DisplayName("Restricción UNIQUE: intentar duplicar dni en empleados lanza DataIntegrityViolationException")
    void testRestriccionUniqueDniEmpleadoLanzaExcepcion() {
        // DNI '00000001' ya fue insertado en la migración V7 para EMP-ADMIN-01
        Empleado duplicado = new Empleado(
                UUID.randomUUID().toString(),
                "ROL-TN1-RECEPCION",
                "SUC-LIM-01",
                "00000001", // DNI ya existente
                "Duplicado",
                "Pérez",
                "duplicado_dni@marvisur.pe",
                "$2b$12$clave12345678901234567890",
                true
        );

        assertThatThrownBy(() -> empleadoAdapter.guardar(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Restricción UNIQUE: intentar duplicar numero_documento en clientes lanza DataIntegrityViolationException")
    void testRestriccionUniqueNumeroDocumentoClienteLanzaExcepcion() {
        String docUnico = "88" + (int)(Math.random() * 900000 + 100000);

        Cliente c1 = new Cliente(
                UUID.randomUUID().toString(),
                TipoDocumento.DNI,
                docUnico,
                "Cliente Original",
                "900000001",
                "c1_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com"
        );
        clienteAdapter.guardar(c1);

        Cliente c2 = new Cliente(
                UUID.randomUUID().toString(),
                TipoDocumento.DNI,
                docUnico, // Mismo documento
                "Cliente Duplicado",
                "900000002",
                "c2_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com"
        );

        assertThatThrownBy(() -> clienteAdapter.guardar(c2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Restricción UNIQUE: intentar duplicar codigo_comprobante en encomiendas lanza DataIntegrityViolationException")
    void testRestriccionUniqueCodigoComprobanteEncomiendaLanzaExcepcion() {
        String idRemitente = UUID.randomUUID().toString();
        String idDestinatario = UUID.randomUUID().toString();
        clienteAdapter.guardar(new Cliente(idRemitente, TipoDocumento.DNI, "71" + (int)(Math.random() * 900000 + 100000), "Rem UQ", "971111111", "remuq@test.com"));
        clienteAdapter.guardar(new Cliente(idDestinatario, TipoDocumento.DNI, "72" + (int)(Math.random() * 900000 + 100000), "Dest UQ", "972222222", "destuq@test.com"));

        String codigoComprobanteDuplicado = "BOL-UQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Encomienda e1 = new Encomienda(
                UUID.randomUUID().toString(),
                codigoComprobanteDuplicado,
                idRemitente,
                idDestinatario,
                "SUC-LIM-01",
                "SUC-LIM-01",
                new BigDecimal("3.00"),
                null,
                "Paquete 1",
                new BigDecimal("12.00"),
                EstadoPago.PAGADO,
                EstadoLogistico.REGISTRADO,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        );
        encomiendaAdapter.guardar(e1);

        Encomienda e2 = new Encomienda(
                UUID.randomUUID().toString(),
                codigoComprobanteDuplicado, // Mismo código de comprobante
                idRemitente,
                idDestinatario,
                "SUC-LIM-01",
                "SUC-LIM-01",
                new BigDecimal("4.00"),
                null,
                "Paquete 2",
                new BigDecimal("16.00"),
                EstadoPago.PAGADO,
                EstadoLogistico.REGISTRADO,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        );

        assertThatThrownBy(() -> encomiendaAdapter.guardar(e2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Restricción CHECK: intentar insertar estado_logistico fuera de la lista es rechazado por la base de datos")
    void testRestriccionCheckEstadoLogisticoRechazaValorInvalido() {
        String idRemitente = UUID.randomUUID().toString();
        String idDestinatario = UUID.randomUUID().toString();
        clienteAdapter.guardar(new Cliente(idRemitente, TipoDocumento.DNI, "81" + (int)(Math.random() * 900000 + 100000), "Rem CK1", "981111111", "remck1@test.com"));
        clienteAdapter.guardar(new Cliente(idDestinatario, TipoDocumento.DNI, "82" + (int)(Math.random() * 900000 + 100000), "Dest CK1", "982222222", "destck1@test.com"));

        String sqlInvalido = """
            INSERT INTO encomiendas (
                id_encomienda, codigo_comprobante, id_remitente, id_destinatario,
                id_sucursal_origen, id_sucursal_destino, peso_kg, monto_total,
                estado_pago, estado_logistico, fecha_registro
            ) VALUES (
                ?, ?, ?, ?,
                'SUC-LIM-01', 'SUC-LIM-01', 5.0, 20.0,
                'PAGADO', 'ESTADO_INEXISTENTE_PROHIBIDO', NOW()
            )
        """;

        assertThatThrownBy(() -> jdbcTemplate.update(
                sqlInvalido,
                UUID.randomUUID().toString(),
                "BOL-CK-" + UUID.randomUUID().toString().substring(0, 8),
                idRemitente,
                idDestinatario
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Restricción CHECK: intentar insertar estado_pago fuera de la lista es rechazado por la base de datos")
    void testRestriccionCheckEstadoPagoRechazaValorInvalido() {
        String idRemitente = UUID.randomUUID().toString();
        String idDestinatario = UUID.randomUUID().toString();
        clienteAdapter.guardar(new Cliente(idRemitente, TipoDocumento.DNI, "91" + (int)(Math.random() * 900000 + 100000), "Rem CK2", "991111111", "remck2@test.com"));
        clienteAdapter.guardar(new Cliente(idDestinatario, TipoDocumento.DNI, "92" + (int)(Math.random() * 900000 + 100000), "Dest CK2", "992222222", "destck2@test.com"));

        String sqlInvalido = """
            INSERT INTO encomiendas (
                id_encomienda, codigo_comprobante, id_remitente, id_destinatario,
                id_sucursal_origen, id_sucursal_destino, peso_kg, monto_total,
                estado_pago, estado_logistico, fecha_registro
            ) VALUES (
                ?, ?, ?, ?,
                'SUC-LIM-01', 'SUC-LIM-01', 5.0, 20.0,
                'ESTADO_PAGO_FANTASMA', 'REGISTRADO', NOW()
            )
        """;

        assertThatThrownBy(() -> jdbcTemplate.update(
                sqlInvalido,
                UUID.randomUUID().toString(),
                "BOL-CK2-" + UUID.randomUUID().toString().substring(0, 8),
                idRemitente,
                idDestinatario
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
