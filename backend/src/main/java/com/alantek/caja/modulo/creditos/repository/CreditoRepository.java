package com.alantek.caja.modulo.creditos.repository;

import com.alantek.caja.modulo.creditos.entity.Credito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface CreditoRepository extends JpaRepository<Credito, Long> {

    List<Credito> findAllByOrderByCreatedAtDesc();

    List<Credito> findBySocioIdOrderByCreatedAtDesc(Long socioId);

    List<Credito> findByEstadoOrderByCreatedAtDesc(String estado);

    boolean existsBySocioIdAndEstadoIn(Long socioId, List<String> estados);

    @Query("SELECT COALESCE(SUM(c.saldoCapital), 0) FROM Credito c WHERE c.estado IN ('VIGENTE', 'EN_MORA')")
    BigDecimal sumSaldoVigente();

    @Query("SELECT COUNT(c) FROM Credito c WHERE c.estado IN ('VIGENTE', 'EN_MORA')")
    long countVigentes();
}
