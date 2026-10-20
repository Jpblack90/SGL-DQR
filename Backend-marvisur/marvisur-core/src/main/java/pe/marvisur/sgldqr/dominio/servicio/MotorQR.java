package pe.marvisur.sgldqr.dominio.servicio;

import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.dominio.modelo.MotivoInactivacion;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.ResultadoValidacion;
import pe.marvisur.sgldqr.dominio.modelo.ResultadoValidacionCruzada;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Servicio de dominio central que implementa el Motor Criptográfico del Doble QR (RF-07, RNF-01, RNF-03).
 *
 * <p><strong>Principios de diseño y seguridad:</strong></p>
 * <ul>
 *   <li>Genera pares enlazados: QR1 Operativo (identificador visible del bulto) y QR2 Llave Digital (token aleatorio
 *       de un solo uso generado con {@link SecureRandom}).</li>
 *   <li>Almacena exclusivamente el hash SHA-256 del QR2. El token en texto plano se entrega una sola vez en la respuesta
 *       de generación y jamás se persiste ni se imprime en logs.</li>
 *   <li>La validación cruzada compara hashes en tiempo constante vía {@link MessageDigest#isEqual(byte[], byte[])}
 *       para prevenir ataques de canal lateral por temporización (timing attacks).</li>
 *   <li>Si el pago de la encomienda está {@link EstadoPago#PENDIENTE}, la entrega se bloquea devolviendo
 *       {@link ResultadoValidacion#FALLIDO_PAGO_PENDIENTE} sin invalidar el token.</li>
 *   <li>Calcula y audita la latencia de procesamiento en milisegundos para verificar el cumplimiento del RNF-03 (&le; 1000 ms).</li>
 * </ul>
 */
public class MotorQR {

    private static final String ALGORITMO_HASH = "SHA-256";
    private static final int LONGITUD_TOKEN_BYTES = 32; // 256 bits de entropía criptográfica
    private static final String PREFIJO_QR1 = "MARVISUR:QR1:";

    private final SecureRandom secureRandom;
    private final Clock clock;

    public MotorQR() {
        this(new SecureRandom(), Clock.systemDefaultZone());
    }

    public MotorQR(Clock clock) {
        this(new SecureRandom(), clock);
    }

    public MotorQR(SecureRandom secureRandom, Clock clock) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "El secureRandom no puede ser nulo");
        this.clock = (clock != null) ? clock : Clock.systemDefaultZone();
    }

    /**
     * Genera un nuevo par enlazado Doble QR para una encomienda.
     *
     * @param encomienda encomienda origen
     * @return {@link ParDobleQR} con el QR1 operativo, el hash del QR2, y el token QR2 en claro
     *         entregado por única vez.
     */
    public ParDobleQR generarDobleQR(Encomienda encomienda) {
        Objects.requireNonNull(encomienda, "La encomienda no puede ser nula");
        LocalDateTime ahora = obtenerFechaActual();

        // 1. QR1: Payload operativo enlazado a la encomienda
        String uuidQr1Operativo = PREFIJO_QR1 + encomienda.getIdEncomienda();

        // 2. QR2: Token de alta entropía con SecureRandom (32 bytes -> 64 caracteres hexadecimales)
        byte[] tokenAleatorioBytes = new byte[LONGITUD_TOKEN_BYTES];
        this.secureRandom.nextBytes(tokenAleatorioBytes);
        String qr2TokenEnClaro = HexFormat.of().formatHex(tokenAleatorioBytes);

        // 3. Hash criptográfico irreversible SHA-256 del QR2
        String hashQr2 = calcularSha256Hex(qr2TokenEnClaro);

        // 4. Par Doble QR con token en claro por única vez y estatus inicial ACTIVO
        return ParDobleQR.crearGenerado(
                encomienda.getIdEncomienda(),
                uuidQr1Operativo,
                hashQr2,
                ahora,
                qr2TokenEnClaro
        );
    }

    /**
     * Realiza la validación cruzada entre el QR1 leído del bulto físico y el QR2 digital presentado por el cliente.
     *
     * @param qr1        contenido escaneado del QR1 físico
     * @param qr2        token en texto plano escaneado del QR2 digital del cliente
     * @param encomienda encomienda a la que debe corresponder la entrega
     * @param par        par Doble QR registrado previamente
     * @return resultado de la validación con estado, latencia en ms y mensaje descriptivo
     */
    public ResultadoValidacionCruzada validarCruzado(String qr1, String qr2, Encomienda encomienda, ParDobleQR par) {
        long nanoInicio = System.nanoTime();
        LocalDateTime fechaValidacion = obtenerFechaActual();

        if (qr1 == null || qr1.isBlank() || qr2 == null || qr2.isBlank() || encomienda == null || par == null) {
            long latencia = calcularLatenciaMs(nanoInicio);
            return ResultadoValidacionCruzada.tokenInvalido(latencia, "Parámetros de validación nulos o incompletos.", fechaValidacion);
        }

        // 1. Validar que QR1 corresponda a la encomienda y al par
        boolean coincideParEncomienda = par.getIdEncomienda().equals(encomienda.getIdEncomienda());
        boolean coincideQr1 = MessageDigest.isEqual(qr1.getBytes(StandardCharsets.UTF_8), par.getUuidQr1Operativo().getBytes(StandardCharsets.UTF_8))
                || qr1.equals(encomienda.getIdEncomienda())
                || qr1.endsWith(":" + encomienda.getIdEncomienda());

        if (!coincideParEncomienda || !coincideQr1) {
            long latencia = calcularLatenciaMs(nanoInicio);
            return ResultadoValidacionCruzada.tokenInvalido(latencia, "El código QR1 físico no corresponde a la encomienda.", fechaValidacion);
        }

        // 2. Validar que el token QR2 no esté ya invalidado o expirado
        if (!par.isTokenActivo()) {
            long latencia = calcularLatenciaMs(nanoInicio);
            return ResultadoValidacionCruzada.tokenInvalido(latencia, "El token QR2 ya ha sido invalidado o ha expirado.", fechaValidacion);
        }

        // 3. Validar hash del QR2 en tiempo constante
        String hashQr2Calculado = calcularSha256Hex(qr2);
        boolean hashCoincide = MessageDigest.isEqual(
                hashQr2Calculado.getBytes(StandardCharsets.UTF_8),
                par.getHashQr2LlaveDigital().getBytes(StandardCharsets.UTF_8)
        );

        if (!hashCoincide) {
            long latencia = calcularLatenciaMs(nanoInicio);
            return ResultadoValidacionCruzada.tokenInvalido(latencia, "El código QR2 no coincide con la llave digital registrada.", fechaValidacion);
        }

        // 4. Validar estado de pago: si es PENDIENTE (Contra Entrega), se bloquea la entrega sin invalidar el token
        if (encomienda.getEstadoPago() == EstadoPago.PENDIENTE) {
            long latencia = calcularLatenciaMs(nanoInicio);
            return ResultadoValidacionCruzada.pagoPendiente(latencia, fechaValidacion);
        }

        // 5. Todo válido: invalidar de forma irreversible el token QR2 y registrar éxito
        par.invalidarToken(MotivoInactivacion.ENTREGA_EXITOSA, fechaValidacion);
        long latencia = calcularLatenciaMs(nanoInicio);
        return ResultadoValidacionCruzada.exitoso(latencia, fechaValidacion);
    }

    /**
     * Calcula el hash criptográfico SHA-256 de una cadena de texto y lo retorna en formato hexadecimal.
     */
    public String calcularSha256Hex(String texto) {
        Objects.requireNonNull(texto, "El texto para calcular hash no puede ser nulo");
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);
            byte[] hashBytes = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash no disponible: " + ALGORITMO_HASH, e);
        }
    }

    private LocalDateTime obtenerFechaActual() {
        return LocalDateTime.now(this.clock);
    }

    private long calcularLatenciaMs(long nanoInicio) {
        return Math.max(0, (System.nanoTime() - nanoInicio) / 1_000_000);
    }
}
