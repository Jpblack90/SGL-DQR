package pe.marvisur.sgldqr.api.seguridad;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de prueba exclusivo para verificar la evaluación de @PreAuthorize y RBAC.
 */
@Hidden
@RestController
@RequestMapping("/api/v1/test-rbac")
public class ControladorPruebaRbac {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<String> soloAdministrador() {
        return ResponseEntity.ok("Acceso concedido a Administrador");
    }

    @GetMapping("/almacenero")
    @PreAuthorize("hasRole('ALMACENERO')")
    public ResponseEntity<String> soloAlmacenero() {
        return ResponseEntity.ok("Acceso concedido a Almacenero");
    }
}
