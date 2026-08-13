package com.alantek.caja.modulo.creditos.dto;

import java.math.BigDecimal;

public record MoraResponse(
        int cuotasMarcadas,
        BigDecimal moraTotal,
        int creditosEnMora) {
}
