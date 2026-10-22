package pe.marvisur.sgldqr.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API REST SGL-DQR — Expreso Marvisur.
 *
 * <p>Módulo adaptador de infraestructura (arquitectura hexagonal).
 * El dominio de negocio reside en marvisur-core, completamente desacoplado de Spring.</p>
 */
@SpringBootApplication
public class MarvisurApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarvisurApiApplication.class, args);
    }
}
