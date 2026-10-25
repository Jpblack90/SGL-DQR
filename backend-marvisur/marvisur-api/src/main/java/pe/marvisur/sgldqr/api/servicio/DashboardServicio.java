package pe.marvisur.sgldqr.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.dto.dashboard.DashboardResumenResponse;
import pe.marvisur.sgldqr.api.dto.dashboard.EncomiendaEstancadaDto;
import pe.marvisur.sgldqr.api.dto.dashboard.PendientesPagoDto;
import pe.marvisur.sgldqr.api.dto.dashboard.PeriodoDashboard;
import pe.marvisur.sgldqr.api.dto.dashboard.RangoPeriodo;
import pe.marvisur.sgldqr.api.dto.dashboard.RecaudacionSucursalDto;
import pe.marvisur.sgldqr.api.dto.dashboard.TotalesDashboardDto;
import pe.marvisur.sgldqr.api.dto.dashboard.ValidacionesDashboardDto;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ConteoEstadoLogisticoProjection;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaEstancadaProjection;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.MetricasValidacionProjection;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RecaudacionSucursalProjection;
import pe.marvisur.sgldqr.api.persistencia.repositorio.TotalesDashboardProjection;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ValidacionCruzadaLogSpringDataRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Servicio de negocio para el cálculo y agregación en tiempo real de indicadores
 * y KPIs operativos del dashboard administrativo (RF-02).
 */
@Service
public class DashboardServicio {

    private static final Logger log = LoggerFactory.getLogger(DashboardServicio.class);

    private final EncomiendaSpringDataRepository encomiendaRepository;
    private final ValidacionCruzadaLogSpringDataRepository validacionLogRepository;
    private final CalculadorPeriodoDashboard calculador;

    @Value("${app.dashboard.zona-horaria:America/Lima}")
    private String zonaHorariaConfig;

    // TODO: confirmar umbral con Marvisur
    @Value("${app.dashboard.horas-estancado:48}")
    private int umbralEstancadoHoras;

    public DashboardServicio(EncomiendaSpringDataRepository encomiendaRepository,
                             ValidacionCruzadaLogSpringDataRepository validacionLogRepository,
                             CalculadorPeriodoDashboard calculador) {
        this.encomiendaRepository = Objects.requireNonNull(encomiendaRepository, "El encomiendaRepository no puede ser nulo");
        this.validacionLogRepository = Objects.requireNonNull(validacionLogRepository, "El validacionLogRepository no puede ser nulo");
        this.calculador = Objects.requireNonNull(calculador, "El calculador no puede ser nulo");
    }

    /**
     * Consulta y consolida todos los indicadores del dashboard para el período y sucursal indicados.
     */
    @Transactional(readOnly = true)
    public DashboardResumenResponse obtenerResumenDashboard(PeriodoDashboard periodo, String idSucursal) {
        PeriodoDashboard periodoEfectivo = periodo != null ? periodo : PeriodoDashboard.HOY;
        String idSucursalFiltro = (idSucursal != null && !idSucursal.trim().isEmpty()) ? idSucursal.trim() : null;

        ZoneId zona;
        try {
            zona = ZoneId.of(zonaHorariaConfig);
        } catch (Exception ex) {
            log.warn("Zona horaria '{}' no válida. Empleando America/Lima por defecto.", zonaHorariaConfig);
            zona = ZoneId.of("America/Lima");
        }

        OffsetDateTime generadoEn = calculador.obtenerAhoraConZona(zona);
        RangoPeriodo rango = calculador.calcularRango(periodoEfectivo, zona);

        log.debug("Generando resumen dashboard: periodo={}, rango=[{} - {}), sucursal={}",
                periodoEfectivo, rango.inicio(), rango.fin(), idSucursalFiltro);

        // 1. Totales del período
        TotalesDashboardDto totales = calcularTotales(rango, idSucursalFiltro);

        // 2. Conteo por estado logístico actual (independiente del período)
        Map<String, Long> porEstadoLogistico = calcularPorEstadoLogistico(idSucursalFiltro);

        // 3. Recaudación agrupada por sucursal de origen
        List<RecaudacionSucursalDto> recaudacionPorSucursal = calcularRecaudacionPorSucursal(rango, idSucursalFiltro);

        // 4. Métricas de validación cruzada Doble QR
        ValidacionesDashboardDto validaciones = calcularMetricasValidacion(rango, idSucursalFiltro);

        // 5. Encomiendas estancadas sin movimiento en las últimas N horas
        List<EncomiendaEstancadaDto> estancados = buscarEncomiendasEstancadas(idSucursalFiltro);

        return new DashboardResumenResponse(
                generadoEn,
                periodoEfectivo,
                idSucursalFiltro,
                totales,
                porEstadoLogistico,
                recaudacionPorSucursal,
                validaciones,
                estancados,
                umbralEstancadoHoras
        );
    }

    private TotalesDashboardDto calcularTotales(RangoPeriodo rango, String idSucursal) {
        TotalesDashboardProjection p = encomiendaRepository.obtenerTotalesPeriodo(
                rango.inicio(), rango.fin(), idSucursal
        );

        long registradas = (p != null && p.getRegistradas() != null) ? p.getRegistradas() : 0L;
        long entregadas = encomiendaRepository.contarEntregadasEnPeriodo(rango.inicio(), rango.fin(), idSucursal);
        long enRuta = (p != null && p.getEnRuta() != null) ? p.getEnRuta() : 0L;
        long pendCant = (p != null && p.getPendientesCantidad() != null) ? p.getPendientesCantidad() : 0L;
        BigDecimal pendMonto = (p != null && p.getPendientesMonto() != null)
                ? p.getPendientesMonto().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal recaudado = (p != null && p.getRecaudado() != null)
                ? p.getRecaudado().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new TotalesDashboardDto(
                registradas,
                entregadas,
                enRuta,
                new PendientesPagoDto(pendCant, pendMonto),
                recaudado
        );
    }

    private Map<String, Long> calcularPorEstadoLogistico(String idSucursal) {
        Map<String, Long> estados = new LinkedHashMap<>();
        estados.put("REGISTRADO", 0L);
        estados.put("EN_ALMACEN_ORIGEN", 0L);
        estados.put("EN_RUTA", 0L);
        estados.put("EN_ALMACEN_DESTINO", 0L);
        estados.put("ENTREGADO", 0L);

        List<ConteoEstadoLogisticoProjection> lista = encomiendaRepository.contarPorEstadoLogistico(idSucursal);
        if (lista != null) {
            for (ConteoEstadoLogisticoProjection c : lista) {
                if (c.getEstadoLogistico() != null) {
                    estados.put(c.getEstadoLogistico(), c.getTotal() != null ? c.getTotal() : 0L);
                }
            }
        }
        return estados;
    }

    private List<RecaudacionSucursalDto> calcularRecaudacionPorSucursal(RangoPeriodo rango, String idSucursal) {
        List<RecaudacionSucursalProjection> lista = encomiendaRepository.obtenerRecaudacionPorSucursal(
                rango.inicio(), rango.fin(), idSucursal
        );
        List<RecaudacionSucursalDto> resultado = new ArrayList<>();
        if (lista != null) {
            for (RecaudacionSucursalProjection r : lista) {
                resultado.add(new RecaudacionSucursalDto(
                        r.getIdSucursal(),
                        r.getNombreAgencia(),
                        r.getRecaudado() != null ? r.getRecaudado().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        r.getPendiente() != null ? r.getPendiente().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                ));
            }
        }
        return resultado;
    }

    private ValidacionesDashboardDto calcularMetricasValidacion(RangoPeriodo rango, String idSucursal) {
        MetricasValidacionProjection m = validacionLogRepository.obtenerMetricasValidacion(
                rango.inicio(), rango.fin(), idSucursal
        );

        long total = (m != null && m.getTotal() != null) ? m.getTotal() : 0L;
        long exitosas = (m != null && m.getExitosas() != null) ? m.getExitosas() : 0L;
        long rechPago = (m != null && m.getRechazadasPagoPendiente() != null) ? m.getRechazadasPagoPendiente() : 0L;
        long rechToken = (m != null && m.getRechazadasTokenInvalido() != null) ? m.getRechazadasTokenInvalido() : 0L;
        Double latProm = (m != null && m.getLatenciaPromedioMs() != null)
                ? Math.round(m.getLatenciaPromedioMs() * 10.0) / 10.0
                : 0.0;
        Integer latMax = (m != null && m.getLatenciaMaximaMs() != null) ? m.getLatenciaMaximaMs() : 0;

        return new ValidacionesDashboardDto(
                total,
                exitosas,
                rechPago,
                rechToken,
                latProm,
                latMax
        );
    }

    private List<EncomiendaEstancadaDto> buscarEncomiendasEstancadas(String idSucursal) {
        OffsetDateTime limite = calculador.calcularLimiteEstancado(umbralEstancadoHoras);
        List<EncomiendaEstancadaProjection> lista = encomiendaRepository.buscarEstancadas(
                limite, idSucursal, PageRequest.of(0, 10)
        );

        List<EncomiendaEstancadaDto> resultado = new ArrayList<>();
        if (lista != null) {
            for (EncomiendaEstancadaProjection p : lista) {
                resultado.add(new EncomiendaEstancadaDto(
                        p.getCodigoComprobante(),
                        p.getEstadoLogistico(),
                        p.getNombreAgenciaDestino(),
                        calculador.calcularHorasSinMovimiento(p.getUltimoMovimiento())
                ));
            }
        }
        return resultado;
    }

    public int getUmbralEstancadoHoras() {
        return umbralEstancadoHoras;
    }

    public void setUmbralEstancadoHoras(int umbralEstancadoHoras) {
        this.umbralEstancadoHoras = umbralEstancadoHoras;
    }

    public String getZonaHorariaConfig() {
        return zonaHorariaConfig;
    }

    public void setZonaHorariaConfig(String zonaHorariaConfig) {
        this.zonaHorariaConfig = zonaHorariaConfig;
    }
}
