package pe.marvisur.sgldqr.api.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pe.marvisur.sgldqr.api.dto.ClienteInputDto;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaRequest;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.HistorialTrackingSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ParDobleQrSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.TokenQr2EstatusSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.aplicacion.ModalidadPago;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;
import pe.marvisur.sgldqr.puertos.AuditoriaPort;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Pruebas de Atomicidad en Registro de Encomienda y Transaccionalidad de Auditoría (RF-04, RF-07)")
class RegistroEncomiendaAtomicidadTest {

    @Autowired
    private EncomiendaApiServicio encomiendaApiServicio;

    @Autowired
    private ClienteSpringDataRepository clienteRepository;

    @Autowired
    private EncomiendaSpringDataRepository encomiendaRepository;

    @Autowired
    private ParDobleQrSpringDataRepository parRepository;

    @SpyBean
    private TokenQr2EstatusSpringDataRepository tokenRepository;

    @Autowired
    private HistorialTrackingSpringDataRepository trackingRepository;

    @Autowired
    private AuditoriaLogSpringDataRepository auditoriaRepository;

    @Autowired
    private SucursalSpringDataRepository sucursalRepository;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    @Autowired
    private AuditoriaPort auditoriaPort;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final String SUC_LIMA = "SUC-LIM-01";
    private static final String SUC_AQP = "SUC-AQP-01";
    private static final String EMP_REC = "EMP-REC-ATOM";

    @BeforeEach
    void setUp() {
        reset(tokenRepository);

        sucursalRepository.save(new SucursalJpaEntity(SUC_LIMA, "Agencia Lima Central", "Av. Grau 450", "Lima"));
        sucursalRepository.save(new SucursalJpaEntity(SUC_AQP, "Agencia Arequipa", "Av. Ejército 100", "Arequipa"));

        if (!empleadoRepository.existsById(EMP_REC)) {
            empleadoRepository.save(new EmpleadoJpaEntity(
                    EMP_REC, "ROL-TN1-RECEPCION", SUC_LIMA, "44556677",
                    "Recepcionista", "Atomicidad", "rec_atom@marvisur.pe",
                    "$2b$12$goz1tdfrnGCvrV/InZEN3OZS.xvzWnlozTQf2LUs8LoZx9uvYjzEW", true
            ));
        }
    }

    @Test
    @DisplayName("P7 Atomicidad: Si falla el guardado del token QR2, ocurre rollback completo (sin cliente nuevo, encomienda, pareja, tracking ni auditoría)")
    void testRollbackCompleto_SiFallaGuardadoTokenQR2() {
        String dniRemitenteNuevo = "88112233";
        String dniDestinatarioNuevo = "88445566";

        // Asegurar que no existan previamente
        clienteRepository.findByNumeroDocumento(dniRemitenteNuevo).ifPresent(c -> clienteRepository.delete(c));
        clienteRepository.findByNumeroDocumento(dniDestinatarioNuevo).ifPresent(c -> clienteRepository.delete(c));

        long countEncomiendasAntes = encomiendaRepository.count();
        long countParejasAntes = parRepository.count();
        long countTokensAntes = tokenRepository.count();
        long countTrackingAntes = trackingRepository.count();
        long countAuditoriaAntes = auditoriaRepository.count();

        // Simular falla en la persistencia del token QR2
        doThrow(new RuntimeException("Error simulado de base de datos al guardar token QR2"))
                .when(tokenRepository).save(any());

        RegistrarEncomiendaRequest request = new RegistrarEncomiendaRequest(
                new ClienteInputDto(TipoDocumento.DNI, dniRemitenteNuevo, "Remitente Nuevo Atom", "999111222", "rem_atom@test.com"),
                new ClienteInputDto(TipoDocumento.DNI, dniDestinatarioNuevo, "Destinatario Nuevo Atom", "999333444", "dest_atom@test.com"),
                SUC_AQP,
                new BigDecimal("6.50"),
                new BigDecimal("0.0200"),
                "Caja de componentes electrónicos sensibles",
                ModalidadPago.CONTADO
        );

        UsuarioAutenticado usuario = new UsuarioAutenticado(
                EMP_REC, "Recepcionista", "Atomicidad", "rec_atom@marvisur.pe", "ROLE_RECEPCIONISTA", SUC_LIMA
        );

        assertThatThrownBy(() -> encomiendaApiServicio.registrarEncomienda(request, usuario, "127.0.0.1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error simulado de base de datos al guardar token QR2");

        // VERIFICACIÓN DE ROLLBACK COMPLETO:
        // 1. Clientes nuevos creados en la transacción NO deben existir
        assertThat(clienteRepository.findByNumeroDocumento(dniRemitenteNuevo)).isEmpty();
        assertThat(clienteRepository.findByNumeroDocumento(dniDestinatarioNuevo)).isEmpty();

        // 2. Conteo de encomiendas debe permanecer idéntico
        assertThat(encomiendaRepository.count()).isEqualTo(countEncomiendasAntes);

        // 3. Conteo de parejas QR y tokens debe permanecer idéntico
        assertThat(parRepository.count()).isEqualTo(countParejasAntes);
        assertThat(tokenRepository.count()).isEqualTo(countTokensAntes);

        // 4. Historial de tracking no debe registrar ningún evento huérfano
        assertThat(trackingRepository.count()).isEqualTo(countTrackingAntes);

        // 5. Auditoría de negocio REGISTRO_ENCOMIENDA debe haberse revertido con la transacción
        assertThat(auditoriaRepository.count()).isEqualTo(countAuditoriaAntes);
    }

    @Test
    @DisplayName("P7/P8 Auditoría: REQUIRES_NEW solo en login; eventos de negocio participan en la transacción y revierten en rollback")
    void testAuditoriaTransaccionalidad_EventosDeNegocioReviertenYLoginPermanece() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        String idRegistroPrueba = "TEST-REG-001";

        try {
            txTemplate.execute(status -> {
                // Evento de autenticación: debe usar REQUIRES_NEW internamente y persistir
                auditoriaPort.registrar(EMP_REC, "LOGIN_FALLIDO", "empleados", EMP_REC, "10.0.0.1");

                // Evento de negocio: debe usar REQUIRED y revertirse en rollback
                auditoriaPort.registrar(EMP_REC, "REGISTRO_ENCOMIENDA", "encomiendas", idRegistroPrueba, "10.0.0.1");

                // Forzar rollback de la transacción externa
                throw new RuntimeException("Forzando rollback de transacción de negocio de prueba");
            });
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage()).contains("Forzando rollback");
        }

        // El evento de negocio debe haberse revertido
        List<pe.marvisur.sgldqr.dominio.modelo.AuditoriaLog> logsNegocio = auditoriaPort.buscarPorRegistro("encomiendas", idRegistroPrueba);
        assertThat(logsNegocio).isEmpty();

        // El evento de login (REQUIRES_NEW) debe haberse guardado exitosamente a pesar del rollback
        List<pe.marvisur.sgldqr.dominio.modelo.AuditoriaLog> logsLogin = auditoriaPort.buscarPorRegistro("empleados", EMP_REC);
        assertThat(logsLogin).anyMatch(l -> "LOGIN_FALLIDO".equals(l.getAccion()));
    }
}
