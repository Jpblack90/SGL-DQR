package pe.marvisur.sgldqr.api.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;
import pe.marvisur.sgldqr.api.seguridad.dto.LoginRequest;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas MockMvc de Autenticación JWT y Control de Acceso Basado en Roles (RBAC)")
class AutenticacionYRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtServicio jwtServicio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.jwt.secreto}")
    private String secretoTest;

    private static final String CORREO_ADMIN = "admin@marvisur.pe";
    private static final String PASSWORD_ADMIN = "AdminMarvisur2026!";

    @BeforeEach
    void setUp() {
        // Aseguramos que el usuario administrador inicial de la semilla V7 existe y está activo
        if (!empleadoRepository.existsById("EMP-ADMIN-01")) {
            EmpleadoJpaEntity admin = new EmpleadoJpaEntity(
                    "EMP-ADMIN-01",
                    "ROL-TN3-ADMIN",
                    "SUC-LIM-01",
                    "00000001",
                    "Administrador",
                    "Sistema Marvisur",
                    CORREO_ADMIN,
                    passwordEncoder.encode(PASSWORD_ADMIN),
                    true
            );
            empleadoRepository.save(admin);
        }
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: credenciales correctas retorna 200, JWT y audita LOGIN_EXITOSO")
    void testLoginCorrecto_devuelve200ConTokenYAuditoria() throws Exception {
        LoginRequest request = new LoginRequest(CORREO_ADMIN, PASSWORD_ADMIN);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipo", is("Bearer")))
                .andExpect(jsonPath("$.expiracion").exists())
                .andExpect(jsonPath("$.expiraEnSegundos", is(28800)))
                .andExpect(jsonPath("$.empleado.id", is("EMP-ADMIN-01")))
                .andExpect(jsonPath("$.empleado.rol", is(ConstantesSeguridad.AUTH_ADMINISTRADOR)))
                .andExpect(jsonPath("$.empleado.correo", is(CORREO_ADMIN)));

        // Verificar registro de auditoría en BD
        Integer countLoginExitoso = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_logs WHERE accion = 'LOGIN_EXITOSO' AND id_empleado = 'EMP-ADMIN-01'",
                Integer.class
        );
        assertThat(countLoginExitoso).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: contraseña incorrecta retorna 401 genérico y audita LOGIN_FALLIDO")
    void testLoginContrasenaIncorrecta_devuelve401GenericoYAuditaFallo() throws Exception {
        LoginRequest request = new LoginRequest(CORREO_ADMIN, "ClaveTotalmenteErronea123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is("CREDENCIALES_INVALIDAS")))
                .andExpect(jsonPath("$.detail", containsString("Credenciales inválidas")));

        // Debe registrar LOGIN_FALLIDO porque el empleado sí existe (cumple FK)
        Integer countLoginFallido = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_logs WHERE accion = 'LOGIN_FALLIDO' AND id_empleado = 'EMP-ADMIN-01'",
                Integer.class
        );
        assertThat(countLoginFallido).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: correo no existente retorna 401 genérico sin auditar (FK restrictiva)")
    void testLoginCorreoInexistente_devuelve401GenericoSinAuditoriaFK() throws Exception {
        LoginRequest request = new LoginRequest("desconocido@marvisur.pe", "CualquierClave123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is("CREDENCIALES_INVALIDAS")))
                .andExpect(jsonPath("$.detail", containsString("Credenciales inválidas")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: empleado dado de baja (estado=false) retorna 401 y audita LOGIN_FALLIDO")
    void testLoginEmpleadoDadoDeBaja_devuelve401GenericoYAuditaFallo() throws Exception {
        String idInactivo = UUID.randomUUID().toString();
        String correoInactivo = "inactivo_" + idInactivo.substring(0, 6) + "@marvisur.pe";
        String dniInactivo = "99" + (int) (Math.random() * 900000 + 100000);

        EmpleadoJpaEntity inactivo = new EmpleadoJpaEntity(
                idInactivo,
                "ROL-TN1-RECEPCION",
                "SUC-LIM-01",
                dniInactivo,
                "Juan",
                "Baja Temporal",
                correoInactivo,
                passwordEncoder.encode("PasswordBaja123!"),
                false // Baja lógica
        );
        empleadoRepository.save(inactivo);

        LoginRequest request = new LoginRequest(correoInactivo, "PasswordBaja123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is("CREDENCIALES_INVALIDAS")));

        Integer countFallido = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_logs WHERE accion = 'LOGIN_FALLIDO' AND id_empleado = ?",
                Integer.class,
                idInactivo
        );
        assertThat(countFallido).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("Ruta protegida: token ausente retorna 401 NO_AUTORIZADO")
    void testRutaProtegida_tokenAusenteRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is("NO_AUTORIZADO")));
    }

    @Test
    @DisplayName("Ruta protegida: token manipulado retorna 401 NO_AUTORIZADO")
    void testRutaProtegida_tokenManipuladoRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer token_falso_y_manipulado_invalido_12345"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is("NO_AUTORIZADO")));
    }

    @Test
    @DisplayName("Ruta protegida: token expirado retorna 401 NO_AUTORIZADO")
    void testRutaProtegida_tokenExpiradoRetorna401() throws Exception {
        // Crear token con fecha de expiración en el pasado
        SecretKey key = Keys.hmacShaKeyFor(secretoTest.getBytes(StandardCharsets.UTF_8));
        Date ahora = new Date();
        Date expirado = new Date(ahora.getTime() - 60000L); // 1 minuto en el pasado

        String tokenExpirado = Jwts.builder()
                .subject("EMP-ADMIN-01")
                .claim("correo", CORREO_ADMIN)
                .claim("rol", ConstantesSeguridad.AUTH_ADMINISTRADOR)
                .issuedAt(new Date(ahora.getTime() - 120000L))
                .expiration(expirado)
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenExpirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is("NO_AUTORIZADO")));
    }

    @Test
    @DisplayName("Control de Roles (@PreAuthorize): 403 al rol equivocado y 200 al rol autorizado")
    void testRbacPreAuthorize_rolEquivocadoDa403YRolCorrectoDa200() throws Exception {
        // Token para Administrador (ROLE_ADMINISTRADOR)
        EmpleadoAutenticadoDto adminDto = new EmpleadoAutenticadoDto(
                "EMP-ADMIN-01", "Admin", "Marvisur", CORREO_ADMIN, ConstantesSeguridad.AUTH_ADMINISTRADOR, "SUC-LIM-01"
        );
        String tokenAdmin = jwtServicio.generarToken(adminDto);

        // Token para Recepcionista (ROLE_RECEPCIONISTA)
        EmpleadoAutenticadoDto recepcionistaDto = new EmpleadoAutenticadoDto(
                UUID.randomUUID().toString(), "Recepcionista", "Test", "recep@marvisur.pe", ConstantesSeguridad.AUTH_RECEPCIONISTA, "SUC-LIM-01"
        );
        String tokenRecepcionista = jwtServicio.generarToken(recepcionistaDto);

        // 1. Acceso a /api/v1/test-rbac/admin con token de RECEPCIONISTA -> 403 ACCESO_DENEGADO
        mockMvc.perform(get("/api/v1/test-rbac/admin")
                        .header("Authorization", "Bearer " + tokenRecepcionista))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));

        // 2. Acceso a /api/v1/test-rbac/admin con token de ADMINISTRADOR -> 200 OK
        mockMvc.perform(get("/api/v1/test-rbac/admin")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", containsString("Acceso concedido a Administrador")));
    }

    @Test
    @DisplayName("Endpoints públicos: /api/v1/publico/** sin token retorna 200 (no 401)")
    void testEndpointsPublicosSinTokenRetornan200() throws Exception {
        mockMvc.perform(get("/api/v1/publico/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado", is("OK")))
                .andExpect(jsonPath("$.mensaje", containsString("Endpoint público")));
    }

    @Test
    @DisplayName("Actuator y Swagger: /actuator/health y /v3/api-docs accesibles sin token")
    void testActuatorYSwaggerSinTokenRetornan200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me: con token válido devuelve perfil del empleado autenticado")
    void testGetMeConTokenValidoRetornaPerfil() throws Exception {
        EmpleadoAutenticadoDto adminDto = new EmpleadoAutenticadoDto(
                "EMP-ADMIN-01", "Administrador", "Sistema Marvisur", CORREO_ADMIN, ConstantesSeguridad.AUTH_ADMINISTRADOR, "SUC-LIM-01"
        );
        String tokenAdmin = jwtServicio.generarToken(adminDto);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("EMP-ADMIN-01")))
                .andExpect(jsonPath("$.correo", is(CORREO_ADMIN)))
                .andExpect(jsonPath("$.rol", is(ConstantesSeguridad.AUTH_ADMINISTRADOR)))
                .andExpect(jsonPath("$.idSucursal", is("SUC-LIM-01")));
    }
}
