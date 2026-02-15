package de.dreistrom.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dreiStromOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DreiStrom API")
                        .version("0.1.0")
                        .description("German Three-Stream Tax & Business Management System API")
                        .contact(new Contact()
                                .name("DreiStrom Team")
                                .email("dev@dreistrom.de")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")));
    }
}
