package com.pascualbravo.calendario.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI calendarioApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Calendario API — Colombia")
                        .version("1.0.0")
                        .description(
                                "API RESTful que consume la API de Festivos (MS1) y genera un calendario anual " +
                                "clasificando cada día como laboral, fin de semana o festivo. " +
                                "Los datos se persisten en PostgreSQL."
                        )
                        .contact(new Contact().name("Pascual Bravo — Programación Avanzada"))
                )
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Servidor de desarrollo")
                ));
    }
}
