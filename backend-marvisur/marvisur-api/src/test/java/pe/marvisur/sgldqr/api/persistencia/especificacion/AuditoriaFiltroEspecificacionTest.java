package pe.marvisur.sgldqr.api.persistencia.especificacion;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Pruebas Unitarias — Constructor de Especificaciones de Auditoría (RF-03)")
class AuditoriaFiltroEspecificacionTest {

    private Root<AuditoriaLogJpaEntity> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;
    private Path<String> pathIdEmpleado;
    private Path<String> pathAccion;
    private Path<OffsetDateTime> pathFechaHora;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);

        pathIdEmpleado = mock(Path.class);
        pathAccion = mock(Path.class);
        pathFechaHora = mock(Path.class);

        when(root.<String>get("idEmpleado")).thenReturn(pathIdEmpleado);
        when(root.<String>get("accion")).thenReturn(pathAccion);
        when(root.<OffsetDateTime>get("fechaHora")).thenReturn(pathFechaHora);
    }

    @Test
    @DisplayName("Sin filtros: no agrega ninguna condición y retorna conjunción vacía")
    void sinFiltrosNoAgregaCondiciones() {
        Predicate conjuncion = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjuncion);

        Specification<AuditoriaLogJpaEntity> spec = AuditoriaFiltroEspecificacion.conFiltros(null, null, null, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(conjuncion);
        verify(cb).conjunction();
        verify(cb, never()).equal(any(), any());
        verify(cb, never()).greaterThanOrEqualTo(any(), any(OffsetDateTime.class));
        verify(cb, never()).lessThanOrEqualTo(any(), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Filtros con cadenas vacías o en blanco: se tratan como nulos y retornan conjunción")
    void filtrosEnBlancoSeIgnoran() {
        Predicate conjuncion = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjuncion);

        Specification<AuditoriaLogJpaEntity> spec = AuditoriaFiltroEspecificacion.conFiltros("   ", "  ", null, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(conjuncion);
        verify(cb).conjunction();
        verify(cb, never()).equal(any(), any());
    }

    @Test
    @DisplayName("Solo idEmpleado: agrega únicamente la condición de igualdad para idEmpleado")
    void soloIdEmpleadoAgregaSoloSuCondicion() {
        Predicate pred = mock(Predicate.class);
        when(cb.equal(pathIdEmpleado, "EMP-ADMIN-01")).thenReturn(pred);
        when(cb.and(any(Predicate[].class))).thenReturn(pred);

        Specification<AuditoriaLogJpaEntity> spec = AuditoriaFiltroEspecificacion.conFiltros("EMP-ADMIN-01", null, null, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).equal(pathIdEmpleado, "EMP-ADMIN-01");
        verify(cb, never()).equal(eq(pathAccion), any());
        verify(cb, never()).greaterThanOrEqualTo(any(), any(OffsetDateTime.class));
        verify(cb, never()).lessThanOrEqualTo(any(), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Solo acción: agrega únicamente la condición de igualdad para acción")
    void soloAccionAgregaSoloSuCondicion() {
        Predicate pred = mock(Predicate.class);
        when(cb.equal(pathAccion, "LOGIN_EXITOSO")).thenReturn(pred);
        when(cb.and(any(Predicate[].class))).thenReturn(pred);

        Specification<AuditoriaLogJpaEntity> spec = AuditoriaFiltroEspecificacion.conFiltros(null, "LOGIN_EXITOSO", null, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).equal(pathAccion, "LOGIN_EXITOSO");
        verify(cb, never()).equal(eq(pathIdEmpleado), any());
        verify(cb, never()).greaterThanOrEqualTo(any(), any(OffsetDateTime.class));
        verify(cb, never()).lessThanOrEqualTo(any(), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Solo fecha desde: agrega únicamente la condición greaterThanOrEqualTo")
    void soloDesdeAgregaSoloSuCondicion() {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(1);
        Predicate pred = mock(Predicate.class);
        when(cb.greaterThanOrEqualTo(pathFechaHora, desde)).thenReturn(pred);
        when(cb.and(any(Predicate[].class))).thenReturn(pred);

        Specification<AuditoriaLogJpaEntity> spec = AuditoriaFiltroEspecificacion.conFiltros(null, null, desde, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).greaterThanOrEqualTo(pathFechaHora, desde);
        verify(cb, never()).lessThanOrEqualTo(any(), any(OffsetDateTime.class));
        verify(cb, never()).equal(any(), any());
    }

    @Test
    @DisplayName("Solo fecha hasta: agrega únicamente la condición lessThanOrEqualTo")
    void soloHastaAgregaSoloSuCondicion() {
        OffsetDateTime hasta = OffsetDateTime.now();
        Predicate pred = mock(Predicate.class);
        when(cb.lessThanOrEqualTo(pathFechaHora, hasta)).thenReturn(pred);
        when(cb.and(any(Predicate[].class))).thenReturn(pred);

        Specification<AuditoriaLogJpaEntity> spec = AuditoriaFiltroEspecificacion.conFiltros(null, null, null, hasta);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).lessThanOrEqualTo(pathFechaHora, hasta);
        verify(cb, never()).greaterThanOrEqualTo(any(), any(OffsetDateTime.class));
        verify(cb, never()).equal(any(), any());
    }

    @Test
    @DisplayName("Combinación de filtros: agrega únicamente las condiciones presentes combinadas con AND")
    void combinacionFiltrosAgregaCondicionesPresentes() {
        OffsetDateTime desde = OffsetDateTime.now().minusHours(2);
        Predicate predEmpleado = mock(Predicate.class);
        Predicate predDesde = mock(Predicate.class);
        Predicate predAnd = mock(Predicate.class);

        when(cb.equal(pathIdEmpleado, "EMP-002")).thenReturn(predEmpleado);
        when(cb.greaterThanOrEqualTo(pathFechaHora, desde)).thenReturn(predDesde);
        when(cb.and(any(Predicate[].class))).thenReturn(predAnd);

        Specification<AuditoriaLogJpaEntity> spec = AuditoriaFiltroEspecificacion.conFiltros("EMP-002", null, desde, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(predAnd);
        verify(cb).equal(pathIdEmpleado, "EMP-002");
        verify(cb).greaterThanOrEqualTo(pathFechaHora, desde);
        verify(cb, never()).equal(eq(pathAccion), any());
        verify(cb, never()).lessThanOrEqualTo(any(), any(OffsetDateTime.class));
    }
}
