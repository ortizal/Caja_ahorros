package com.alantek.caja.modulo.tesoreria.dto;

import java.math.BigDecimal;

public record PresupuestoPartidaResponse(
        Long id,
        Integer anio,
        String concepto,
        Long cuentaContableId,
        String cuentaContableCodigo,
        BigDecimal montoPresupuestado,
        BigDecimal montoEjecutado,
        BigDecimal porcentajeEjecucion) {
}
