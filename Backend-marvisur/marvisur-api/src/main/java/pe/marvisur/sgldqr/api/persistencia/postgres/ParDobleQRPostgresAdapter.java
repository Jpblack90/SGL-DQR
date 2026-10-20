package pe.marvisur.sgldqr.api.persistencia.postgres;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.TokenQr2EstatusJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ValidacionCruzadaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.ParDobleQrMapper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ParDobleQrSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.TokenQr2EstatusSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ValidacionCruzadaLogSpringDataRepository;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.TokenQR2Estatus;
import pe.marvisur.sgldqr.dominio.modelo.ValidacionCruzadaLog;
import pe.marvisur.sgldqr.puertos.RepositorioParDobleQRPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador de persistencia PostgreSQL para el par criptográfico Doble QR y auditoría de validaciones.
 */
@Component
public class ParDobleQRPostgresAdapter implements RepositorioParDobleQRPort {

    private final ParDobleQrSpringDataRepository parRepository;
    private final TokenQr2EstatusSpringDataRepository tokenRepository;
    private final ValidacionCruzadaLogSpringDataRepository validacionLogRepository;

    public ParDobleQRPostgresAdapter(ParDobleQrSpringDataRepository parRepository,
                                     TokenQr2EstatusSpringDataRepository tokenRepository,
                                     ValidacionCruzadaLogSpringDataRepository validacionLogRepository) {
        this.parRepository = Objects.requireNonNull(parRepository, "El parRepository no puede ser nulo");
        this.tokenRepository = Objects.requireNonNull(tokenRepository, "El tokenRepository no puede ser nulo");
        this.validacionLogRepository = Objects.requireNonNull(validacionLogRepository, "El validacionLogRepository no puede ser nulo");
    }

    @Override
    @Transactional
    public void guardar(ParDobleQR parDobleQR) {
        Objects.requireNonNull(parDobleQR, "El parDobleQR no puede ser nulo");
        ParDobleQrJpaEntity parEntidad = ParDobleQrMapper.aEntidad(parDobleQR);
        parRepository.save(parEntidad);

        if (parDobleQR.getTokenEstatus() != null) {
            TokenQr2EstatusJpaEntity tokenEntidad = ParDobleQrMapper.aEntidad(parDobleQR.getTokenEstatus());
            tokenRepository.save(tokenEntidad);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParDobleQR> buscarPorId(String idParejaQr) {
        Objects.requireNonNull(idParejaQr, "El idParejaQr no puede ser nulo");
        return parRepository.findById(idParejaQr)
                .map(parEntidad -> {
                    TokenQr2EstatusJpaEntity tokenEntidad = tokenRepository.findByIdParejaQr(idParejaQr).orElse(null);
                    return ParDobleQrMapper.aDominio(parEntidad, tokenEntidad);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParDobleQR> buscarPorEncomiendaId(String idEncomienda) {
        Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        return parRepository.findByIdEncomienda(idEncomienda)
                .map(parEntidad -> {
                    TokenQr2EstatusJpaEntity tokenEntidad = tokenRepository.findByIdParejaQr(parEntidad.getIdParejaQr()).orElse(null);
                    return ParDobleQrMapper.aDominio(parEntidad, tokenEntidad);
                });
    }

    @Override
    @Transactional
    public void guardarTokenEstatus(TokenQR2Estatus tokenEstatus) {
        Objects.requireNonNull(tokenEstatus, "El tokenEstatus no puede ser nulo");
        TokenQr2EstatusJpaEntity entidad = ParDobleQrMapper.aEntidad(tokenEstatus);
        tokenRepository.save(entidad);
    }

    @Override
    @Transactional
    public void actualizarTokenEstatus(TokenQR2Estatus tokenEstatus) {
        Objects.requireNonNull(tokenEstatus, "El tokenEstatus no puede ser nulo");
        TokenQr2EstatusJpaEntity entidad = ParDobleQrMapper.aEntidad(tokenEstatus);
        tokenRepository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TokenQR2Estatus> buscarTokenEstatusPorParejaId(String idParejaQr) {
        Objects.requireNonNull(idParejaQr, "El idParejaQr no puede ser nulo");
        return tokenRepository.findByIdParejaQr(idParejaQr)
                .map(ParDobleQrMapper::aDominio);
    }

    @Override
    @Transactional
    public void guardarValidacionLog(ValidacionCruzadaLog validacionCruzadaLog) {
        Objects.requireNonNull(validacionCruzadaLog, "El validacionCruzadaLog no puede ser nulo");
        ValidacionCruzadaLogJpaEntity entidad = ParDobleQrMapper.aEntidad(validacionCruzadaLog);
        validacionLogRepository.save(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidacionCruzadaLog> listarValidacionesPorParejaId(String idParejaQr) {
        Objects.requireNonNull(idParejaQr, "El idParejaQr no puede ser nulo");
        return validacionLogRepository.findByIdParejaQrOrderByFechaValidacionDesc(idParejaQr)
                .stream()
                .map(ParDobleQrMapper::aDominio)
                .toList();
    }
}
