package com.alantek.caja.modulo.email.dto;

public record EmailConfiguracionResponse(
    Long id,
    String metodo,
    String smtpHost,
    Integer smtpPort,
    String smtpUsername,
    Boolean smtpUseTls,
    Boolean smtpUseSsl,
    String apiUrl,
    String apiProvider,
    String fromEmail,
    String fromName,
    Boolean activo
) {}
