package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.marvisur.sgldqr.api.dto.dashboard.DashboardResumenResponse;
import pe.marvisur.sgldqr.api.dto.dashboard.PeriodoDashboard;
import pe.marvisur.sgldqr.api.servicio.DashboardServicio;

import java.util.Objects;

/**
 * Controlador REST para el Dashboard Administrativo (RF-02).
 * Proporciona indicadores operativos consolidados en tiempo real.
 * Restringido exclusivamente al rol Administrador (TN3).
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard Administrativo", description = "Endpoints de KPIs operativos y resumen consolidado en tiempo real (RF-02)")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    private final DashboardServicio dashboardServicio;

    public DashboardController(DashboardServicio dashboardServicio) {
        this.dashboardServicio = Objects.requireNonNull(dashboardServicio, "El dashboardServicio no puede ser nulo");
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasAuthority('ROLE_ADMINISTRADOR')")
    @Operation(
            summary = "Obtener resumen de KPIs operativos del dashboard",
            description = "Retorna indicadores agregados en tiempo real para el período especificado (HOY, SIETE_DIAS, TREINTA_DIAS) " +
                    "y opcionalmente filtrados por sucursal de origen. Requiere rol Administrador (TN3).\n\n" +
                    "LIMITACIÓN CONOCIDA: El valor 'recaudado' contabiliza el monto_total de encomiendas en estado PAGADO " +
                    "según su fecha de REGISTRO (dentro del período), debido a que el Diccionario de Datos actual no almacena " +
                    "la fecha exacta del cobro."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resumen de KPIs operativos generado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DashboardResumenResponse.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                            "  \"generadoEn\": \"2026-10-24T18:30:00-05:00\",\n" +
                                            "  \"periodo\": \"HOY\",\n" +
                                            "  \"idSucursal\": null,\n" +
                                            "  \"totales\": {\n" +
                                            "    \"registradas\": 42,\n" +
                                            "    \"entregadas\": 35,\n" +
                                            "    \"enRuta\": 4,\n" +
                                            "    \"pendientesDePago\": { \"cantidad\": 2, \"monto\": 90.00 },\n" +
                                            "    \"recaudado\": 1850.50\n" +
                                            "  },\n" +
                                            "  \"porEstadoLogistico\": {\n" +
                                            "    \"REGISTRADO\": 1,\n" +
                                            "    \"EN_ALMACEN_ORIGEN\": 2,\n" +
                                            "    \"EN_RUTA\": 4,\n" +
                                            "    \"EN_ALMACEN_DESTINO\": 0,\n" +
                                            "    \"ENTREGADO\": 35\n" +
                                            "  },\n" +
                                            "  \"recaudacionPorSucursal\": [\n" +
                                            "    {\n" +
                                            "      \"idSucursal\": \"SUC-LIM-01\",\n" +
                                            "      \"nombreAgencia\": \"Agencia Central\",\n" +
                                            "      \"recaudado\": 1200.00,\n" +
                                            "      \"pendiente\": 50.00\n" +
                                            "    }\n" +
                                            "  ],\n" +
                                            "  \"validaciones\": {\n" +
                                            "    \"total\": 35,\n" +
                                            "    \"exitosas\": 33,\n" +
                                            "    \"rechazadasPagoPendiente\": 1,\n" +
                                            "    \"rechazadasTokenInvalido\": 1,\n" +
                                            "    \"latenciaPromedioMs\": 142.5,\n" +
                                            "    \"latenciaMaximaMs\": 310\n" +
                                            "  },\n" +
                                            "  \"estancados\": [\n" +
                                            "    {\n" +
                                            "      \"codigoComprobante\": \"T001-00000042\",\n" +
                                            "      \"estadoLogistico\": \"EN_ALMACEN_ORIGEN\",\n" +
                                            "      \"nombreAgenciaDestino\": \"Agencia Arequipa\",\n" +
                                            "      \"horasSinMovimiento\": 52\n" +
                                            "    }\n" +
                                            "  ],\n" +
                                            "  \"umbralEstancadoHoras\": 48\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "No autenticado o token JWT ausente/inválido (código: NO_AUTORIZADO)"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado: requiere rol Administrador TN3 (código: ACCESO_DENEGADO)")
    })
    public ResponseEntity<DashboardResumenResponse> obtenerResumen(
            @Parameter(description = "Período temporal para la agregación de métricas (por defecto HOY)")
            @RequestParam(name = "periodo", required = false, defaultValue = "HOY") PeriodoDashboard periodo,
            @Parameter(description = "Identificador de la sucursal de origen para filtrar (opcional)")
            @RequestParam(name = "idSucursal", required = false) String idSucursal
    ) {
        DashboardResumenResponse resumen = dashboardServicio.obtenerResumenDashboard(periodo, idSucursal);
        return ResponseEntity.ok(resumen);
    }
}
