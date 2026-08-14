package dev.murilodcosta.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.URL;

public record ShortenRequest(
    @NotBlank(message = "URL original é obrigatória")
    @URL(message = "URL original deve ser um endereço válido")
    String url,

    @Positive(message = "Tempo de expiração deve ser um valor positivo em minutos")
    Long expiresInMinutes
) {}
