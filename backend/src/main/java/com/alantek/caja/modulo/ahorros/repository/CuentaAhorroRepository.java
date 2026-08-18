package com.alantek.caja.modulo.ahorros.repository;

import com.alantek.caja.modulo.ahorros.entity.CuentaAhorro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CuentaAhorroRepository extends JpaRepository<CuentaAhorro, Long> {

    List<CuentaAhorro> findAllByOrderByFechaAperturaDesc();

    Page<CuentaAhorro> findAllByOrderByFechaAperturaDesc(Pageable pageable);

    List<CuentaAhorro> findBySocioIdOrderByFechaAperturaDesc(Long socioId);

    Page<CuentaAhorro> findBySocioIdOrderByFechaAperturaDesc(Long socioId, Pageable pageable);

    List<CuentaAhorro> findByEstadoOrderByFechaAperturaDesc(String estado);

    Optional<CuentaAhorro> findByNumeroCuenta(String numeroCuenta);

    @Query("SELECT COALESCE(MAX(c.id), 0) FROM CuentaAhorro c")
    Long maxId();
}
