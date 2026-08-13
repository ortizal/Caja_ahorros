package com.alantek.caja.modulo.creditos.repository;

import com.alantek.caja.modulo.creditos.entity.CreditoEstadoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditoEstadoHistorialRepository extends JpaRepository<CreditoEstadoHistorial, Long> {

    List<CreditoEstadoHistorial> findByCreditoIdOrderByChangedAtAsc(Long creditoId);
}
