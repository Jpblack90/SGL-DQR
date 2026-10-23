package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Validador local para las reglas de negocio de ventanilla antes de enviar la petición a la API.
 */
public final class ValidadorVentanilla {

    private ValidadorVentanilla() {
    }

    public record ResultadoValidacion(boolean esValido, List<String> errores) {
        public String mensajeAgrupado() {
            return String.join("\n• ", errores);
        }
    }

    public static ResultadoValidacion validar(RegistrarEncomiendaComando comando) {
        List<String> errores = new ArrayList<>();

        if (comando == null) {
            errores.add("Los datos de la encomienda son requeridos.");
            return new ResultadoValidacion(false, errores);
        }

        // Remitente
        validarCliente(comando.remitente(), "Remitente", errores);

        // Destinatario
        validarCliente(comando.destinatario(), "Destinatario", errores);

        // Sucursal destino
        if (comando.idSucursalDestino() == null || comando.idSucursalDestino().isBlank()) {
            errores.add("Debe seleccionar la agencia de destino.");
        }

        // Peso
        if (comando.pesoKg() == null) {
            errores.add("El peso en kilogramos es obligatorio.");
        } else if (comando.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            errores.add("El peso debe ser mayor a 0 kg.");
        }

        // Volumen
        if (comando.volumenM3() != null && comando.volumenM3().compareTo(BigDecimal.ZERO) < 0) {
            errores.add("El volumen no puede ser negativo.");
        }

        // Modalidad de pago
        if (comando.modalidadPago() == null || comando.modalidadPago().isBlank()) {
            errores.add("Debe seleccionar una modalidad de pago (CONTADO o CONTRA_ENTREGA).");
        } else {
            String mod = comando.modalidadPago().trim().toUpperCase();
            if (!mod.equals("CONTADO") && !mod.equals("CONTRA_ENTREGA")) {
                errores.add("Modalidad de pago inválida. Debe ser CONTADO o CONTRA_ENTREGA.");
            }
        }

        return new ResultadoValidacion(errores.isEmpty(), errores);
    }

    public static void validarCliente(ClienteDto cliente, String rol, List<String> errores) {
        if (cliente == null) {
            errores.add("Los datos del " + rol.toLowerCase() + " son obligatorios.");
            return;
        }

        if (cliente.nombresRazonSocial() == null || cliente.nombresRazonSocial().isBlank()) {
            errores.add("El nombre o razón social del " + rol.toLowerCase() + " es obligatorio.");
        }

        if (cliente.tipoDocumento() == null || cliente.tipoDocumento().isBlank()) {
            errores.add("El tipo de documento del " + rol.toLowerCase() + " es obligatorio.");
        } else {
            String tipo = cliente.tipoDocumento().trim().toUpperCase();
            String num = cliente.numeroDocumento() != null ? cliente.numeroDocumento().trim() : "";

            if (tipo.equals("DNI")) {
                if (!num.matches("^\\d{8}$")) {
                    errores.add("El DNI del " + rol.toLowerCase() + " debe tener exactamente 8 dígitos numéricos.");
                }
            } else if (tipo.equals("RUC")) {
                if (!num.matches("^\\d{11}$")) {
                    errores.add("El RUC del " + rol.toLowerCase() + " debe tener exactamente 11 dígitos numéricos.");
                }
            } else if (tipo.equals("CE")) {
                if (num.isBlank() || num.length() < 6 || num.length() > 12) {
                    errores.add("El Carnet de Extranjería del " + rol.toLowerCase() + " debe tener entre 6 y 12 caracteres.");
                }
            } else {
                if (num.isBlank()) {
                    errores.add("El número de documento del " + rol.toLowerCase() + " es obligatorio.");
                }
            }
        }
    }
}
