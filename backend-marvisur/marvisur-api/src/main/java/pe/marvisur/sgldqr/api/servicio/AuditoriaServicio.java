package pe.marvisur.sgldqr.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.marvisur.sgldqr.api.dto.ItemAuditoriaDto;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.especificacion.AuditoriaFiltroEspecificacion;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para la consulta y trazabilidad de bitácora de auditoría (RF-03).
 */
@Service
public class AuditoriaServicio {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaServicio.class);

    private final AuditoriaLogSpringDataRepository auditoriaRepository;
    private final EmpleadoSpringDataRepository empleadoRepository;

    public AuditoriaServicio(AuditoriaLogSpringDataRepository auditoriaRepository,
                             EmpleadoSpringDataRepository empleadoRepository) {
        this.auditoriaRepository = Objects.requireNonNull(auditoriaRepository, "auditoriaRepository no puede ser nulo");
        this.empleadoRepository = Objects.requireNonNull(empleadoRepository, "empleadoRepository no puede ser nulo");
    }

    /**
     * Consulta paginada y filtrada de registros de auditoría operativa.
     */
    @Transactional(readOnly = true)
    public Page<ItemAuditoriaDto> listarAuditoria(String idEmpleado, String accion, OffsetDateTime desde, OffsetDateTime hasta, Pageable pageable) {
        int tamanioSeguro = Math.min(pageable != null ? pageable.getPageSize() : 20, 100);
        int numeroPagina = pageable != null ? pageable.getPageNumber() : 0;
        Pageable pageableAjustado = PageRequest.of(numeroPagina, tamanioSeguro, Sort.by(Sort.Direction.DESC, "fechaHora"));

        String empleadoFiltro = (idEmpleado != null && !idEmpleado.isBlank()) ? idEmpleado.trim() : null;
        String accionFiltro = (accion != null && !accion.isBlank()) ? accion.trim() : null;

        Specification<AuditoriaLogJpaEntity> spec = AuditoriaFiltroEspecificacion.conFiltros(
                empleadoFiltro, accionFiltro, desde, hasta
        );

        Page<AuditoriaLogJpaEntity> paginaLogs = auditoriaRepository.findAll(spec, pageableAjustado);

        Set<String> idsEmpleados = paginaLogs.getContent().stream()
                .map(AuditoriaLogJpaEntity::getIdEmpleado)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> nombresEmpleados = empleadoRepository.findAllById(idsEmpleados).stream()
                .collect(Collectors.toMap(
                        EmpleadoJpaEntity::getIdEmpleado,
                        e -> (e.getNombres() + " " + e.getApellidos()).trim(),
                        (v1, v2) -> v1
                ));

        return paginaLogs.map(logEntity -> {
            String nombreEmpleado;
            if (logEntity.getIdEmpleado() == null) {
                nombreEmpleado = "Sistema / No autenticado";
            } else {
                nombreEmpleado = nombresEmpleados.getOrDefault(logEntity.getIdEmpleado(), "Personal no disponible");
            }

            return new ItemAuditoriaDto(
                    logEntity.getIdLog(),
                    logEntity.getIdEmpleado(),
                    nombreEmpleado,
                    logEntity.getAccion(),
                    logEntity.getTablaAfectada(),
                    logEntity.getIdRegistroAfectado(),
                    logEntity.getFechaHora(),
                    logEntity.getIpOrigen()
            );
        });
    }
}
