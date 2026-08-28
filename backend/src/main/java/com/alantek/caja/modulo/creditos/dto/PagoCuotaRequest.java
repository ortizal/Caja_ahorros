package com.alantek.caja.modulo.creditos.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record PagoCuotaRequest(
        Long cuotaId,
        @DecimalMin(value = "0.0", message = "El monto de capital no puede ser negativo") BigDecimal montoCapital,
        @DecimalMin(value = "0.0", message = "El monto de interes no puede ser negativo") BigDecimal montoInteres,
        @DecimalMin(value = "0.0", message = "El monto de mora no puede ser negativo") BigDecimal montoMora,
        @DecimalMin(value = "0.0", message = "El abono a capital no puede ser negativo") BigDecimal montoAbonoCapital,
        String tipo,
        String descripcion) {
}
