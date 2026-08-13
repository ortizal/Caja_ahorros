package com.alantek.caja.modulo.aportaciones.repository;

import com.alantek.caja.modulo.aportaciones.entity.AportacionConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AportacionConfigRepository extends JpaRepository<AportacionConfig, Long> {

    List<AportacionConfig> findAllByOrderByIdDesc();

    Optional<AportacionConfig> findTopByOrderByIdDesc();
}
