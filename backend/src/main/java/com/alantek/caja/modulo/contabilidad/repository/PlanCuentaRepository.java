package com.alantek.caja.modulo.contabilidad.repository;

import com.alantek.caja.modulo.contabilidad.entity.PlanCuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanCuentaRepository extends JpaRepository<PlanCuenta, Long> {

    Optional<PlanCuenta> findByCodigo(String codigo);
}
