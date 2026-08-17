package com.alantek.caja.modulo.contabilidad.repository;

import com.alantek.caja.modulo.contabilidad.entity.ReglaContable;
import com.alantek.caja.modulo.contabilidad.repository.ReglaContableRepository;
import com.alantek.caja.modulo.contabilidad.repository.PlanCuentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReglaContableRepositoryTest {

    private static final LocalDate FECHA = LocalDate.of(2030, 1, 15);

    @Autowired
    private ReglaContableRepository reglaContableRepository;

    @Autowired
    private PlanCuentaRepository planCuentaRepository;

    private final Map<String, List<String>> matriz = Map.of(
            "APORTACION", List.of("1.1.01", "2.1.01"),
            "DEPOSITO_AHORRO", List.of("1.1.01", "2.1.02"),
            "RETIRO_AHORRO", List.of("2.1.02", "1.1.01"),
            "DESEMBOLSO_CREDITO", List.of("1.3.01", "1.1.01"),
            "PAGO_CAPITAL", List.of("1.1.01", "1.3.01"),
            "PAGO_INTERES", List.of("1.1.01", "4.1.01"),
            "PAGO_MORA", List.of("1.1.01", "4.1.02"),
            "GASTO_PAGADO", List.of("5.1.01", "1.1.01"),
            "COBRO_CUENTA", List.of("1.1.01", "1.4.01"));

    @Test
    void todasLasOperacionesDeLaMatrizTienenReglaVigente() {
        for (String operacion : matriz.keySet()) {
            Optional<ReglaContable> regla = reglaContableRepository.findVigente(operacion, FECHA);
            assertThat(regla).as("Regla vigente para %s", operacion).isPresent();
            assertThat(regla.get().getActivo()).isTrue();
            assertThat(regla.get().getVigenteDesde()).isBeforeOrEqualTo(FECHA);
        }
    }

    @Test
    void reglasApuntanACuentasDeLaMatriz() {
        for (Map.Entry<String, List<String>> entrada : matriz.entrySet()) {
            ReglaContable regla = reglaContableRepository.findVigente(entrada.getKey(), FECHA).orElseThrow();

            String codigoDebe = planCuentaRepository.findById(regla.getCuentaDebeId()).orElseThrow().getCodigo();
            String codigoHaber = planCuentaRepository.findById(regla.getCuentaHaberId()).orElseThrow().getCodigo();

            assertThat(codigoDebe).as("Cuenta Debe de %s", entrada.getKey()).isEqualTo(entrada.getValue().get(0));
            assertThat(codigoHaber).as("Cuenta Haber de %s", entrada.getKey()).isEqualTo(entrada.getValue().get(1));
        }
    }

    @Test
    void operacionDesconocidaNoTieneReglaVigente() {
        assertThat(reglaContableRepository.findVigente("OPERACION_INEXISTENTE", FECHA)).isEmpty();
    }

    @Test
    void operacionesUnicasPorNombre() {
        List<ReglaContable> reglas = reglaContableRepository.findAll();
        long unicas = reglas.stream().map(ReglaContable::getOperacion).distinct().count();
        assertThat(unicas).isEqualTo(reglas.size());
    }
}
