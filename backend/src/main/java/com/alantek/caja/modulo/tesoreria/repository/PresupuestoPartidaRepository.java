package com.alantek.caja.modulo.tesoreria.repository;

import com.alantek.caja.modulo.tesoreria.entity.PresupuestoPartida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PresupuestoPartidaRepository extends JpaRepository<PresupuestoPartida, Long> {

    List<PresupuestoPartida> findByAnioOrderByConceptoAsc(Integer anio);

    Optional<PresupuestoPartida> findByAnioAndConcepto(Integer anio, String concepto);
}
