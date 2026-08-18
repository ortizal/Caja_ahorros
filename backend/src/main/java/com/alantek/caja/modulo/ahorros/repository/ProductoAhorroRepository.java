package com.alantek.caja.modulo.ahorros.repository;

import com.alantek.caja.modulo.ahorros.entity.ProductoAhorro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoAhorroRepository extends JpaRepository<ProductoAhorro, Long> {

    List<ProductoAhorro> findAllByOrderByActivoDescIdDesc();

    Page<ProductoAhorro> findAllByOrderByActivoDescIdDesc(Pageable pageable);

    List<ProductoAhorro> findByActivoTrue();
}
