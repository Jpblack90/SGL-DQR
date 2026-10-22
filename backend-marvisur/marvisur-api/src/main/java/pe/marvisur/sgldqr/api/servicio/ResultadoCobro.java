package pe.marvisur.sgldqr.api.servicio;

import pe.marvisur.sgldqr.api.dto.RespuestaCobroDto;

import java.util.Objects;

/**
 * Encapsula el resultado del registro de cobro en ventanilla para encomiendas Contra Entrega.
 */
public class ResultadoCobro {

    public enum Tipo {
        EXITOSO,
        YA_PAGADA,
        NO_ENCONTRADO
    }

    private final Tipo tipo;
    private final RespuestaCobroDto respuestaDto;
    private final String mensaje;

    private ResultadoCobro(Tipo tipo, RespuestaCobroDto respuestaDto, String mensaje) {
        this.tipo = Objects.requireNonNull(tipo, "El tipo no puede ser nulo");
        this.respuestaDto = respuestaDto;
        this.mensaje = mensaje;
    }

    public static ResultadoCobro exitoso(RespuestaCobroDto respuestaDto) {
        return new ResultadoCobro(Tipo.EXITOSO, respuestaDto, respuestaDto.mensaje());
    }

    public static ResultadoCobro yaPagada(String mensaje) {
        return new ResultadoCobro(Tipo.YA_PAGADA, null, mensaje);
    }

    public static ResultadoCobro noEncontrado(String mensaje) {
        return new ResultadoCobro(Tipo.NO_ENCONTRADO, null, mensaje);
    }

    public Tipo getTipo() {
        return tipo;
    }

    public RespuestaCobroDto getRespuestaDto() {
        return respuestaDto;
    }

    public String getMensaje() {
        return mensaje;
    }
}
