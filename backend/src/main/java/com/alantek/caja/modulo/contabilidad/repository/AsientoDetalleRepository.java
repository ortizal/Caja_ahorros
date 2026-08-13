package com.alantek.caja.modulo.contabilidad.repository;

import com.alantek.caja.modulo.contabilidad.entity.AsientoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AsientoDetalleRepository extends JpaRepository<AsientoDetalle, Long> {

    List<AsientoDetalle> findByAsiento_Id(Long asientoId);

    @Query("SELECT COALESCE(SUM(d.debe), 0) FROM AsientoDetalle d WHERE d.asiento.id = :asientoId")
    BigDecimal sumDebe(@Param("asientoId") Long asientoId);

    @Query("SELECT COALESCE(SUM(d.haber), 0) FROM AsientoDetalle d WHERE d.asiento.id = :asientoId")
    BigDecimal sumHaber(@Param("asientoId") Long asientoId);
}
