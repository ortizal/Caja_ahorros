package com.alantek.caja.modulo.caja.repository;

import com.alantek.caja.modulo.caja.entity.CajaMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CajaMovimientoRepository extends JpaRepository<CajaMovimiento, Long> {

    List<CajaMovimiento> findByCajaAperturaId(Long cajaAperturaId);

    Page<CajaMovimiento> findByCajaAperturaId(Long cajaAperturaId, Pageable pageable);
}
