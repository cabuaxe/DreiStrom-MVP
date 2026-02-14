package de.dreistrom.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSpecExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void specIsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("DreiStrom API"))
                .andExpect(jsonPath("$.info.version").value("0.1.0"));
    }

    @Test
    void exportSpecToFile() throws Exception {
        String spec = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Path outputDir = Path.of("target");
        Files.createDirectories(outputDir);
        Path specFile = outputDir.resolve("openapi.json");
        Files.writeString(specFile, spec);

        // Verify file was written
        org.assertj.core.api.Assertions.assertThat(specFile).exists();
        org.assertj.core.api.Assertions.assertThat(Files.readString(specFile))
                .contains("DreiStrom API");
    }
}
