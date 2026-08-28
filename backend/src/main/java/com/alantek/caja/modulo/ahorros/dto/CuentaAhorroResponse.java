package com.alantek.caja.modulo.ahorros.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaAhorroResponse(
        Long id,
        Long socioId,
        String socioCodigo,
        String socioNombre,
        Long productoId,
        String nombreProducto,
        String numeroCuenta,
        String tipoAhorro,
        BigDecimal saldo,
        String estado,
        LocalDate fechaApertura,
        LocalDate fechaCierre) {
}
