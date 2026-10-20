package pe.marvisur.sgldqr.dominio.excepcion;

/**
 * Se lanza cuando se intenta entregar una encomienda cuyo pago está PENDIENTE (Contra Entrega).
 */
public class ExcepcionEntregaBloqueada extends ExcepcionDominio {

    public ExcepcionEntregaBloqueada(String idEncomienda) {
        super(String.format("La entrega de la encomienda '%s' está bloqueada: el pago se encuentra PENDIENTE (Contra Entrega). Debe registrarse el cobro antes de proceder.", idEncomienda));
    }
}
