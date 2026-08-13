package com.alantek.caja.modulo.creditos.repository;

import com.alantek.caja.modulo.creditos.entity.SolicitudCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudCreditoRepository extends JpaRepository<SolicitudCredito, Long> {

    List<SolicitudCredito> findAllByOrderByCreatedAtDesc();

    List<SolicitudCredito> findByEstadoOrderByCreatedAtDesc(String estado);

    List<SolicitudCredito> findBySocioIdOrderByCreatedAtDesc(Long socioId);

    boolean existsBySocioIdAndEstadoIn(Long socioId, List<String> estados);
}
