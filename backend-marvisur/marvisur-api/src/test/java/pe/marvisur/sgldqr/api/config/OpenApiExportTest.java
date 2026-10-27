package pe.marvisur.sgldqr.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OpenApiExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Genera y exporta especificación OpenAPI para endpoints públicos")
    void exportarOpenApiPublico() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs/publico"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] rawBytes = result.getResponse().getContentAsByteArray();
        assertThat(rawBytes).isNotEmpty();

        // Guardar bytes tal cual en target/openapi/ (carpeta no versionada para mantener el árbol git limpio)
        Path targetDir = Paths.get("target/openapi");
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve("openapi-publico.json");
        Files.write(targetFile, rawBytes);
        assertThat(Files.exists(targetFile)).isTrue();

        // Verificaciones sobre el contenido y protección contra regresión de codificación UTF-8
        String json = new String(rawBytes, StandardCharsets.UTF_8);
        assertThat(json).contains("/api/v1/publico/rastreo");
        assertThat(json).doesNotContain("/api/v1/entregas/validar");

        assertThat(json).doesNotContain("Ã");
        assertThat(json).doesNotContain("\u00C3");
        assertThat(json).doesNotContain("├");
        assertThat(json).doesNotContain("\u251C");
        assertThat(json).contains("Público");
    }

    @Test
    @DisplayName("Genera y exporta especificación OpenAPI para endpoints internos")
    void exportarOpenApiInterno() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs/interno"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] rawBytes = result.getResponse().getContentAsByteArray();
        assertThat(rawBytes).isNotEmpty();

        // Guardar bytes tal cual en target/openapi/ (carpeta no versionada para mantener el árbol git limpio)
        Path targetDir = Paths.get("target/openapi");
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve("openapi-interno.json");
        Files.write(targetFile, rawBytes);
        assertThat(Files.exists(targetFile)).isTrue();

        // Verificaciones sobre el contenido y protección contra regresión de codificación UTF-8
        String json = new String(rawBytes, StandardCharsets.UTF_8);
        assertThat(json).contains("/api/v1/encomiendas");
        assertThat(json).doesNotContain("/api/v1/publico/rastreo");
        assertThat(json).doesNotContain("/test-rbac");

        assertThat(json).doesNotContain("Ã");
        assertThat(json).doesNotContain("\u00C3");
        assertThat(json).doesNotContain("├");
        assertThat(json).doesNotContain("\u251C");
        assertThat(json).contains("Autenticación");
    }
}
