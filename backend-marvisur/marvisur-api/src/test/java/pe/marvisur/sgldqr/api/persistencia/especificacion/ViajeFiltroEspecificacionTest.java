package pe.marvisur.sgldqr.api.persistencia.especificacion;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.RutaJpaEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Pruebas Unitarias — Constructor de Especificaciones de Viajes (RF-05)")
class ViajeFiltroEspecificacionTest {

    private Root<ProgramacionViajeJpaEntity> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;

    private Path<String> pathEstadoViaje;
    private Path<String> pathIdChofer;
    private Path<String> pathIdRuta;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);

        pathEstadoViaje = mock(Path.class);
        pathIdChofer = mock(Path.class);
        pathIdRuta = mock(Path.class);

        when(root.<String>get("estadoViaje")).thenReturn(pathEstadoViaje);
        when(root.<String>get("idChofer")).thenReturn(pathIdChofer);
        when(root.<String>get("idRuta")).thenReturn(pathIdRuta);
    }

    @Test
    @DisplayName("Sin filtros (nulos o blancos): no agrega ninguna condición y retorna conjunción vacía")
    void sinFiltrosNoAgregaCondiciones() {
        Predicate conjuncion = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjuncion);

        Specification<ProgramacionViajeJpaEntity> spec = ViajeFiltroEspecificacion.conFiltros(null, null, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(conjuncion);
        verify(cb).conjunction();
        verify(cb, never()).equal(any(), any());
        verify(cb, never()).and(any(Predicate[].class));

        // Con cadenas en blanco
        Specification<ProgramacionViajeJpaEntity> specBlancos = ViajeFiltroEspecificacion.conFiltros("   ", "  ", "   ");
        Predicate resultadoBlancos = specBlancos.toPredicate(root, query, cb);
        assertThat(resultadoBlancos).isSameAs(conjuncion);
    }

    @Test
    @DisplayName("Filtro solo por estadoViaje: agrega exclusivamente el predicado de estado")
    void soloEstadoAgregaPredicado() {
        Predicate predicadoEstado = mock(Predicate.class);
        when(cb.equal(pathEstadoViaje, "PROGRAMADO")).thenReturn(predicadoEstado);
        when(cb.and(any(Predicate[].class))).thenReturn(predicadoEstado);

        Specification<ProgramacionViajeJpaEntity> spec = ViajeFiltroEspecificacion.conFiltros("PROGRAMADO", null, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(predicadoEstado);
        verify(cb).equal(pathEstadoViaje, "PROGRAMADO");
        verify(cb, never()).equal(eq(pathIdChofer), any());
        verify(query, never()).subquery(any());
    }

    @Test
    @DisplayName("Filtro solo por idChofer: agrega exclusivamente el predicado de chofer")
    void soloChoferAgregaPredicado() {
        Predicate predicadoChofer = mock(Predicate.class);
        when(cb.equal(pathIdChofer, "EMP-004")).thenReturn(predicadoChofer);
        when(cb.and(any(Predicate[].class))).thenReturn(predicadoChofer);

        Specification<ProgramacionViajeJpaEntity> spec = ViajeFiltroEspecificacion.conFiltros(null, null, "EMP-004");
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(predicadoChofer);
        verify(cb).equal(pathIdChofer, "EMP-004");
        verify(cb, never()).equal(eq(pathEstadoViaje), any());
        verify(query, never()).subquery(any());
    }

    @Test
    @DisplayName("Filtro solo por idSucursalOrigen: genera subquery sobre rutas y agrega predicado IN")
    @SuppressWarnings("unchecked")
    void soloSucursalOrigenAgregaSubquery() {
        Subquery<String> subquery = mock(Subquery.class);
        Root<RutaJpaEntity> rutaRoot = mock(Root.class);
        Path<String> pathSubqueryIdRuta = mock(Path.class);
        Path<String> pathSubqueryOrigen = mock(Path.class);
        Predicate predicadoSubquery = mock(Predicate.class);
        Predicate predicadoIn = mock(Predicate.class);

        when(query.subquery(String.class)).thenReturn(subquery);
        when(subquery.from(RutaJpaEntity.class)).thenReturn(rutaRoot);
        when(rutaRoot.<String>get("idRuta")).thenReturn(pathSubqueryIdRuta);
        when(rutaRoot.<String>get("idSucursalOrigen")).thenReturn(pathSubqueryOrigen);
        when(cb.equal(pathSubqueryOrigen, "SUC-LIM-01")).thenReturn(predicadoSubquery);
        when(subquery.select(pathSubqueryIdRuta)).thenReturn(subquery);
        when(subquery.where(predicadoSubquery)).thenReturn(subquery);
        when(pathIdRuta.in(subquery)).thenReturn(predicadoIn);
        when(cb.and(any(Predicate[].class))).thenReturn(predicadoIn);

        Specification<ProgramacionViajeJpaEntity> spec = ViajeFiltroEspecificacion.conFiltros(null, "SUC-LIM-01", null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(predicadoIn);
        verify(query).subquery(String.class);
        verify(pathIdRuta).in(subquery);
        verify(cb, never()).equal(eq(pathEstadoViaje), any());
        verify(cb, never()).equal(eq(pathIdChofer), any());
    }

    @Test
    @DisplayName("Combinación de todos los filtros: combina los 3 predicados con AND")
    @SuppressWarnings("unchecked")
    void combinacionDeTodosLosFiltros() {
        Predicate predEstado = mock(Predicate.class);
        Predicate predChofer = mock(Predicate.class);
        Predicate predIn = mock(Predicate.class);
        Predicate predAnd = mock(Predicate.class);

        Subquery<String> subquery = mock(Subquery.class);
        Root<RutaJpaEntity> rutaRoot = mock(Root.class);
        Path<String> pathSubIdRuta = mock(Path.class);
        Path<String> pathSubOrigen = mock(Path.class);
        Predicate predSubquery = mock(Predicate.class);

        when(cb.equal(pathEstadoViaje, "EN_RUTA")).thenReturn(predEstado);
        when(cb.equal(pathIdChofer, "EMP-004")).thenReturn(predChofer);

        when(query.subquery(String.class)).thenReturn(subquery);
        when(subquery.from(RutaJpaEntity.class)).thenReturn(rutaRoot);
        when(rutaRoot.<String>get("idRuta")).thenReturn(pathSubIdRuta);
        when(rutaRoot.<String>get("idSucursalOrigen")).thenReturn(pathSubOrigen);
        when(cb.equal(pathSubOrigen, "SUC-LIM-01")).thenReturn(predSubquery);
        when(subquery.select(pathSubIdRuta)).thenReturn(subquery);
        when(subquery.where(predSubquery)).thenReturn(subquery);
        when(pathIdRuta.in(subquery)).thenReturn(predIn);

        when(cb.and(any(Predicate[].class))).thenReturn(predAnd);

        Specification<ProgramacionViajeJpaEntity> spec = ViajeFiltroEspecificacion.conFiltros("EN_RUTA", "SUC-LIM-01", "EMP-004");
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(predAnd);
        verify(cb).equal(pathEstadoViaje, "EN_RUTA");
        verify(cb).equal(pathIdChofer, "EMP-004");
        verify(pathIdRuta).in(subquery);
    }
}
