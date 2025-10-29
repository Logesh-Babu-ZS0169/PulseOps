package com.intics.metrics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8181");
        localServer.setDescription("Development Server");

        Contact contact = new Contact();
        contact.setEmail("logesh.babu@intics.ai");
        contact.setName("Intics Team");

        Info info = new Info()
                .title("Inbound Ingestion Metrics Dashboard API")
                .version("1.0.0")
                .description("REST API for real-time monitoring of inbound document ingestion metrics. " +
                        "Tracks MEDICAL_COMMERCIAL and MEDICAL_GBD document types with hourly granularity. " +
                        "Provides daily/hourly metrics, failure recovery reports, and traffic predictions.")
                .contact(contact)
                .license(new License()
                        .name("Proprietary")
                        .url("https://www.intics.ai"));

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
