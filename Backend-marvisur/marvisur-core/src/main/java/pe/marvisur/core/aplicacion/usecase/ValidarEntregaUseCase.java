package pe.marvisur.core.aplicacion.usecase;

import pe.marvisur.core.aplicacion.puerto.RepositorioCodigoQRPort;
import pe.marvisur.core.aplicacion.puerto.RepositorioEncomiendaPort;
import pe.marvisur.core.dominio.excepcion.ExcepcionEncomiendaNoEncontrada;
import pe.marvisur.core.dominio.modelo.CodigoQR;
import pe.marvisur.core.dominio.modelo.Encomienda;
import pe.marvisur.core.dominio.modelo.EstadoEnvio;
import pe.marvisur.core.dominio.modelo.EstadoPago;
import pe.marvisur.core.dominio.servicio.MotorQR;
import pe.marvisur.core.dominio.servicio.ResultadoValidacion;

/**
 * Caso de uso: Validar la entrega de una encomienda mediante Doble QR.
 *
 * <p><strong>Flujo de negocio:</strong></p>
 * <ol>
 *   <li>Buscar la encomienda; si no existe, lanzar {@link ExcepcionEncomiendaNoEncontrada}.</li>
 *   <li>Buscar el par de códigos QR asociado.</li>
 *   <li>Validar cruzadamente QR1 + QR2 con el {@link MotorQR}.</li>
 *   <li>Si la validación no es {@link ResultadoValidacion#VALIDO}, retornar el resultado sin cambios.</li>
 *   <li>Si el pago está pendiente, retornar {@link ResultadoValidacion#PAGO_PENDIENTE} sin invalidar QR2.</li>
 *   <li>Invalidar el QR2 (operación irreversible).</li>
 *   <li>Transicionar la encomienda a estado {@link EstadoEnvio#ENTREGADA}.</li>
 *   <li>Persistir los cambios en ambos repositorios.</li>
 *   <li>Retornar {@link ResultadoValidacion#VALIDO}.</li>
 * </ol>
 */
public class ValidarEntregaUseCase {

    // -------------------------------------------------------------------------
    // Puertos (dependencias inyectadas)
    // -------------------------------------------------------------------------

    private final RepositorioEncomiendaPort repositorioEncomienda;
    private final RepositorioCodigoQRPort repositorioCodigoQR;
    private final MotorQR motorQR;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Construye el caso de uso con sus dependencias inyectadas via constructor.
     *
     * @param repositorioEncomienda puerto de persistencia de encomiendas
     * @param repositorioCodigoQR   puerto de persistencia de códigos QR
     * @param motorQR               servicio de dominio para validación QR
     */
    public ValidarEntregaUseCase(RepositorioEncomiendaPort repositorioEncomienda,
                                 RepositorioCodigoQRPort repositorioCodigoQR,
                                 MotorQR motorQR) {
        this.repositorioEncomienda = repositorioEncomienda;
        this.repositorioCodigoQR = repositorioCodigoQR;
        this.motorQR = motorQR;
    }

    // -------------------------------------------------------------------------
    // Método principal del caso de uso
    // -------------------------------------------------------------------------

    /**
     * Valida la entrega de una encomienda cruzando QR1 y QR2.
     *
     * @param encomiendaId UUID de la encomienda a entregar
     * @param qr1Leido     contenido del QR1 escaneado (físico del ticket)
     * @param qr2Leido     contenido del QR2 escaneado (digital del destinatario)
     * @return resultado de la validación
     * @throws ExcepcionEncomiendaNoEncontrada si la encomienda no existe
     */
    public ResultadoValidacion validarEntrega(String encomiendaId, String qr1Leido, String qr2Leido) {

        // --- Paso 1: Buscar encomienda ---
        Encomienda encomienda = repositorioEncomienda.buscarPorId(encomiendaId)
                .orElseThrow(() -> new ExcepcionEncomiendaNoEncontrada(encomiendaId));

        // --- Paso 2: Buscar par de códigos QR ---
        CodigoQR codigoQR = repositorioCodigoQR.buscarPorEncomiendaId(encomiendaId)
                .orElseThrow(() -> new ExcepcionEncomiendaNoEncontrada(
                        "No se encontró el par QR para la encomienda: " + encomiendaId));

        // --- Paso 3: Validar cruzado QR1 + QR2 ---
        ResultadoValidacion resultado = motorQR.validarCruzado(qr1Leido, qr2Leido, codigoQR);

        // --- Paso 4: Si no es válido, retornar sin modificar nada ---
        if (resultado != ResultadoValidacion.VALIDO) {
            return resultado;
        }

        // --- Paso 5: Verificar si el pago está pendiente ---
        if (encomienda.getEstadoPago() == EstadoPago.PENDIENTE) {
            return ResultadoValidacion.PAGO_PENDIENTE;
        }

        // --- Paso 6: Invalidar QR2 de forma irreversible ---
        codigoQR.invalidarQR2();

        // --- Paso 7: Actualizar par QR en el repositorio ---
        repositorioCodigoQR.actualizar(codigoQR);

        // --- Paso 8: Transicionar encomienda a ENTREGADA ---
        encomienda.transicionarEstado(EstadoEnvio.ENTREGADA);

        // --- Paso 9: Actualizar encomienda en el repositorio ---
        repositorioEncomienda.actualizar(encomienda);

        return ResultadoValidacion.VALIDO;
    }
}
