package com.alantek.caja.modulo.contabilidad.repository;

import com.alantek.caja.modulo.contabilidad.entity.AsientoContable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AsientoContableRepository extends JpaRepository<AsientoContable, Long> {

    List<AsientoContable> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);

    List<AsientoContable> findByPeriodoIdOrderByFechaAsc(Long periodoId);
}
