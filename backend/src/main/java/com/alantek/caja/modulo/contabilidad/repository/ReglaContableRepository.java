package com.alantek.caja.modulo.contabilidad.repository;

import com.alantek.caja.modulo.contabilidad.entity.ReglaContable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ReglaContableRepository extends JpaRepository<ReglaContable, Long> {

    @Query("""
            SELECT r FROM ReglaContable r
            WHERE r.operacion = :operacion
              AND r.activo = true
              AND r.vigenteDesde <= :fecha
              AND (r.vigenteHasta IS NULL OR r.vigenteHasta >= :fecha)
            ORDER BY r.vigenteDesde DESC
            """)
    Optional<ReglaContable> findVigente(@Param("operacion") String operacion, @Param("fecha") LocalDate fecha);

    Optional<ReglaContable> findByOperacion(String operacion);
}
