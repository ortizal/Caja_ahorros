package com.alantek.caja.modulo.caja.dto;

import java.math.BigDecimal;

public record CajaArqueoResponse(
        Long id,
        Long cajaAperturaId,
        BigDecimal saldoSistema,
        BigDecimal saldoFisico,
        BigDecimal diferencia,
        String observacion) {
}
