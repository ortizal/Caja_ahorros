package com.alantek.caja.modulo.ahorros.repository;

import com.alantek.caja.modulo.ahorros.entity.ProductoAhorro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoAhorroRepository extends JpaRepository<ProductoAhorro, Long> {

    List<ProductoAhorro> findAllByOrderByActivoDescIdDesc();

    List<ProductoAhorro> findByActivoTrue();
}
