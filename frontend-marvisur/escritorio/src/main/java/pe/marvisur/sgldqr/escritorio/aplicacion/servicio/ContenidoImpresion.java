package pe.marvisur.sgldqr.escritorio.aplicacion.servicio;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.SucursalDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;

/**
 * Arma el contenido de los documentos impresos (ticket del cliente y etiqueta del bulto)
 * y de los enlaces para enviar la Billetera Digital al destinatario.
 * <p>
 * Clase sin estado ni dependencias de JavaFX, para poder probarla de forma aislada.
 * Regla de seguridad: aquí nunca se arma un QR con el token QR2; el QR del ticket es el del
 * rastreo público (solo el comprobante) y el de la etiqueta es el QR1 operativo.
 * </p>
 */
public final class ContenidoImpresion {

    private static final String LINEA_DOBLE = "================================";
    private static final String LINEA_SIMPLE = "--------------------------------";

    private ContenidoImpresion() {
    }

    /**
     * Devuelve el nombre de la agencia para un código de sucursal; si no está en el catálogo,
     * devuelve el propio código.
     */
    public static String nombreAgencia(String idSucursal, Collection<SucursalDto> sucursales) {
        if (idSucursal == null || idSucursal.isBlank()) {
            return "-";
        }
        if (sucursales != null) {
            for (SucursalDto s : sucursales) {
                if (s != null && idSucursal.equalsIgnoreCase(s.id())
                        && s.nombreAgencia() != null && !s.nombreAgencia().isBlank()) {
                    return s.nombreAgencia().trim();
                }
            }
        }
        return idSucursal;
    }

    /**
     * Obtiene la dirección base del portal web a partir del enlace de la Billetera
     * ({@code <base>/billetera#<token>}); si no se puede, usa la dirección de respaldo.
     * Nunca devuelve el token.
     */
    public static String basePortal(String enlaceBilletera, String respaldo) {
        if (enlaceBilletera != null) {
            int i = enlaceBilletera.indexOf("/billetera");
            if (i > 0) {
                return quitarBarraFinal(enlaceBilletera.substring(0, i));
            }
        }
        if (respaldo == null || respaldo.isBlank()) {
            return null;
        }
        return quitarBarraFinal(respaldo.trim());
    }

    /** Dirección pública de rastreo con el comprobante ya escrito. */
    public static String urlRastreo(String basePortal, String codigoComprobante) {
        if (basePortal == null || basePortal.isBlank() || codigoComprobante == null || codigoComprobante.isBlank()) {
            return null;
        }
        return quitarBarraFinal(basePortal.trim()) + "/rastreo?codigo=" + codificar(codigoComprobante.trim());
    }

    /** Texto del ticket que se entrega al cliente en ventanilla. */
    public static String textoTicketCliente(String comprobante, String fecha, String agenciaOrigen,
                                            String agenciaDestino, String remitente, String destinatario,
                                            String pesoKg, String estadoPago, String total, boolean conQrRastreo) {
        StringBuilder sb = new StringBuilder()
                .append(LINEA_DOBLE).append('\n')
                .append("      EXPRESO MARVISUR S.A.").append('\n')
                .append("   COMPROBANTE DE ENCOMIENDA").append('\n')
                .append(LINEA_DOBLE).append('\n')
                .append("COMPROBANTE: ").append(valor(comprobante)).append('\n')
                .append("FECHA:       ").append(valor(fecha)).append('\n')
                .append("ORIGEN:      ").append(valor(agenciaOrigen)).append('\n')
                .append("DESTINO:     ").append(valor(agenciaDestino)).append('\n')
                .append(LINEA_SIMPLE).append('\n')
                .append("REMITENTE:   ").append(valor(remitente)).append('\n')
                .append("DESTINATARIO:").append(valor(destinatario)).append('\n')
                .append(LINEA_SIMPLE).append('\n')
                .append("PESO:        ").append(valor(pesoKg)).append(" Kg").append('\n')
                .append("ESTADO PAGO: ").append(valor(estadoPago)).append('\n')
                .append("TOTAL:       S/. ").append(valor(total)).append('\n')
                .append(LINEA_DOBLE).append('\n');
        if (conQrRastreo) {
            sb.append(" Escanee el QR para rastrear").append('\n')
              .append("        su envio en linea").append('\n');
        } else {
            sb.append(" Rastree su envio con el numero").append('\n')
              .append("  de comprobante en el portal").append('\n');
        }
        sb.append(" Su llave de retiro (QR2) llega").append('\n')
          .append("   a la Billetera Digital").append('\n')
          .append(LINEA_DOBLE).append('\n');
        return sb.toString();
    }

    /** Texto de la etiqueta que se pega en el bulto, encima del QR1. */
    public static String textoEtiquetaBulto(String comprobante, String agenciaOrigen, String agenciaDestino,
                                            String destinatario, String pesoKg, String fecha, boolean reimpresion) {
        return LINEA_DOBLE + '\n'
                + "      EXPRESO MARVISUR S.A.\n"
                + (reimpresion ? "  ETIQUETA DE BULTO (REIMPRESA)\n" : "       ETIQUETA DE BULTO\n")
                + LINEA_DOBLE + '\n'
                + "DESTINO:     " + valor(agenciaDestino).toUpperCase(Locale.ROOT) + '\n'
                + "ORIGEN:      " + valor(agenciaOrigen) + '\n'
                + LINEA_SIMPLE + '\n'
                + "COMPROBANTE: " + valor(comprobante) + '\n'
                + "DESTINATARIO:" + valor(destinatario) + '\n'
                + (pesoKg != null && !pesoKg.isBlank() ? "PESO:        " + pesoKg + " Kg\n" : "")
                + "FECHA:       " + valor(fecha) + '\n'
                + LINEA_DOBLE + '\n'
                + "  QR1 OPERATIVO - NO DESPEGAR\n";
    }

    /** Mensaje que recibe el destinatario con el enlace de su Billetera Digital. */
    public static String mensajeBilletera(String nombreDestinatario, String comprobante, String enlaceBilletera) {
        String saludo = nombreDestinatario != null && !nombreDestinatario.isBlank()
                ? "Hola " + nombreDestinatario.trim() + ", " : "Hola, ";
        return saludo + "tiene una encomienda de Expreso Marvisur (comprobante " + valor(comprobante) + "). "
                + "Este es su enlace personal para retirarla: " + valor(enlaceBilletera) + " "
                + "Presente el QR de ese enlace en la agencia de destino. No lo comparta con nadie.";
    }

    /**
     * Enlace de WhatsApp con el mensaje listo. Un celular peruano de 9 dígitos recibe el código 51;
     * sin teléfono, WhatsApp pide elegir el contacto.
     */
    public static String enlaceWhatsApp(String telefono, String mensaje) {
        String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
        if (digitos.length() == 9 && digitos.startsWith("9")) {
            digitos = "51" + digitos;
        }
        return "https://wa.me/" + digitos + "?text=" + codificar(mensaje == null ? "" : mensaje);
    }

    /** Enlace {@code mailto:} con asunto y cuerpo listos para el programa de correo. */
    public static String enlaceCorreo(String correo, String asunto, String cuerpo) {
        String destino = correo == null ? "" : correo.trim();
        return "mailto:" + destino + "?subject=" + codificar(asunto == null ? "" : asunto)
                + "&body=" + codificar(cuerpo == null ? "" : cuerpo);
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String quitarBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }
}
