package com.alantek.caja.modulo.creditos.repository;

import com.alantek.caja.modulo.creditos.entity.ProductoCredito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoCreditoRepository extends JpaRepository<ProductoCredito, Long> {

    List<ProductoCredito> findAllByOrderByActivoDescIdDesc();

    List<ProductoCredito> findByActivoTrueOrderByIdDesc();

    Page<ProductoCredito> findAllByOrderByActivoDescIdDesc(Pageable pageable);

    Page<ProductoCredito> findByActivoTrueOrderByIdDesc(Pageable pageable);

    @Query("select coalesce(max(p.id), 0) from ProductoCredito p")
    long maxId();
}
