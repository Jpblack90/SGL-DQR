package pe.marvisur.sgldqr.puertos;

import java.time.LocalDateTime;

/**
 * Puerto para desacoplar el tiempo del sistema y facilitar pruebas unitarias deterministas.
 */
public interface RelojPort {

    LocalDateTime ahora();
}
