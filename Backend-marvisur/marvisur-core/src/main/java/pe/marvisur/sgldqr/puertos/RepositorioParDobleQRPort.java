package pe.marvisur.sgldqr.puertos;

import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.TokenQR2Estatus;
import pe.marvisur.sgldqr.dominio.modelo.ValidacionCruzadaLog;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para el par criptográfico Doble QR, su ciclo de vida y auditoría de validaciones.
 */
public interface RepositorioParDobleQRPort {

    void guardar(ParDobleQR parDobleQR);

    Optional<ParDobleQR> buscarPorId(String idParejaQr);

    Optional<ParDobleQR> buscarPorEncomiendaId(String idEncomienda);

    void guardarTokenEstatus(TokenQR2Estatus tokenEstatus);

    void actualizarTokenEstatus(TokenQR2Estatus tokenEstatus);

    Optional<TokenQR2Estatus> buscarTokenEstatusPorParejaId(String idParejaQr);

    void guardarValidacionLog(ValidacionCruzadaLog validacionCruzadaLog);

    List<ValidacionCruzadaLog> listarValidacionesPorParejaId(String idParejaQr);
}
