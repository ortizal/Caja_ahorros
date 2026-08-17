package com.alantek.caja.modulo.tesoreria.repository;

import com.alantek.caja.modulo.tesoreria.entity.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    List<Gasto> findAllByOrderByCreatedAtDesc();

    List<Gasto> findByEstadoOrderByCreatedAtDesc(String estado);
}
