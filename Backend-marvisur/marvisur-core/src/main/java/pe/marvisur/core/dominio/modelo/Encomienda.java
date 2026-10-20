package pe.marvisur.core.dominio.modelo;

import pe.marvisur.core.dominio.excepcion.ExcepcionTransicionInvalida;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad principal del dominio SGL-DQR.
 * <p>
 * Representa una encomienda (paquete) enviada entre agencias de Expreso Marvisur.
 * Gestiona su propio ciclo de vida mediante {@link #transicionarEstado(EstadoEnvio)}
 * y calcula la tarifa de envío según el peso del paquete.
 * </p>
 *
 * <p><strong>Reglas de negocio clave:</strong></p>
 * <ul>
 *   <li>Tarifa mínima: S/ 5.00. Fórmula: 5 + (peso × 2.50) con mínimo de 5.</li>
 *   <li>Si {@code estadoPago == PENDIENTE} y el estado llega a {@code EN_ALMACEN_DESTINO},
 *       la encomienda puede bloquearse con {@link #bloquearPorPagoContraEntrega()}.</li>
 *   <li>Las llaves primarias son UUID generadas en el cliente (arquitectura offline-first).</li>
 * </ul>
 */
public class Encomienda {

    // -------------------------------------------------------------------------
    // Constantes de tarifa
    // -------------------------------------------------------------------------

    /** Tarifa base fija en soles (S/ 5.00). */
    private static final BigDecimal TARIFA_BASE = new BigDecimal("5.00");

    /** Costo adicional por kilogramo (S/ 2.50/kg). */
    private static final BigDecimal COSTO_POR_KG = new BigDecimal("2.50");

    // -------------------------------------------------------------------------
    // Mapa de transiciones de estado válidas
    // -------------------------------------------------------------------------
    private static final Map<EstadoEnvio, Set<EstadoEnvio>> TRANSICIONES_VALIDAS = Map.of(
            EstadoEnvio.REGISTRADA,          EnumSet.of(EstadoEnvio.EN_TRANSITO, EstadoEnvio.CANCELADA),
            EstadoEnvio.EN_TRANSITO,         EnumSet.of(EstadoEnvio.EN_ALMACEN_DESTINO, EstadoEnvio.CANCELADA),
            EstadoEnvio.EN_ALMACEN_DESTINO,  EnumSet.of(EstadoEnvio.BLOQUEADA, EstadoEnvio.ENTREGADA, EstadoEnvio.CANCELADA),
            EstadoEnvio.BLOQUEADA,           EnumSet.of(EstadoEnvio.ENTREGADA, EstadoEnvio.CANCELADA),
            EstadoEnvio.ENTREGADA,           EnumSet.noneOf(EstadoEnvio.class),
            EstadoEnvio.CANCELADA,           EnumSet.noneOf(EstadoEnvio.class)
    );

    // -------------------------------------------------------------------------
    // Atributos
    // -------------------------------------------------------------------------

    /** Identificador único de la encomienda (UUID). */
    private final String id;

    /** Nombre completo del remitente. */
    private String remitente;

    /** Nombre completo del destinatario. */
    private String destinatario;

    /** Peso del paquete en kilogramos. */
    private double peso;

    /** Tarifa calculada en soles peruanos (S/.). */
    private BigDecimal tarifa;

    /** Estado actual del pago. */
    private EstadoPago estadoPago;

    /** Estado actual del ciclo de vida del envío. */
    private EstadoEnvio estadoEnvio;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Construye una nueva encomienda generando automáticamente su UUID.
     *
     * @param remitente   nombre completo del remitente (no nulo ni vacío)
     * @param destinatario nombre completo del destinatario (no nulo ni vacío)
     * @param peso         peso en kilogramos (debe ser positivo)
     * @param estadoPago   estado inicial del pago
     */
    public Encomienda(String remitente, String destinatario, double peso, EstadoPago estadoPago) {
        this.id = UUID.randomUUID().toString();
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.peso = peso;
        this.estadoPago = estadoPago;
        this.estadoEnvio = EstadoEnvio.REGISTRADA;
        this.tarifa = calcularTarifa();
    }

    /**
     * Constructor completo para reconstruir una encomienda desde persistencia.
     *
     * @param id           identificador UUID
     * @param remitente    nombre del remitente
     * @param destinatario nombre del destinatario
     * @param peso         peso en kg
     * @param tarifa       tarifa ya calculada
     * @param estadoPago   estado de pago
     * @param estadoEnvio  estado del envío
     */
    public Encomienda(String id, String remitente, String destinatario, double peso,
                      BigDecimal tarifa, EstadoPago estadoPago, EstadoEnvio estadoEnvio) {
        this.id = id;
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.peso = peso;
        this.tarifa = tarifa;
        this.estadoPago = estadoPago;
        this.estadoEnvio = estadoEnvio;
    }

    // -------------------------------------------------------------------------
    // Métodos de dominio
    // -------------------------------------------------------------------------

    /**
     * Calcula la tarifa de envío en función del peso.
     * <p>
     * Fórmula: {@code max(5.00, 5.00 + peso × 2.50)}
     * </p>
     *
     * @return tarifa en soles, redondeada a 2 decimales, mínimo S/ 5.00
     */
    public BigDecimal calcularTarifa() {
        BigDecimal tarifaCalculada = TARIFA_BASE.add(
                COSTO_POR_KG.multiply(BigDecimal.valueOf(peso))
        ).setScale(2, RoundingMode.HALF_UP);

        return tarifaCalculada.max(TARIFA_BASE);
    }

    /**
     * Realiza la transición al nuevo estado indicado, validando que sea permitida
     * según el ciclo de vida definido en {@link #TRANSICIONES_VALIDAS}.
     *
     * @param nuevoEstado estado destino deseado
     * @throws ExcepcionTransicionInvalida si la transición no está permitida
     */
    public void transicionarEstado(EstadoEnvio nuevoEstado) {
        Set<EstadoEnvio> estadosPermitidos = TRANSICIONES_VALIDAS.getOrDefault(
                this.estadoEnvio, EnumSet.noneOf(EstadoEnvio.class));

        if (!estadosPermitidos.contains(nuevoEstado)) {
            throw new ExcepcionTransicionInvalida(
                    String.format("Transición no permitida: %s → %s. Estados permitidos desde %s: %s",
                            this.estadoEnvio, nuevoEstado, this.estadoEnvio, estadosPermitidos));
        }
        this.estadoEnvio = nuevoEstado;
    }

    /**
     * Bloquea la encomienda por pago pendiente contra entrega.
     * <p>
     * Solo aplica si:
     * <ul>
     *   <li>{@code estadoPago == PENDIENTE}</li>
     *   <li>{@code estadoEnvio == EN_ALMACEN_DESTINO}</li>
     * </ul>
     * Si ambas condiciones se cumplen, transiciona automáticamente a {@code BLOQUEADA}.
     * </p>
     */
    public void bloquearPorPagoContraEntrega() {
        if (this.estadoPago == EstadoPago.PENDIENTE
                && this.estadoEnvio == EstadoEnvio.EN_ALMACEN_DESTINO) {
            transicionarEstado(EstadoEnvio.BLOQUEADA);
        }
    }

    // -------------------------------------------------------------------------
    // Getters y Setters
    // -------------------------------------------------------------------------

    /** @return identificador UUID de la encomienda */
    public String getId() {
        return id;
    }

    /** @return nombre del remitente */
    public String getRemitente() {
        return remitente;
    }

    /** @param remitente nuevo nombre del remitente */
    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    /** @return nombre del destinatario */
    public String getDestinatario() {
        return destinatario;
    }

    /** @param destinatario nuevo nombre del destinatario */
    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    /** @return peso en kilogramos */
    public double getPeso() {
        return peso;
    }

    /** @param peso nuevo peso en kg */
    public void setPeso(double peso) {
        this.peso = peso;
    }

    /** @return tarifa calculada en soles */
    public BigDecimal getTarifa() {
        return tarifa;
    }

    /** @param tarifa nueva tarifa (usada al recalcular o cargar desde persistencia) */
    public void setTarifa(BigDecimal tarifa) {
        this.tarifa = tarifa;
    }

    /** @return estado actual del pago */
    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    /** @param estadoPago nuevo estado de pago */
    public void setEstadoPago(EstadoPago estadoPago) {
        this.estadoPago = estadoPago;
    }

    /** @return estado actual del envío */
    public EstadoEnvio getEstadoEnvio() {
        return estadoEnvio;
    }

    /** @param estadoEnvio nuevo estado (usar {@link #transicionarEstado} cuando sea posible) */
    public void setEstadoEnvio(EstadoEnvio estadoEnvio) {
        this.estadoEnvio = estadoEnvio;
    }

    @Override
    public String toString() {
        return "Encomienda{id='" + id + "', remitente='" + remitente
                + "', destinatario='" + destinatario + "', peso=" + peso
                + "kg, tarifa=S/." + tarifa + ", estadoPago=" + estadoPago
                + ", estadoEnvio=" + estadoEnvio + '}';
    }
}
