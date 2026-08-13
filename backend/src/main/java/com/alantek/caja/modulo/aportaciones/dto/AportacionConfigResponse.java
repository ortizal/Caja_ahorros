package com.alantek.caja.modulo.aportaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AportacionConfigResponse(
        Long id,
        String tipo,
        String modoCalculo,
        BigDecimal valor,
        String periodicidad,
        BigDecimal montoMinimo,
        BigDecimal montoMaximo,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta) {
}
