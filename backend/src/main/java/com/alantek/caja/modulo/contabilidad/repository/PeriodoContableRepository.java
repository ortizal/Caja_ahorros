package com.alantek.caja.modulo.contabilidad.repository;

import com.alantek.caja.modulo.contabilidad.entity.PeriodoContable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PeriodoContableRepository extends JpaRepository<PeriodoContable, Long> {

    Optional<PeriodoContable> findByAnioAndMes(Integer anio, Integer mes);
}
