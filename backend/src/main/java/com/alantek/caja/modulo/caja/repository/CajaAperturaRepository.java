package com.alantek.caja.modulo.caja.repository;

import com.alantek.caja.modulo.caja.entity.CajaApertura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CajaAperturaRepository extends JpaRepository<CajaApertura, Long> {

    Optional<CajaApertura> findFirstByCajeroIdAndFechaAndEstado(Long cajeroId, LocalDate fecha, String estado);

    List<CajaApertura> findByFechaOrderByOpenedAtAsc(LocalDate fecha);

    List<CajaApertura> findByCajeroIdOrderByOpenedAtDesc(Long cajeroId);

    List<CajaApertura> findByEstadoOrderByOpenedAtAsc(String estado);
}
