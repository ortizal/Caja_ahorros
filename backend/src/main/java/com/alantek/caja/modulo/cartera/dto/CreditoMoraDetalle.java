package com.alantek.caja.modulo.cartera.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreditoMoraDetalle(
        Long creditoId,
        String nombreProducto,
        BigDecimal montoDesembolsado,
        BigDecimal saldoCapital,
        BigDecimal tasaInteres,
        BigDecimal tasaMora,
        Integer plazoMeses,
        LocalDate fechaDesembolso,
        String estado,
        List<CuotaMoraDetalle> cuotasConMora,
        BigDecimal moraTotalCredito) {
}
