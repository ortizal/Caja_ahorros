package com.alantek.caja.modulo.bancos.repository;

import com.alantek.caja.modulo.bancos.entity.BancoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BancoMovimientoRepository extends JpaRepository<BancoMovimiento, Long> {

    List<BancoMovimiento> findByCuentaBancariaIdOrderByFechaAsc(Long cuentaBancariaId);
}
