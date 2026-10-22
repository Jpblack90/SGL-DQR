package pe.marvisur.sgldqr.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Genera y exporta especificación OpenAPI para endpoints públicos")
    void exportarOpenApiPublico() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs/publico"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(json).contains("/api/v1/publico/rastreo");
        assertThat(json).doesNotContain("/api/v1/entregas/validar");

        Object jsonObject = objectMapper.readValue(json, Object.class);
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

        Path docsDir = Paths.get("../../docs").toAbsolutePath().normalize();
        if (!Files.exists(docsDir)) {
            docsDir = Paths.get("docs").toAbsolutePath().normalize();
        }
        Path targetFile = docsDir.resolve("openapi-publico.json");
        Files.writeString(targetFile, prettyJson);

        assertThat(Files.exists(targetFile)).isTrue();
    }

    @Test
    @DisplayName("Genera y exporta especificación OpenAPI para endpoints internos")
    void exportarOpenApiInterno() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs/interno"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(json).contains("/api/v1/encomiendas");
        assertThat(json).doesNotContain("/api/v1/publico/rastreo");

        Object jsonObject = objectMapper.readValue(json, Object.class);
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

        Path docsDir = Paths.get("../../docs").toAbsolutePath().normalize();
        if (!Files.exists(docsDir)) {
            docsDir = Paths.get("docs").toAbsolutePath().normalize();
        }
        Path targetFile = docsDir.resolve("openapi-interno.json");
        Files.writeString(targetFile, prettyJson);

        assertThat(Files.exists(targetFile)).isTrue();
    }
}
