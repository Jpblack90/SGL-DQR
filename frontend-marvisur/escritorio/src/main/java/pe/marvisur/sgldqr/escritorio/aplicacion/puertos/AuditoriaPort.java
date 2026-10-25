package pe.marvisur.sgldqr.escritorio.aplicacion.puertos;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ItemAuditoriaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PaginaDto;

import java.time.OffsetDateTime;

/**
 * Puerto de operaciones para la consulta de la bitácora de auditoría operativa y de seguridad (RF-03).
 */
public interface AuditoriaPort {

    PaginaDto<ItemAuditoriaDto> listarAuditoria(String idEmpleado, String accion, OffsetDateTime desde, OffsetDateTime hasta, int pagina, int tamano);
}
