package dev.murilodcosta.url_shortener.config;

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
        return new OpenAPI()
                .info(new Info()
                        .title("High-Performance URL Shortener API")
                        .version("1.0.0")
                        .description("Production-grade distributed URL Shortener API built with Spring Boot, Java 21 Virtual Threads, Redis Cache-Aside, Token Bucket Rate Limiting, and Prometheus Metrics.")
                        .contact(new Contact()
                                .name("Murilo D. Costa")
                                .url("https://github.com/murilodcosta/url-shortener"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server")
                ));
    }
}
