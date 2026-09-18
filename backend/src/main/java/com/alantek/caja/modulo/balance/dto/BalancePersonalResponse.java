package com.alantek.caja.modulo.balance.dto;

import com.alantek.caja.modulo.ahorros.dto.CuentaAhorroResponse;
import com.alantek.caja.modulo.aportaciones.dto.AportacionResponse;
import com.alantek.caja.modulo.creditos.dto.CreditoResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BalancePersonalResponse(
        Long socioId,
        String codigo,
        String nombreCompleto,
        String identificacion,
        String estado,
        LocalDate fechaIngreso,
        BigDecimal totalAportado,
        BigDecimal aportePendienteActual,
        List<AportacionResponse> aportaciones,
        BigDecimal saldoTotalAhorros,
        List<CuentaAhorroResponse> cuentasAhorro,
        BigDecimal saldoTotalCreditos,
        int creditosVigentes,
        int cuotasVencidas,
        BigDecimal moraTotal,
        List<CreditoResponse> creditos,
        BigDecimal posicionNeta) {
}