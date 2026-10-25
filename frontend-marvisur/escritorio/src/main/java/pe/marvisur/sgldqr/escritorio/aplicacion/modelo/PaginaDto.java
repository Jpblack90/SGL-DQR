package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;

/**
 * Contenedor genérico para respuestas paginadas de Spring Data (Page&lt;T&gt;).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaginaDto<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        boolean first,
        boolean last,
        boolean empty
) {
    public List<T> content() {
        return content != null ? Collections.unmodifiableList(content) : Collections.emptyList();
    }

    public static <T> PaginaDto<T> vacia() {
        return new PaginaDto<>(Collections.emptyList(), 0L, 0, 0, 20, true, true, true);
    }
}
