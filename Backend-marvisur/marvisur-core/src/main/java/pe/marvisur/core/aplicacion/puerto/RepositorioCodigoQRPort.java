package pe.marvisur.core.aplicacion.puerto;

import pe.marvisur.core.dominio.modelo.CodigoQR;

import java.util.Optional;

/**
 * Puerto de repositorio para la entidad {@link CodigoQR}.
 *
 * <p>Los adaptadores que implementen este puerto deben garantizar que:
 * <ul>
 *   <li>Una encomienda tiene como máximo un par de códigos QR (relación 1..1).</li>
 *   <li>Los hashes SHA-256 se almacenan tal cual (cadena hexadecimal de 64 caracteres).</li>
 * </ul>
 * </p>
 */
public interface RepositorioCodigoQRPort {

    /**
     * Persiste un nuevo par de códigos QR.
     *
     * @param codigoQR par de QR a guardar
     */
    void guardar(CodigoQR codigoQR);

    /**
     * Busca el par de códigos QR asociado a una encomienda.
     *
     * @param encomiendaId UUID de la encomienda
     * @return {@link Optional} con el CodigoQR si existe, vacío si no
     */
    Optional<CodigoQR> buscarPorEncomiendaId(String encomiendaId);

    /**
     * Actualiza el estado del par de códigos QR (p.ej. tras invalidar QR2).
     *
     * @param codigoQR par de QR con los datos actualizados
     */
    void actualizar(CodigoQR codigoQR);
}
