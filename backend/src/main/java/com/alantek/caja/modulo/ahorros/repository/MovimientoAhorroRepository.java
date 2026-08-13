package com.alantek.caja.modulo.ahorros.repository;

import com.alantek.caja.modulo.ahorros.entity.MovimientoAhorro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoAhorroRepository extends JpaRepository<MovimientoAhorro, Long> {

    List<MovimientoAhorro> findByCuentaIdOrderByCreatedAtAsc(Long cuentaId);

    List<MovimientoAhorro> findByTipoAndEstado(String tipo, String estado);

    boolean existsByTipoAndEstadoAndPeriodo(String tipo, String estado, String periodo);
}
