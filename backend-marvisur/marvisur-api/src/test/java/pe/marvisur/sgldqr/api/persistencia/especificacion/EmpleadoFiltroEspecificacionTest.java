package pe.marvisur.sgldqr.api.persistencia.especificacion;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Pruebas Unitarias — Constructor de Especificaciones de Empleados (RF-03)")
class EmpleadoFiltroEspecificacionTest {

    private Root<EmpleadoJpaEntity> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;

    private Path<Boolean> pathEstado;
    private Path<String> pathIdRol;
    private Path<String> pathIdSucursal;
    private Path<String> pathDni;
    private Path<String> pathNombres;
    private Path<String> pathApellidos;
    private Path<String> pathCorreo;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);

        pathEstado = mock(Path.class);
        pathIdRol = mock(Path.class);
        pathIdSucursal = mock(Path.class);
        pathDni = mock(Path.class);
        pathNombres = mock(Path.class);
        pathApellidos = mock(Path.class);
        pathCorreo = mock(Path.class);

        when(root.<Boolean>get("estado")).thenReturn(pathEstado);
        when(root.<String>get("idRol")).thenReturn(pathIdRol);
        when(root.<String>get("idSucursal")).thenReturn(pathIdSucursal);
        when(root.<String>get("dni")).thenReturn(pathDni);
        when(root.<String>get("nombres")).thenReturn(pathNombres);
        when(root.<String>get("apellidos")).thenReturn(pathApellidos);
        when(root.<String>get("correo")).thenReturn(pathCorreo);
    }

    @Test
    @DisplayName("Sin filtros: no agrega ninguna condición y retorna conjunción vacía")
    void sinFiltrosNoAgregaCondiciones() {
        Predicate conjuncion = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjuncion);

        Specification<EmpleadoJpaEntity> spec = EmpleadoFiltroEspecificacion.conFiltros(null, null, null, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(conjuncion);
        verify(cb).conjunction();
        verify(cb, never()).equal(any(), any());
        verify(cb, never()).or(any(Predicate[].class));
    }

    @Test
    @DisplayName("Filtros en blanco o vacíos se ignoran y retornan conjunción vacía")
    void filtrosEnBlancoSeIgnoran() {
        Predicate conjuncion = mock(Predicate.class);
        when(cb.conjunction()).thenReturn(conjuncion);

        Specification<EmpleadoJpaEntity> spec = EmpleadoFiltroEspecificacion.conFiltros(null, "   ", "", "  ");
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(conjuncion);
        verify(cb).conjunction();
        verify(cb, never()).equal(any(), any());
        verify(cb, never()).or(any(Predicate[].class));
    }

    @Test
    @DisplayName("Solo estado: agrega únicamente la condición equal para estado")
    void soloEstadoAgregaSoloSuCondicion() {
        Predicate predEstado = mock(Predicate.class);
        when(cb.equal(pathEstado, true)).thenReturn(predEstado);
        when(cb.and(any(Predicate[].class))).thenReturn(predEstado);

        Specification<EmpleadoJpaEntity> spec = EmpleadoFiltroEspecificacion.conFiltros(true, null, null, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).equal(pathEstado, true);
        verify(cb, never()).equal(eq(pathIdRol), any());
        verify(cb, never()).equal(eq(pathIdSucursal), any());
        verify(cb, never()).or(any(Predicate[].class));
    }

    @Test
    @DisplayName("Solo idRol: agrega únicamente la condición equal para idRol")
    void soloIdRolAgregaSoloSuCondicion() {
        Predicate predRol = mock(Predicate.class);
        when(cb.equal(pathIdRol, "ROL-ADMIN")).thenReturn(predRol);
        when(cb.and(any(Predicate[].class))).thenReturn(predRol);

        Specification<EmpleadoJpaEntity> spec = EmpleadoFiltroEspecificacion.conFiltros(null, "ROL-ADMIN", null, null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).equal(pathIdRol, "ROL-ADMIN");
        verify(cb, never()).equal(eq(pathEstado), any());
        verify(cb, never()).equal(eq(pathIdSucursal), any());
        verify(cb, never()).or(any(Predicate[].class));
    }

    @Test
    @DisplayName("Solo idSucursal: agrega únicamente la condición equal para idSucursal")
    void soloIdSucursalAgregaSoloSuCondicion() {
        Predicate predSucursal = mock(Predicate.class);
        when(cb.equal(pathIdSucursal, "SUC-01")).thenReturn(predSucursal);
        when(cb.and(any(Predicate[].class))).thenReturn(predSucursal);

        Specification<EmpleadoJpaEntity> spec = EmpleadoFiltroEspecificacion.conFiltros(null, null, "SUC-01", null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).equal(pathIdSucursal, "SUC-01");
        verify(cb, never()).equal(eq(pathEstado), any());
        verify(cb, never()).equal(eq(pathIdRol), any());
        verify(cb, never()).or(any(Predicate[].class));
    }

    @Test
    @DisplayName("Solo q: construye búsqueda disyuntiva insensible a mayúsculas sobre DNI, nombres, apellidos y correo")
    @SuppressWarnings("unchecked")
    void soloQConstruyeDisyuncionLikeLower() {
        Expression<String> lowerDni = mock(Expression.class);
        Expression<String> lowerNombres = mock(Expression.class);
        Expression<String> lowerApellidos = mock(Expression.class);
        Expression<String> lowerCorreo = mock(Expression.class);

        when(cb.lower(pathDni)).thenReturn(lowerDni);
        when(cb.lower(pathNombres)).thenReturn(lowerNombres);
        when(cb.lower(pathApellidos)).thenReturn(lowerApellidos);
        when(cb.lower(pathCorreo)).thenReturn(lowerCorreo);

        Predicate pDni = mock(Predicate.class);
        Predicate pNombres = mock(Predicate.class);
        Predicate pApellidos = mock(Predicate.class);
        Predicate pCorreo = mock(Predicate.class);

        String patronEsperado = "%carlos%";
        when(cb.like(lowerDni, patronEsperado)).thenReturn(pDni);
        when(cb.like(lowerNombres, patronEsperado)).thenReturn(pNombres);
        when(cb.like(lowerApellidos, patronEsperado)).thenReturn(pApellidos);
        when(cb.like(lowerCorreo, patronEsperado)).thenReturn(pCorreo);

        Predicate orPredicate = mock(Predicate.class);
        when(cb.or(pDni, pNombres, pApellidos, pCorreo)).thenReturn(orPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(orPredicate);

        Specification<EmpleadoJpaEntity> spec = EmpleadoFiltroEspecificacion.conFiltros(null, null, null, " Carlos ");
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isNotNull();
        verify(cb).or(pDni, pNombres, pApellidos, pCorreo);
        verify(cb, never()).equal(eq(pathEstado), any());
        verify(cb, never()).equal(eq(pathIdRol), any());
        verify(cb, never()).equal(eq(pathIdSucursal), any());
    }

    @Test
    @DisplayName("Combinación de filtros: combina predicados presentes con and")
    void combinacionFiltrosCombinaConAnd() {
        Predicate predEstado = mock(Predicate.class);
        Predicate predSucursal = mock(Predicate.class);
        Predicate predAnd = mock(Predicate.class);

        when(cb.equal(pathEstado, false)).thenReturn(predEstado);
        when(cb.equal(pathIdSucursal, "SUC-02")).thenReturn(predSucursal);
        when(cb.and(any(Predicate[].class))).thenReturn(predAnd);

        Specification<EmpleadoJpaEntity> spec = EmpleadoFiltroEspecificacion.conFiltros(false, null, "SUC-02", null);
        Predicate resultado = spec.toPredicate(root, query, cb);

        assertThat(resultado).isSameAs(predAnd);
        verify(cb).equal(pathEstado, false);
        verify(cb).equal(pathIdSucursal, "SUC-02");
        verify(cb, never()).equal(eq(pathIdRol), any());
        verify(cb, never()).or(any(Predicate[].class));
    }
}
