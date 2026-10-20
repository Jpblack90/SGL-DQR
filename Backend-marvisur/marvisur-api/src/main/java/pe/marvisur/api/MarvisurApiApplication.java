package pe.marvisur.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API REST SGL-DQR — Expreso Marvisur.
 *
 * <p>Arquitectura hexagonal: este módulo es el adaptador de infraestructura.
 * El dominio de negocio reside en {@code marvisur-core}, sin dependencias de Spring.</p>
 */
@SpringBootApplication
public class MarvisurApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarvisurApiApplication.class, args);
    }
}
