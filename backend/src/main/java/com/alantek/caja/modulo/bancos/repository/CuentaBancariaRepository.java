package com.alantek.caja.modulo.bancos.repository;

import com.alantek.caja.modulo.bancos.entity.CuentaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, Long> {

    @Query("SELECT COALESCE(SUM(c.saldoContable), 0) FROM CuentaBancaria c")
    BigDecimal sumSaldoContable();
}
