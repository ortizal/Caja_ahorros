package com.alantek.caja.modulo.tesoreria.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaPorPagarResponse(
        Long id,
        String proveedor,
        String concepto,
        BigDecimal monto,
        Long cuentaContableId,
        String cuentaContableCodigo,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        String estado,
        Long comprobanteId,
        Long cajaMovimientoId) {
}
