package com.alantek.caja.modulo.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailConfiguracionRequest(
    @NotBlank String metodo,
    String smtpHost,
    Integer smtpPort,
    String smtpUsername,
    String smtpPassword,
    Boolean smtpUseTls,
    Boolean smtpUseSsl,
    String apiUrl,
    String apiKey,
    String apiProvider,
    @Email String fromEmail,
    String fromName,
    Boolean activo
) {}
