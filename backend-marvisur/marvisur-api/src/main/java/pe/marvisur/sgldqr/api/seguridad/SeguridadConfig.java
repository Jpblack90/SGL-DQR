package pe.marvisur.sgldqr.api.seguridad;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración central de seguridad Spring Security:
 * - Modo sin sesión (stateless) basado en JWT.
 * - CSRF desactivado para la API REST.
 * - Manejadores personalizados de excepción (RFC 7807 ProblemDetail).
 * - Control de acceso basado en roles con soporte de @PreAuthorize.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SeguridadConfig {

    private final FiltroAutenticacionJwt filtroJwt;
    private final FiltroRateLimitingPublico filtroRateLimitingPublico;
    private final PuntoEntradaAutenticacionJwt puntoEntrada;
    private final ManejadorAccesoDenegadoJwt manejadorAccesoDenegado;
    private final CorsConfigurationSource corsConfigurationSource;

    public SeguridadConfig(FiltroAutenticacionJwt filtroJwt,
                           FiltroRateLimitingPublico filtroRateLimitingPublico,
                           PuntoEntradaAutenticacionJwt puntoEntrada,
                           ManejadorAccesoDenegadoJwt manejadorAccesoDenegado,
                           @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource) {
        this.filtroJwt = filtroJwt;
        this.filtroRateLimitingPublico = filtroRateLimitingPublico;
        this.puntoEntrada = puntoEntrada;
        this.manejadorAccesoDenegado = manejadorAccesoDenegado;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desactivar CSRF solo para la API REST sin sesión
                .csrf(AbstractHttpConfigurer::disable)
                // Habilitar CORS con la configuración centralizada
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Sesión completamente stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Manejo de excepciones de seguridad con ProblemDetail RFC 7807
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(puntoEntrada)
                        .accessDeniedHandler(manejadorAccesoDenegado)
                )
                // Reglas de autorización de rutas
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas explícitas
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/publico/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Toda otra ruta bajo /api/v1/** requiere autenticación
                        .requestMatchers("/api/v1/**").authenticated()
                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )
                // Inserción del filtro JWT antes del filtro estándar de usuario/contraseña
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
                // Inserción del filtro de limitación de tasa antes del filtro JWT
                .addFilterBefore(filtroRateLimitingPublico, FiltroAutenticacionJwt.class);

        return http.build();
    }
}
