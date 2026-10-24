package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controla la concurrencia y el ciclo de vida de las consultas periódicas del Dashboard (RF-02).
 * <p>
 * Reglas de negocio y concurrencia:
 * 1. Solo se ejecutan peticiones mientras la pestaña esté activa y visible ({@code activo == true}).
 * 2. Nunca se permiten dos peticiones simultáneas en curso (omite ejecuciones concurrentes).
 * 3. Al desactivarse (cambio de pestaña, cierre de sesión o salida), se detiene y descarta peticiones posteriores.
 * </p>
 */
public class PoliticaActualizacionDashboard {

    public static final int INTERVALO_REFRESCO_SEGUNDOS = 30;

    private final AtomicBoolean activo;
    private final AtomicBoolean cargando;

    public PoliticaActualizacionDashboard() {
        this(false);
    }

    public PoliticaActualizacionDashboard(boolean activoInicial) {
        this.activo = new AtomicBoolean(activoInicial);
        this.cargando = new AtomicBoolean(false);
    }

    public void activar() {
        this.activo.set(true);
    }

    public void desactivar() {
        this.activo.set(false);
        this.cargando.set(false);
    }

    public boolean isActivo() {
        return activo.get();
    }

    public boolean isCargando() {
        return cargando.get();
    }

    /**
     * Evalúa si una nueva actualización puede y debe iniciar.
     * Si la pestaña está activa y no hay una carga en curso, adquiere el bloqueo atómico
     * de ejecución y retorna {@code true}. Si ya hay una en curso o está inactivo, retorna {@code false}.
     */
    public boolean intentarIniciarCarga() {
        if (!activo.get()) {
            return false;
        }
        return cargando.compareAndSet(false, true);
    }

    /**
     * Libera el bloqueo de carga al finalizar la petición (con éxito o error).
     */
    public void finalizarCarga() {
        this.cargando.set(false);
    }
}
