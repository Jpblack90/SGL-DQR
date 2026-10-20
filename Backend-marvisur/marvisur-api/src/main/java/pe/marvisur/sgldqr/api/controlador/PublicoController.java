package pe.marvisur.sgldqr.api.controlador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador de endpoints públicos sin requerimiento de autenticación.
 */
@RestController
@RequestMapping("/api/v1/publico")
@Tag(name = "Endpoints Públicos", description = "Servicios abiertos sin token de autenticación")
public class PublicoController {

    @GetMapping("/ping")
    @Operation(summary = "Comprobación de conectividad pública", description = "Verifica que los endpoints /api/v1/publico/** no exigen token JWT.")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of(
                "estado", "OK",
                "mensaje", "Endpoint público accesible sin token"
        ));
    }
}
