package com.alantek.caja.modulo.creditos.service;

import com.alantek.caja.modulo.creditos.dto.SimulacionCreditoResponse;
import com.alantek.caja.modulo.creditos.dto.SimulacionCreditoResponse.CuotaSimulada;
import com.alantek.caja.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AmortizacionService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal DOCE = new BigDecimal("12");
    private static final int PRECISION = 10;

    public SimulacionCreditoResponse simular(BigDecimal monto, BigDecimal tasaAnualPct, int plazoMeses,
                                             String sistemaAmortizacion) {
        return simular(monto, tasaAnualPct, plazoMeses, sistemaAmortizacion, LocalDate.now());
    }

    public SimulacionCreditoResponse simular(BigDecimal monto, BigDecimal tasaAnualPct, int plazoMeses,
                                             String sistemaAmortizacion, LocalDate fechaPrimeraCuota) {
        List<CuotaSimulada> cuotas = generarTabla(monto, tasaAnualPct, plazoMeses, sistemaAmortizacion,
                fechaPrimeraCuota);
        BigDecimal totalInteres = cuotas.stream()
                .map(CuotaSimulada::interes)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal cuotaReferencia = cuotas.isEmpty() ? BigDecimal.ZERO
                : cuotas.get(0).cuota();
        return new SimulacionCreditoResponse(cuotaReferencia, totalInteres,
                monto.add(totalInteres), sistemaAmortizacion.toUpperCase(), cuotas);
    }

    public List<CuotaSimulada> generarTabla(BigDecimal monto, BigDecimal tasaAnualPct, int plazoMeses,
                                            String sistemaAmortizacion, LocalDate fechaPrimeraCuota) {
        if (monto == null || monto.signum() <= 0) {
            throw new BusinessException("El monto debe ser mayor a cero");
        }
        if (plazoMeses <= 0) {
            throw new BusinessException("El plazo debe ser al menos 1 mes");
        }
        String sistema = sistemaAmortizacion == null ? "FRANCES" : sistemaAmortizacion.toUpperCase();
        if (!List.of("FRANCES", "ALEMAN", "AMERICANO").contains(sistema)) {
            throw new BusinessException("Sistema de amortizacion no soportado: " + sistema);
        }
        LocalDate primera = fechaPrimeraCuota == null ? LocalDate.now() : fechaPrimeraCuota;
        BigDecimal tasaMensual = tasaAnualPct.divide(CIEN, PRECISION, RoundingMode.HALF_UP)
                .divide(DOCE, PRECISION, RoundingMode.HALF_UP);
        BigDecimal saldo = monto.setScale(2, RoundingMode.HALF_UP);
        BigDecimal capitalConstante = monto.divide(new BigDecimal(plazoMeses), PRECISION, RoundingMode.HALF_UP);
        BigDecimal cuotaFrancesa = cuotaFrancesa(monto, tasaMensual, plazoMeses);

        List<CuotaSimulada> cuotas = new ArrayList<>(plazoMeses);
        for (int k = 1; k <= plazoMeses; k++) {
            LocalDate fecha = primera.plusMonths(k - 1);
            BigDecimal interes = saldo.multiply(tasaMensual).setScale(2, RoundingMode.HALF_UP);
            BigDecimal capital;
            BigDecimal cuota;
            switch (sistema) {
                case "FRANCES" -> {
                    cuota = cuotaFrancesa.setScale(2, RoundingMode.HALF_UP);
                    capital = cuota.subtract(interes);
                    if (k == plazoMeses || capital.compareTo(saldo) > 0) {
                        capital = saldo;
                        cuota = capital.add(interes);
                    }
                }
                case "ALEMAN" -> {
                    capital = capitalConstante.setScale(2, RoundingMode.HALF_UP);
                    if (k == plazoMeses) {
                        capital = saldo;
                    }
                    cuota = capital.add(interes);
                }
                default -> {
                    if (k < plazoMeses) {
                        capital = BigDecimal.ZERO;
                        cuota = interes;
                    } else {
                        capital = saldo;
                        cuota = capital.add(interes);
                    }
                }
            }
            saldo = saldo.subtract(capital).setScale(2, RoundingMode.HALF_UP);
            cuotas.add(new CuotaSimulada(k, fecha, capital, interes, cuota, saldo));
        }
        return cuotas;
    }

    private BigDecimal cuotaFrancesa(BigDecimal monto, BigDecimal tasaMensual, int plazoMeses) {
        if (tasaMensual.signum() == 0) {
            return monto.divide(new BigDecimal(plazoMeses), PRECISION, RoundingMode.HALF_UP);
        }
        BigDecimal factor = BigDecimal.ONE.add(tasaMensual).pow(plazoMeses);
        BigDecimal numerador = monto.multiply(tasaMensual).multiply(factor);
        BigDecimal denominador = factor.subtract(BigDecimal.ONE);
        return numerador.divide(denominador, PRECISION, RoundingMode.HALF_UP);
    }
}
