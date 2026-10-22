package pe.marvisur.sgldqr.api.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio criptográfico para la generación, firma y validación de tokens JWT (HS256).
 */
@Service
public class JwtServicio {

    private static final Logger log = LoggerFactory.getLogger(JwtServicio.class);

    @Value("${app.jwt.secreto:}")
    private String secreto;

    @Value("${app.jwt.expiracion-minutos:480}")
    private long expiracionMinutos;

    private final Environment environment;
    private SecretKey claveFirma;

    public JwtServicio(Environment environment) {
        this.environment = Objects.requireNonNull(environment, "El environment no puede ser nulo");
    }

    @PostConstruct
    public void inicializar() {
        boolean esProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        if (secreto == null || secreto.trim().isEmpty()) {
            if (esProd) {
                throw new IllegalStateException("FATAL: En perfil 'prod' es obligatorio configurar app.jwt.secreto (variable JWT_SECRET) con al menos 256 bits.");
            }
            log.warn("ADVERTENCIA: app.jwt.secreto no está configurado. Generando clave efímera segura para desarrollo local.");
            this.claveFirma = Jwts.SIG.HS256.key().build();
            return;
        }

        byte[] secretoBytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (secretoBytes.length < 32) {
            throw new IllegalStateException("El secreto JWT (app.jwt.secreto) debe tener al menos 256 bits (32 bytes). Tamaño actual: " + secretoBytes.length + " bytes.");
        }

        this.claveFirma = Keys.hmacShaKeyFor(secretoBytes);
    }

    /**
     * Genera un token JWT firmado para el empleado autenticado.
     *
     * @param empleado datos del empleado
     * @return token JWT en formato compacto
     */
    public String generarToken(EmpleadoAutenticadoDto empleado) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plusSeconds(expiracionMinutos * 60L);

        return Jwts.builder()
                .subject(empleado.id())
                .claim("correo", empleado.correo())
                .claim("nombres", empleado.nombres())
                .claim("apellidos", empleado.apellidos())
                .claim("rol", empleado.rol())
                .claim("idSucursal", empleado.idSucursal())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(claveFirma)
                .compact();
    }

    /**
     * Extrae y valida los claims del token JWT.
     *
     * @param token token JWT
     * @return Claims si el token es válido y no está expirado ni manipulado
     */
    public Optional<Claims> obtenerClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(claveFirma)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (ExpiredJwtException ex) {
            log.debug("Token JWT expirado: {}", ex.getMessage());
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token JWT inválido o manipulado: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Valida si un token JWT es estructural y criptográficamente válido.
     */
    public boolean esTokenValido(String token) {
        return obtenerClaims(token).isPresent();
    }

    /**
     * Obtiene la fecha de expiración en formato ISO-8601 (OffsetDateTime UTC).
     */
    public OffsetDateTime calcularFechaExpiracion() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(expiracionMinutos * 60L);
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }

    public long getExpiracionSegundos() {
        return expiracionMinutos * 60L;
    }
}
