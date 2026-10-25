package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Lógica de presentación, validación desacoplada y formateo para Rutas y Flota (RF-05).
 * Diseñado para ser probado de forma headless sin inicializar JavaFX.
 */
public final class RutasFlotaViewModel {

    public static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA_HORA_CORTO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Pattern PATRON_PLACA = Pattern.compile("^[A-Z0-9-]{5,10}$");

    public static final List<String> FILTROS_ESTADO_VIAJE = List.of(
            "TODOS",
            "PROGRAMADO",
            "EN_RUTA",
            "FINALIZADO"
    );

    public static final List<String> FILTROS_ESTADO_ETIQUETAS = List.of(
            "Todos los estados",
            "Programado",
            "En ruta",
            "Finalizado"
    );

    private final ZoneId zonaPresentacion;

    public RutasFlotaViewModel(ZoneId zonaPresentacion) {
        this.zonaPresentacion = zonaPresentacion != null ? zonaPresentacion : ZoneId.systemDefault();
    }

    public static RutasFlotaViewModel porDefecto() {
        return new RutasFlotaViewModel(ZoneId.systemDefault());
    }

    public static RutasFlotaViewModel conZona(ZoneId zona) {
        return new RutasFlotaViewModel(zona);
    }

    public ZoneId getZonaPresentacion() {
        return zonaPresentacion;
    }

    public String formatearFechaHoraTextoInstancia(String isoString) {
        return formatearFechaHoraTexto(isoString, this.zonaPresentacion);
    }

    public String formatearFechaHoraCompletaInstancia(OffsetDateTime odt) {
        return formatearFechaHoraCompleta(odt, this.zonaPresentacion);
    }

    // =========================================================================
    // MAPEO Y FORMATEO DE ESTADOS
    // =========================================================================

    public static String mapearFiltroEstadoApi(String etiqueta) {
        if (etiqueta == null) return "TODOS";
        return switch (etiqueta.trim().toUpperCase()) {
            case "PROGRAMADO" -> "PROGRAMADO";
            case "EN RUTA", "EN_RUTA" -> "EN_RUTA";
            case "FINALIZADO" -> "FINALIZADO";
            default -> "TODOS";
        };
    }

    public static String etiquetaEstadoLogistico(String estado) {
        if (estado == null) return "--";
        return switch (estado.trim().toUpperCase()) {
            case "REGISTRADO" -> "Registrado";
            case "RECEPCIONADO_ORIGEN" -> "Recepción origen";
            case "EN_RUTA" -> "En ruta";
            case "EN_ALMACEN_DESTINO" -> "Almacén destino";
            case "ENTREGADO" -> "Entregado";
            default -> estado.trim();
        };
    }

    public static String etiquetaEstadoViaje(String estado) {
        if (estado == null) return "--";
        return switch (estado.trim().toUpperCase()) {
            case "PROGRAMADO" -> "Programado";
            case "EN_RUTA" -> "En ruta";
            case "FINALIZADO" -> "Finalizado";
            default -> estado.trim();
        };
    }

    // =========================================================================
    // FORMATEO PARA CONTROLES Y COMBOS
    // =========================================================================

    public static String formatearRutaCombo(RutaDto ruta) {
        if (ruta == null || ruta.id() == null) {
            return "Seleccione una ruta...";
        }
        String origen = (ruta.origen() != null && ruta.origen().nombreAgencia() != null)
                ? ruta.origen().nombreAgencia().trim() : "Origen";
        String destino = (ruta.destino() != null && ruta.destino().nombreAgencia() != null)
                ? ruta.destino().nombreAgencia().trim() : "Destino";
        String dist = (ruta.distanciaKm() != null) ? ruta.distanciaKm() + " km" : "";
        return dist.isEmpty() ? (origen + " ➔ " + destino) : (origen + " ➔ " + destino + " (" + dist + ")");
    }

    public static String formatearVehiculoCombo(VehiculoDto v) {
        if (v == null || v.idVehiculo() == null) {
            return "Seleccione un vehículo...";
        }
        String placa = v.placa() != null ? v.placa().trim() : "";
        String mod = v.modelo() != null ? v.modelo().trim() : "";
        String cap = v.capacidadCargaKg() != null ? v.capacidadCargaKg() + " kg" : "";
        return placa + " - " + mod + " (" + cap + ")";
    }

    public static String formatearChoferCombo(EmpleadoDto e) {
        if (e == null || e.idEmpleado() == null) {
            return "Seleccione un chofer...";
        }
        String nombre = e.nombreCompleto() != null ? e.nombreCompleto().trim() : "";
        String dni = (e.dni() != null && !e.dni().isBlank()) ? " (" + e.dni().trim() + ")" : "";
        return nombre + dni;
    }

    // =========================================================================
    // FECHAS Y HORAS (America/Lima)
    // =========================================================================

    public static OffsetDateTime construirOffsetDateTimeLima(LocalDate fecha, int hora, int minuto) {
        if (fecha == null) return null;
        LocalTime time = LocalTime.of(
                Math.max(0, Math.min(23, hora)),
                Math.max(0, Math.min(59, minuto)),
                0
        );
        return fecha.atTime(time).atZone(ZONA_LIMA).toOffsetDateTime();
    }

    public static String formatearFechaHoraIso(OffsetDateTime odt) {
        if (odt == null) return null;
        return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static String formatearFechaHoraTexto(String isoString) {
        return formatearFechaHoraTexto(isoString, ZoneId.systemDefault());
    }

    public static String formatearFechaHoraTexto(String isoString, ZoneId zona) {
        if (isoString == null || isoString.isBlank()) {
            return "--/--/---- --:--";
        }
        ZoneId zonaEfectiva = zona != null ? zona : ZoneId.systemDefault();
        try {
            OffsetDateTime odt = OffsetDateTime.parse(isoString);
            return odt.atZoneSameInstant(zonaEfectiva).format(FORMATO_FECHA_HORA_CORTO);
        } catch (Exception e1) {
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(isoString);
                return ldt.format(FORMATO_FECHA_HORA_CORTO);
            } catch (Exception e2) {
                return isoString;
            }
        }
    }

    public static String formatearFechaHoraCompleta(OffsetDateTime odt) {
        return formatearFechaHoraCompleta(odt, ZoneId.systemDefault());
    }

    public static String formatearFechaHoraCompleta(OffsetDateTime odt, ZoneId zona) {
        if (odt == null) return "--/--/---- --:--:--";
        ZoneId zonaEfectiva = zona != null ? zona : ZoneId.systemDefault();
        return odt.atZoneSameInstant(zonaEfectiva).format(FORMATO_FECHA_HORA);
    }

    // =========================================================================
    // VALIDACIONES DE FORMULARIO
    // =========================================================================

    public static List<String> validarVehiculo(String placa, String modelo, BigDecimal capacidadCargaKg) {
        List<String> errores = new ArrayList<>();
        if (placa == null || placa.isBlank()) {
            errores.add("La placa es obligatoria");
        } else {
            String p = placa.trim().toUpperCase();
            if (p.length() < 5 || p.length() > 10 || !PATRON_PLACA.matcher(p).matches()) {
                errores.add("La placa debe tener entre 5 y 10 caracteres alfanuméricos en mayúsculas (ej. ABC-123)");
            }
        }

        if (modelo == null || modelo.isBlank()) {
            errores.add("El modelo del vehículo es obligatorio");
        } else if (modelo.trim().length() < 2 || modelo.trim().length() > 100) {
            errores.add("El modelo debe tener entre 2 y 100 caracteres");
        }

        if (capacidadCargaKg == null) {
            errores.add("La capacidad de carga es obligatoria");
        } else if (capacidadCargaKg.compareTo(BigDecimal.ZERO) <= 0) {
            errores.add("La capacidad de carga debe ser mayor a cero");
        }

        return errores;
    }

    public static List<String> validarViaje(RutaDto ruta,
                                            VehiculoDto vehiculo,
                                            EmpleadoDto chofer,
                                            LocalDate fechaSalida,
                                            int horaSalida,
                                            int minSalida,
                                            LocalDate fechaLlegada,
                                            int horaLlegada,
                                            int minLlegada) {
        List<String> errores = new ArrayList<>();

        if (ruta == null || ruta.id() == null || ruta.id().isBlank()) {
            errores.add("Debe seleccionar una ruta de transporte");
        }
        if (vehiculo == null || vehiculo.idVehiculo() == null || vehiculo.idVehiculo().isBlank()) {
            errores.add("Debe seleccionar un vehículo de la flota");
        }
        if (chofer == null || chofer.idEmpleado() == null || chofer.idEmpleado().isBlank()) {
            errores.add("Debe seleccionar un chofer calificado");
        }

        if (fechaSalida == null) {
            errores.add("Debe indicar la fecha de salida");
        }
        if (fechaLlegada == null) {
            errores.add("Debe indicar la fecha de llegada estimada");
        }

        if (fechaSalida != null && fechaLlegada != null) {
            OffsetDateTime salida = construirOffsetDateTimeLima(fechaSalida, horaSalida, minSalida);
            OffsetDateTime llegada = construirOffsetDateTimeLima(fechaLlegada, horaLlegada, minLlegada);

            if (!llegada.isAfter(salida)) {
                errores.add("La fecha y hora de llegada estimada debe ser posterior a la salida");
            }
        }

        return errores;
    }

    public static Optional<String> primerError(List<String> errores) {
        if (errores == null || errores.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(errores.get(0));
    }

    // =========================================================================
    // HABILITACIÓN DE ACCIONES SEGÚN ESTADO Y ROL
    // =========================================================================

    public static boolean puedeIniciarViaje(ViajeDto viaje) {
        return viaje != null && "PROGRAMADO".equalsIgnoreCase(viaje.estadoViaje());
    }

    public static boolean puedeFinalizarViaje(ViajeDto viaje) {
        return viaje != null && "EN_RUTA".equalsIgnoreCase(viaje.estadoViaje());
    }

    public static boolean puedeVerManifiesto(ViajeDto viaje) {
        return viaje != null && viaje.idViaje() != null && !viaje.idViaje().isBlank();
    }

    public static boolean puedeCrearViaje(String rol) {
        return "TN3".equalsIgnoreCase(ModeloMenuRol.normalizarRol(rol));
    }

    public static boolean puedeGestionarFlota(String rol) {
        return "TN3".equalsIgnoreCase(ModeloMenuRol.normalizarRol(rol));
    }

    // =========================================================================
    // TRADUCCIÓN DE CÓDIGOS DE ERROR DE LA API
    // =========================================================================

    public static String traducirCodigoError(String codigo, String detallePorDefecto) {
        if (codigo == null) {
            return (detallePorDefecto != null && !detallePorDefecto.isBlank()) ? detallePorDefecto : "Error en la operación";
        }
        return switch (codigo.trim().toUpperCase()) {
            case "PLACA_DUPLICADA" -> "Ya existe un vehículo con esa placa";
            case "CHOFER_INVALIDO" -> "El colaborador elegido no es un chofer activo";
            case "CHOFER_OCUPADO" -> "El chofer ya tiene un viaje en ruta";
            case "VEHICULO_OCUPADO" -> "El vehículo ya está en un viaje en ruta";
            case "FECHA_INVALIDA" -> "Revisa las fechas del viaje";
            case "TRANSICION_INVALIDA" -> "El viaje ya cambió de estado: actualiza la lista";
            case "ACCESO_DENEGADO", "HTTP_403" -> "No tienes permiso para esta acción";
            case "NO_AUTORIZADO", "HTTP_401" -> "Sesión expirada o no autorizada";
            case "VALIDACION_FALLIDA" -> (detallePorDefecto != null && !detallePorDefecto.isBlank()) ? detallePorDefecto : "Datos de entrada inválidos";
            case "SIN_CONEXION" -> "Requiere conexión";
            default -> (detallePorDefecto != null && !detallePorDefecto.isBlank()) ? detallePorDefecto : "Error en el servidor central: " + codigo;
        };
    }

    public static String limpiarMensajeError(Throwable error) {
        if (error == null) {
            return "Error inesperado en el módulo de rutas y flota";
        }
        if (error instanceof ApiExcepcion ae) {
            if (ae.getStatusCode() == 403) {
                return "No tienes permiso para esta acción";
            }
            if (ae.getStatusCode() == 401) {
                return "Sesión expirada o no autorizada";
            }
            if ("SIN_CONEXION".equalsIgnoreCase(ae.getCodigoError()) || ae.getStatusCode() == 0) {
                return "Requiere conexión";
            }
            if (ae.getCodigoError() != null && !ae.getCodigoError().isBlank()) {
                String traducido = traducirCodigoError(ae.getCodigoError(), ae.getMessage());
                return traducido.replace("null", "").trim();
            }
            String msg = ae.getMessage();
            if (msg != null && !msg.isBlank() && !msg.equalsIgnoreCase("null")) {
                return msg.replace("null", "").trim();
            }
        }
        String msg = error.getMessage();
        if (msg == null || msg.isBlank() || msg.equalsIgnoreCase("null")) {
            return "Requiere conexión";
        }
        if (msg.contains("Connection refused") || msg.contains("ConnectException") || msg.contains("SIN_CONEXION")) {
            return "Requiere conexión";
        }
        return msg.replace("null", "").trim();
    }

    // =========================================================================
    // CÁLCULOS DEL MANIFIESTO
    // =========================================================================

    public static BigDecimal calcularPesoTotalManifiesto(List<ManifiestoEncomiendaDto> encomiendas) {
        if (encomiendas == null || encomiendas.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return encomiendas.stream()
                .map(ManifiestoEncomiendaDto::pesoKg)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static String formatearTituloManifiesto(ViajeDto viaje) {
        if (viaje == null) {
            return "Manifiesto de Carga";
        }
        String origen = viaje.ruta() != null && viaje.ruta().origen() != null && viaje.ruta().origen().nombreAgencia() != null
                ? viaje.ruta().origen().nombreAgencia().trim()
                : (viaje.idSucursalOrigen() != null ? viaje.idSucursalOrigen().trim() : "Origen");
        String destino = viaje.ruta() != null && viaje.ruta().destino() != null && viaje.ruta().destino().nombreAgencia() != null
                ? viaje.ruta().destino().nombreAgencia().trim()
                : (viaje.idSucursalDestino() != null ? viaje.idSucursalDestino().trim() : "Destino");
        String placa = viaje.placaEfectiva();
        if ("S/P".equals(placa) && viaje.idViaje() != null) {
            placa = viaje.idViaje();
        }
        return String.format("Manifiesto · %s → %s · %s", origen, destino, placa);
    }

    public static String textoResumenManifiesto(List<ManifiestoEncomiendaDto> encomiendas) {
        int total = (encomiendas != null) ? encomiendas.size() : 0;
        BigDecimal peso = calcularPesoTotalManifiesto(encomiendas);
        return String.format(java.util.Locale.US, "Total encomiendas: %d | Peso total: %.2f kg", total, peso);
    }
}
