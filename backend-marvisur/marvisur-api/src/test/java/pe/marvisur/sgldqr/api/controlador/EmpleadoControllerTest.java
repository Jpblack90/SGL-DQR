package pe.marvisur.sgldqr.api.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pe.marvisur.sgldqr.api.dto.ActualizarEmpleadoRequest;
import pe.marvisur.sgldqr.api.dto.CambiarEstadoEmpleadoRequest;
import pe.marvisur.sgldqr.api.dto.CrearEmpleadoRequest;
import pe.marvisur.sgldqr.api.dto.RestablecerPasswordRequest;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;
import pe.marvisur.sgldqr.api.seguridad.dto.LoginRequest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración — Gestión de Empleados y Personal Operativo (RF-03)")
class EmpleadoControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("marvisur_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtServicio jwtServicio;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    @Autowired
    private AuditoriaLogSpringDataRepository auditoriaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenRecepcionista;
    private String tokenAlmacenero;
    private String tokenChofer;

    @BeforeEach
    void setUp() {
        auditoriaRepository.deleteAllInBatch();

        // Preservar EMP-ADMIN-01 de la migración V7 y limpiar los demás empleados de prueba
        for (EmpleadoJpaEntity emp : empleadoRepository.findAll()) {
            if (!"EMP-ADMIN-01".equals(emp.getIdEmpleado())) {
                empleadoRepository.delete(emp);
            }
        }

        tokenAdmin = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ADMIN-01", "Admin", "General", "admin@marvisur.pe",
                ConstantesSeguridad.AUTH_ADMINISTRADOR, "SUC-LIM-01"
        ));

        tokenRecepcionista = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                UUID.randomUUID().toString(), "Recepcionista", "Turno", "rec@marvisur.pe",
                ConstantesSeguridad.AUTH_RECEPCIONISTA, "SUC-LIM-01"
        ));

        tokenAlmacenero = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                UUID.randomUUID().toString(), "Almacenero", "Turno", "alm@marvisur.pe",
                ConstantesSeguridad.AUTH_ALMACENERO, "SUC-LIM-01"
        ));

        tokenChofer = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                UUID.randomUUID().toString(), "Chofer", "Ruta", "chf@marvisur.pe",
                ConstantesSeguridad.AUTH_CHOFER, "SUC-LIM-01"
        ));
    }

    private String generarDniAleatorio() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(10000000, 99999999));
    }

    @Test
    @DisplayName("Alta de empleado exitosa registra en BD y crea fila de auditoría sin password en JSON")
    void altaEmpleadoExitoso() throws Exception {
        String dni = generarDniAleatorio();
        String correo = "carlos." + UUID.randomUUID().toString().substring(0, 6) + "@marvisur.pe";
        CrearEmpleadoRequest request = new CrearEmpleadoRequest(
                dni, "Carlos", "Quispe Mamani", correo,
                ConstantesSeguridad.ID_ROL_RECEPCIONISTA, "SUC-LIM-01", "ClaveSegura2026"
        );

        mockMvc.perform(post("/api/v1/empleados")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.idEmpleado").isNotEmpty())
                .andExpect(jsonPath("$.dni", is(dni)))
                .andExpect(jsonPath("$.nombres", is("Carlos")))
                .andExpect(jsonPath("$.apellidos", is("Quispe Mamani")))
                .andExpect(jsonPath("$.correo", is(correo.toLowerCase())))
                .andExpect(jsonPath("$.idRol", is(ConstantesSeguridad.ID_ROL_RECEPCIONISTA)))
                .andExpect(jsonPath("$.nombreRol", is("Recepcionista")))
                .andExpect(jsonPath("$.idSucursal", is("SUC-LIM-01")))
                .andExpect(jsonPath("$.estado", is(true)))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Verificar auditoría
        List<AuditoriaLogJpaEntity> logs = auditoriaRepository.findAll();
        assertThat(logs).hasSize(1);
        AuditoriaLogJpaEntity log = logs.get(0);
        assertThat(log.getAccion()).isEqualTo("EMPLEADO_CREADO");
        assertThat(log.getTablaAfectada()).isEqualTo("empleados");
        assertThat(log.getIdEmpleado()).isEqualTo("EMP-ADMIN-01");
    }

    @Test
    @DisplayName("Alta con DNI duplicado retorna 409 DNI_DUPLICADO sin fila de auditoría")
    void altaEmpleadoDniDuplicado() throws Exception {
        CrearEmpleadoRequest request = new CrearEmpleadoRequest(
                "00000001", "Duplicado", "Dni", "dup@marvisur.pe",
                ConstantesSeguridad.ID_ROL_RECEPCIONISTA, "SUC-LIM-01", "ClaveSegura2026"
        );

        mockMvc.perform(post("/api/v1/empleados")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("DNI_DUPLICADO")));

        assertThat(auditoriaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Alta con correo duplicado (insensible a mayúsculas) retorna 409 CORREO_DUPLICADO sin auditoría")
    void altaEmpleadoCorreoDuplicado() throws Exception {
        String dni = generarDniAleatorio();
        CrearEmpleadoRequest request = new CrearEmpleadoRequest(
                dni, "Admin", "Clon", "ADMIN@MARVISUR.PE",
                ConstantesSeguridad.ID_ROL_RECEPCIONISTA, "SUC-LIM-01", "ClaveSegura2026"
        );

        mockMvc.perform(post("/api/v1/empleados")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("CORREO_DUPLICADO")));

        assertThat(auditoriaRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Lectura por ID retorna empleado sin revelar contraseñas")
    void lecturaEmpleadoPorId() throws Exception {
        mockMvc.perform(get("/api/v1/empleados/EMP-ADMIN-01")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEmpleado", is("EMP-ADMIN-01")))
                .andExpect(jsonPath("$.dni", is("00000001")))
                .andExpect(jsonPath("$.correo", is("admin@marvisur.pe")))
                .andExpect(jsonPath("$.nombreRol", is("Administrador")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Listado con filtros y paginación")
    void listadoConFiltros() throws Exception {
        mockMvc.perform(get("/api/v1/empleados")
                        .param("estado", "ACTIVOS")
                        .param("q", "Administrador")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].dni", is("00000001")));
    }

    @Test
    @DisplayName("Edición de empleado actualiza datos y genera auditoría")
    void edicionEmpleadoExitoso() throws Exception {
        String idEmp = UUID.randomUUID().toString();
        String dni = generarDniAleatorio();
        EmpleadoJpaEntity entity = new EmpleadoJpaEntity(
                idEmp, ConstantesSeguridad.ID_ROL_RECEPCIONISTA, "SUC-LIM-01",
                dni, "Pedro", "Navaja", "pedro@marvisur.pe",
                passwordEncoder.encode("ClaveSegura2026"), true
        );
        empleadoRepository.save(entity);

        ActualizarEmpleadoRequest request = new ActualizarEmpleadoRequest(
                "Pedro Antonio", "Navaja Flores", "pedro.navaja@marvisur.pe",
                ConstantesSeguridad.ID_ROL_ALMACENERO, "SUC-LIM-01"
        );

        mockMvc.perform(put("/api/v1/empleados/" + idEmp)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombres", is("Pedro Antonio")))
                .andExpect(jsonPath("$.apellidos", is("Navaja Flores")))
                .andExpect(jsonPath("$.correo", is("pedro.navaja@marvisur.pe")))
                .andExpect(jsonPath("$.idRol", is(ConstantesSeguridad.ID_ROL_ALMACENERO)));

        List<AuditoriaLogJpaEntity> logs = auditoriaRepository.findAll();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAccion()).isEqualTo("EMPLEADO_ACTUALIZADO");
    }

    @Test
    @DisplayName("Baja lógica y posterior reactivación registran auditoría y bloquean login")
    void bajaYReactivacionConBloqueoDeLogin() throws Exception {
        String idEmp = UUID.randomUUID().toString();
        String dni = generarDniAleatorio();
        String correo = "test.baja." + dni + "@marvisur.pe";
        String password = "ClaveSegura2026";
        EmpleadoJpaEntity entity = new EmpleadoJpaEntity(
                idEmp, ConstantesSeguridad.ID_ROL_RECEPCIONISTA, "SUC-LIM-01",
                dni, "Baja", "Prueba", correo,
                passwordEncoder.encode(password), true
        );
        empleadoRepository.save(entity);

        // 1. Dar de baja lógica
        CambiarEstadoEmpleadoRequest bajaReq = new CambiarEstadoEmpleadoRequest(false);
        mockMvc.perform(patch("/api/v1/empleados/" + idEmp + "/estado")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bajaReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is(false)));

        // 2. Verificar que login es rechazado tras la baja
        LoginRequest loginReq = new LoginRequest(correo, password);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is("CREDENCIALES_INVALIDAS")));

        // 3. Reactivar empleado
        CambiarEstadoEmpleadoRequest reactivaReq = new CambiarEstadoEmpleadoRequest(true);
        mockMvc.perform(patch("/api/v1/empleados/" + idEmp + "/estado")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reactivaReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is(true)));

        // 4. Verificar auditoría: EMPLEADO_DADO_DE_BAJA, LOGIN_FALLIDO, EMPLEADO_REACTIVADO
        List<AuditoriaLogJpaEntity> logs = auditoriaRepository.findAll();
        assertThat(logs).extracting(AuditoriaLogJpaEntity::getAccion)
                .contains("EMPLEADO_DADO_DE_BAJA", "LOGIN_FALLIDO", "EMPLEADO_REACTIVADO");
    }

    @Test
    @DisplayName("Protección al último administrador: rechaza baja si count <= 1")
    void proteccionUltimoAdministrador() throws Exception {
        CambiarEstadoEmpleadoRequest request = new CambiarEstadoEmpleadoRequest(false);
        mockMvc.perform(patch("/api/v1/empleados/EMP-ADMIN-01/estado")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("ACCION_NO_PERMITIDA"))); // Ocurre autobaja primero
    }

    @Test
    @DisplayName("Autobaja rechazada con 409 ACCION_NO_PERMITIDA aun existiendo otro admin")
    void autobajaRechazadaConVariosAdmins() throws Exception {
        // Crear segundo administrador activo
        String idAdmin2 = UUID.randomUUID().toString();
        EmpleadoJpaEntity entity = new EmpleadoJpaEntity(
                idAdmin2, ConstantesSeguridad.ID_ROL_ADMINISTRADOR, "SUC-LIM-01",
                generarDniAleatorio(), "Admin", "Dos", "admin2@marvisur.pe",
                passwordEncoder.encode("ClaveSegura2026"), true
        );
        empleadoRepository.save(entity);

        // Admin 1 intenta darse de baja a sí mismo
        CambiarEstadoEmpleadoRequest request = new CambiarEstadoEmpleadoRequest(false);
        mockMvc.perform(patch("/api/v1/empleados/EMP-ADMIN-01/estado")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("ACCION_NO_PERMITIDA")));
    }

    @Test
    @DisplayName("Restablecimiento de contraseña por administrador")
    void restablecerPasswordExitoso() throws Exception {
        String idEmp = UUID.randomUUID().toString();
        String correo = "reset." + idEmp.substring(0, 5) + "@marvisur.pe";
        EmpleadoJpaEntity entity = new EmpleadoJpaEntity(
                idEmp, ConstantesSeguridad.ID_ROL_RECEPCIONISTA, "SUC-LIM-01",
                generarDniAleatorio(), "Empleado", "Reset", correo,
                passwordEncoder.encode("ClaveAntigua2026"), true
        );
        empleadoRepository.save(entity);

        RestablecerPasswordRequest request = new RestablecerPasswordRequest("NuevaClave2026");
        mockMvc.perform(post("/api/v1/empleados/" + idEmp + "/password")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje", is("Contraseña restablecida exitosamente")));

        // Login con nueva clave tiene éxito
        LoginRequest loginReq = new LoginRequest(correo, "NuevaClave2026");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Control de acceso RBAC: 403 Forbidden para TN1, TN2 y TN4")
    void rbacDeniegaAccesoParaNoAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/empleados")
                        .header("Authorization", "Bearer " + tokenRecepcionista))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));

        mockMvc.perform(get("/api/v1/empleados")
                        .header("Authorization", "Bearer " + tokenAlmacenero))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));

        mockMvc.perform(get("/api/v1/empleados")
                        .header("Authorization", "Bearer " + tokenChofer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));
    }

    @Test
    @DisplayName("Petición sin token retorna 401 Unauthorized")
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/empleados"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is("NO_AUTORIZADO")));
    }
}
