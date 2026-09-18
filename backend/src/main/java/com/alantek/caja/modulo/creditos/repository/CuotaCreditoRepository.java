package com.alantek.caja.modulo.creditos.repository;

import com.alantek.caja.modulo.creditos.entity.CuotaCredito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
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

    long countByEstado(String estado);

    @Query("SELECT COALESCE(SUM(c.mora), 0) FROM CuotaCredito c WHERE c.estado = 'VENCIDA'")
    BigDecimal sumMoraVencida();

    @Query("SELECT COALESCE(SUM(c.mora), 0) FROM CuotaCredito c WHERE c.creditoId IN :creditoIds AND c.estado = 'VENCIDA'")
    BigDecimal sumMoraVencidaByCreditoIds(@org.springframework.data.repository.query.Param("creditoIds") java.util.List<Long> creditoIds);

    long countByCreditoIdInAndEstado(java.util.List<Long> creditoIds, String estado);
}
