package com.alantek.caja.modulo.bancos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BancoMovimientoResponse(
        Long id,
        Long cuentaBancariaId,
        String tipo,
        BigDecimal monto,
        LocalDate fecha,
        Long comprobanteId,
        Boolean conciliado,
        BigDecimal saldoContable) {
}
