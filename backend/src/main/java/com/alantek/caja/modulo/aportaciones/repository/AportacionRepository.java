package com.alantek.caja.modulo.aportaciones.repository;

import com.alantek.caja.modulo.aportaciones.entity.Aportacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AportacionRepository extends JpaRepository<Aportacion, Long> {

    Optional<Aportacion> findBySocioIdAndPeriodo(Long socioId, String periodo);

    boolean existsBySocioIdAndPeriodo(Long socioId, String periodo);

    List<Aportacion> findByPeriodoOrderBySocioId(String periodo);

    List<Aportacion> findBySocioIdOrderByPeriodoDesc(Long socioId);

    List<Aportacion> findAllByOrderByPeriodoDesc();
}
