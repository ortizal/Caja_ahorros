package com.alantek.caja.modulo.tesoreria.repository;

import com.alantek.caja.modulo.tesoreria.entity.CuentaPorCobrar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuentaPorCobrarRepository extends JpaRepository<CuentaPorCobrar, Long> {

    List<CuentaPorCobrar> findAllByOrderByCreatedAtDesc();

    List<CuentaPorCobrar> findByEstadoOrderByCreatedAtDesc(String estado);
}
