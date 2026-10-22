package pe.marvisur.sgldqr.api.persistencia.mapeador;

import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.TokenQr2EstatusJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ValidacionCruzadaLogJpaEntity;
import pe.marvisur.sgldqr.dominio.modelo.EstadoTokenQR2;
import pe.marvisur.sgldqr.dominio.modelo.MotivoInactivacion;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.ResultadoValidacion;
import pe.marvisur.sgldqr.dominio.modelo.TokenQR2Estatus;
import pe.marvisur.sgldqr.dominio.modelo.ValidacionCruzadaLog;

/**
 * Mapeador manual para agregados y entidades criptográficas Doble QR.
 */
public final class ParDobleQrMapper {

    private ParDobleQrMapper() {
    }

    public static ParDobleQR aDominio(ParDobleQrJpaEntity entidad, TokenQr2EstatusJpaEntity tokenEntidad) {
        if (entidad == null) return null;

        TokenQR2Estatus tokenDominio = aDominio(tokenEntidad);
        return new ParDobleQR(
                entidad.getIdParejaQr(),
                entidad.getIdEncomienda(),
                entidad.getUuidQr1Operativo(),
                entidad.getHashQr2LlaveDigital(),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaGeneracion()),
                tokenDominio
        );
    }

    public static ParDobleQrJpaEntity aEntidad(ParDobleQR dominio) {
        if (dominio == null) return null;

        return new ParDobleQrJpaEntity(
                dominio.getIdParejaQr(),
                dominio.getIdEncomienda(),
                dominio.getUuidQr1Operativo(),
                dominio.getHashQr2LlaveDigital(),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaGeneracion())
        );
    }

    public static TokenQR2Estatus aDominio(TokenQr2EstatusJpaEntity entidad) {
        if (entidad == null) return null;

        MotivoInactivacion motivo = (entidad.getMotivoInactivacion() != null)
                ? MotivoInactivacion.valueOf(entidad.getMotivoInactivacion())
                : null;

        return new TokenQR2Estatus(
                entidad.getIdTokenStatus(),
                entidad.getIdParejaQr(),
                EstadoTokenQR2.valueOf(entidad.getEstadoToken()),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaInvalidacion()),
                motivo
        );
    }

    public static TokenQr2EstatusJpaEntity aEntidad(TokenQR2Estatus dominio) {
        if (dominio == null) return null;

        String motivoStr = (dominio.getMotivoInactivacion() != null)
                ? dominio.getMotivoInactivacion().name()
                : null;

        return new TokenQr2EstatusJpaEntity(
                dominio.getIdTokenStatus(),
                dominio.getIdParejaQr(),
                dominio.getEstadoToken().name(),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaInvalidacion()),
                motivoStr
        );
    }

    public static ValidacionCruzadaLog aDominio(ValidacionCruzadaLogJpaEntity entidad) {
        if (entidad == null) return null;

        return new ValidacionCruzadaLog(
                entidad.getIdValidacion(),
                entidad.getIdParejaQr(),
                entidad.getIdEmpleadoValida(),
                ResultadoValidacion.valueOf(entidad.getResultado()),
                entidad.getLatenciaProcesamientoMs(),
                FechaMapperHelper.aLocalDateTime(entidad.getFechaValidacion())
        );
    }

    public static ValidacionCruzadaLogJpaEntity aEntidad(ValidacionCruzadaLog dominio) {
        if (dominio == null) return null;

        return new ValidacionCruzadaLogJpaEntity(
                dominio.getIdValidacion(),
                dominio.getIdParejaQr(),
                dominio.getIdEmpleadoValida(),
                dominio.getResultado().name(),
                dominio.getLatenciaProcesamientoMs(),
                FechaMapperHelper.aOffsetDateTime(dominio.getFechaValidacion())
        );
    }
}
