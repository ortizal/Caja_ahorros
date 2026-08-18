package com.alantek.caja.modulo.creditos.repository;

import com.alantek.caja.modulo.creditos.entity.CuotaCredito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CuotaCreditoRepository extends JpaRepository<CuotaCredito, Long> {

    List<CuotaCredito> findByCreditoIdOrderByNumeroCuotaAsc(Long creditoId);

    List<CuotaCredito> findByCreditoIdAndEstado(Long creditoId, String estado);

    List<CuotaCredito> findByEstadoAndFechaVencimientoBefore(String estado, LocalDate fecha);

    List<CuotaCredito> findByEstadoInOrderByFechaVencimientoAsc(List<String> estados);

    Page<CuotaCredito> findByCreditoId(Long creditoId, Pageable pageable);

    Page<CuotaCredito> findByCreditoIdAndEstadoNot(Long creditoId, String estado, Pageable pageable);

    long countByCreditoIdAndEstado(Long creditoId, String estado);
}
