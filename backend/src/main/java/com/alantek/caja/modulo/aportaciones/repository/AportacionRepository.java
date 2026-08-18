package com.alantek.caja.modulo.aportaciones.repository;

import com.alantek.caja.modulo.aportaciones.entity.Aportacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AportacionRepository extends JpaRepository<Aportacion, Long> {

    Optional<Aportacion> findBySocioIdAndPeriodo(Long socioId, String periodo);

    boolean existsBySocioIdAndPeriodo(Long socioId, String periodo);

    List<Aportacion> findByPeriodoOrderBySocioId(String periodo);

    List<Aportacion> findBySocioIdOrderByPeriodoDesc(Long socioId);

    List<Aportacion> findAllByOrderByPeriodoDesc();

    Page<Aportacion> findByPeriodo(String periodo, Pageable pageable);

    Page<Aportacion> findBySocioId(Long socioId, Pageable pageable);

    Page<Aportacion> findByPeriodoAndSocioId(String periodo, Long socioId, Pageable pageable);
}
