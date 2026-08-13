package com.alantek.caja.modulo.aportaciones.repository;

import com.alantek.caja.modulo.aportaciones.entity.AportacionPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AportacionPagoRepository extends JpaRepository<AportacionPago, Long> {

    List<AportacionPago> findByAportacionIdOrderByPagadoAtAsc(Long aportacionId);
}
