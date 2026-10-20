package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;

import java.util.Optional;

/**
 * Puerto de repositorio para el par criptográfico Doble QR.
 */
public interface RepositorioParDobleQRPort {

    void guardar(ParDobleQR parDobleQR);

    Optional<ParDobleQR> buscarPorId(String idParejaQr);

    Optional<ParDobleQR> buscarPorEncomiendaId(String idEncomienda);
}
