package com.alantek.caja.modulo.contabilidad.controller;

import com.alantek.caja.modulo.contabilidad.entity.PeriodoContable;
import com.alantek.caja.modulo.contabilidad.service.PeriodoContableService;
import com.alantek.caja.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/periodos-contables")
public class PeriodoContableController {

    private final PeriodoContableService periodoService;

    public PeriodoContableController(PeriodoContableService periodoService) {
        this.periodoService = periodoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public PageResponse<PeriodoContable> listar(@PageableDefault(size = 10) Pageable pageable) {
        return periodoService.listar(pageable);
    }

    @PostMapping("/cerrar")
    @PreAuthorize("hasAuthority('CONTABILIDAD:APROBAR')")
    public PeriodoContable cerrar(@RequestParam Integer anio, @RequestParam Integer mes) {
        return periodoService.cerrar(anio, mes);
    }

    @PostMapping("/reabrir")
    @PreAuthorize("hasRole('ADMIN')")
    public PeriodoContable reabrir(@RequestParam Integer anio, @RequestParam Integer mes) {
        return periodoService.reabrir(anio, mes);
    }
}
