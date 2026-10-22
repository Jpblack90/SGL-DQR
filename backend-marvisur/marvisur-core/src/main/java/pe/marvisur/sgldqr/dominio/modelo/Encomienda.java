package pe.marvisur.sgldqr.dominio.modelo;

import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionEntregaBloqueada;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionTransicionInvalida;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad principal de dominio (Agregado Raíz) que representa un envío de encomienda en Expreso Marvisur.
 *
 * <p><strong>Reglas de negocio del dominio:</strong></p>
 * <ul>
 *   <li>Una encomienda NO puede pasar a {@link EstadoLogistico#ENTREGADO} si su {@code estadoPago}
 *       es {@link EstadoPago#PENDIENTE} (modalidad Contra Entrega). En tal caso, se bloquea la entrega
 *       lanzando {@link ExcepcionEntregaBloqueada}.</li>
 *   <li>El ciclo de transiciones logísticas sigue el flujo operativo oficial:
 *       {@code REGISTRADO -> EN_ALMACEN_ORIGEN -> EN_RUTA -> EN_ALMACEN_DESTINO -> ENTREGADO}.</li>
 *   <li>El bloqueo no se modela como un estado logístico separado en BD (ya que no existe en el
 *       Diccionario de Datos), sino como una regla invariante de dominio en {@link #transicionarEstado(EstadoLogistico)}.</li>
 * </ul>
 */
public class Encomienda {

    private static final Map<EstadoLogistico, Set<EstadoLogistico>> TRANSICIONES_VALIDAS = new EnumMap<>(EstadoLogistico.class);

    static {
        TRANSICIONES_VALIDAS.put(EstadoLogistico.REGISTRADO, EnumSet.of(EstadoLogistico.EN_ALMACEN_ORIGEN, EstadoLogistico.EN_RUTA));
        TRANSICIONES_VALIDAS.put(EstadoLogistico.EN_ALMACEN_ORIGEN, EnumSet.of(EstadoLogistico.EN_RUTA));
        TRANSICIONES_VALIDAS.put(EstadoLogistico.EN_RUTA, EnumSet.of(EstadoLogistico.EN_ALMACEN_DESTINO));
        TRANSICIONES_VALIDAS.put(EstadoLogistico.EN_ALMACEN_DESTINO, EnumSet.of(EstadoLogistico.ENTREGADO));
        TRANSICIONES_VALIDAS.put(EstadoLogistico.ENTREGADO, EnumSet.noneOf(EstadoLogistico.class));
    }

    private final String idEncomienda;
    private final String codigoComprobante;
    private final String idRemitente;
    private final String idDestinatario;
    private final String idSucursalOrigen;
    private final String idSucursalDestino;
    private BigDecimal pesoKg;
    private BigDecimal volumenM3;
    private String descripcionContenido;
    private BigDecimal montoTotal;
    private EstadoPago estadoPago;
    private EstadoLogistico estadoLogistico;
    private final LocalDateTime fechaRegistro;

    public Encomienda(String idEncomienda, String codigoComprobante, String idRemitente,
                      String idDestinatario, String idSucursalOrigen, String idSucursalDestino,
                      BigDecimal pesoKg, BigDecimal volumenM3, String descripcionContenido,
                      BigDecimal montoTotal, EstadoPago estadoPago, EstadoLogistico estadoLogistico,
                      LocalDateTime fechaRegistro) {
        this.idEncomienda = Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        this.codigoComprobante = Objects.requireNonNull(codigoComprobante, "El codigoComprobante no puede ser nulo");
        this.idRemitente = Objects.requireNonNull(idRemitente, "El idRemitente no puede ser nulo");
        this.idDestinatario = Objects.requireNonNull(idDestinatario, "El idDestinatario no puede ser nulo");
        this.idSucursalOrigen = Objects.requireNonNull(idSucursalOrigen, "El idSucursalOrigen no puede ser nulo");
        this.idSucursalDestino = Objects.requireNonNull(idSucursalDestino, "El idSucursalDestino no puede ser nulo");
        this.pesoKg = Objects.requireNonNull(pesoKg, "El pesoKg no puede ser nulo");
        this.volumenM3 = volumenM3;
        this.descripcionContenido = descripcionContenido;
        this.montoTotal = Objects.requireNonNull(montoTotal, "El montoTotal no puede ser nulo");
        this.estadoPago = Objects.requireNonNull(estadoPago, "El estadoPago no puede ser nulo");
        this.estadoLogistico = Objects.requireNonNull(estadoLogistico, "El estadoLogistico no puede ser nulo");
        this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "La fechaRegistro no puede ser nula");
    }

    public static Encomienda crearNueva(String codigoComprobante, String idRemitente,
                                        String idDestinatario, String idSucursalOrigen, String idSucursalDestino,
                                        BigDecimal pesoKg, BigDecimal volumenM3, String descripcionContenido,
                                        BigDecimal montoTotal, EstadoPago estadoPago, LocalDateTime fechaRegistro) {
        return new Encomienda(
                UUID.randomUUID().toString(),
                codigoComprobante,
                idRemitente,
                idDestinatario,
                idSucursalOrigen,
                idSucursalDestino,
                pesoKg,
                volumenM3,
                descripcionContenido,
                montoTotal,
                estadoPago,
                EstadoLogistico.REGISTRADO,
                fechaRegistro
        );
    }

    /**
     * Transiciona el estado logístico de la encomienda validando el flujo oficial
     * y bloqueando la entrega si el pago está PENDIENTE.
     *
     * @param nuevoEstado estado destino
     * @throws ExcepcionEntregaBloqueada si intenta pasar a {@link EstadoLogistico#ENTREGADO} con pago PENDIENTE
     * @throws ExcepcionTransicionInvalida si la transición no está permitida en el flujo
     */
    public void transicionarEstado(EstadoLogistico nuevoEstado) {
        Objects.requireNonNull(nuevoEstado, "El nuevoEstado no puede ser nulo");

        Set<EstadoLogistico> permitidos = TRANSICIONES_VALIDAS.getOrDefault(this.estadoLogistico, EnumSet.noneOf(EstadoLogistico.class));
        if (!permitidos.contains(nuevoEstado)) {
            throw new ExcepcionTransicionInvalida(this.estadoLogistico, nuevoEstado);
        }

        // Regla crítica: No puede pasar a ENTREGADO si el pago está PENDIENTE
        if (nuevoEstado == EstadoLogistico.ENTREGADO && this.estadoPago == EstadoPago.PENDIENTE) {
            throw new ExcepcionEntregaBloqueada(this.idEncomienda);
        }

        this.estadoLogistico = nuevoEstado;
    }

    /**
     * Registra el cobro de la encomienda (ej. cobro en ventanilla de destino en modalidad Contra Entrega).
     */
    public void registrarPago() {
        this.estadoPago = EstadoPago.PAGADO;
    }

    /**
     * Informa si la encomienda se encuentra bloqueada para entrega física en almacén de destino.
     */
    public boolean estaBloqueadaParaEntrega() {
        return this.estadoLogistico == EstadoLogistico.EN_ALMACEN_DESTINO
                && this.estadoPago == EstadoPago.PENDIENTE;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public String getCodigoComprobante() {
        return codigoComprobante;
    }

    public String getIdRemitente() {
        return idRemitente;
    }

    public String getIdDestinatario() {
        return idDestinatario;
    }

    public String getIdSucursalOrigen() {
        return idSucursalOrigen;
    }

    public String getIdSucursalDestino() {
        return idSucursalDestino;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public BigDecimal getVolumenM3() {
        return volumenM3;
    }

    public void setVolumenM3(BigDecimal volumenM3) {
        this.volumenM3 = volumenM3;
    }

    public String getDescripcionContenido() {
        return descripcionContenido;
    }

    public void setDescripcionContenido(String descripcionContenido) {
        this.descripcionContenido = descripcionContenido;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago;
    }

    public EstadoLogistico getEstadoLogistico() {
        return estadoLogistico;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Encomienda that = (Encomienda) o;
        return Objects.equals(idEncomienda, that.idEncomienda);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEncomienda);
    }
}
