package com.alantek.caja.modulo.creditos.repository;

import com.alantek.caja.modulo.creditos.entity.ProductoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoCreditoRepository extends JpaRepository<ProductoCredito, Long> {

    List<ProductoCredito> findAllByOrderByActivoDescIdDesc();

    List<ProductoCredito> findByActivoTrueOrderByIdDesc();

    @Query("select coalesce(max(p.id), 0) from ProductoCredito p")
    long maxId();
}
