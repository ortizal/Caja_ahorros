package com.alantek.caja.modulo.tesoreria.repository;

import com.alantek.caja.modulo.tesoreria.entity.CuentaPorPagar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuentaPorPagarRepository extends JpaRepository<CuentaPorPagar, Long> {

    List<CuentaPorPagar> findAllByOrderByCreatedAtDesc();

    List<CuentaPorPagar> findByEstadoOrderByCreatedAtDesc(String estado);
}
