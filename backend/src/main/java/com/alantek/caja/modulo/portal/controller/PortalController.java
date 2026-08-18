package com.alantek.caja.modulo.portal.controller;

import com.alantek.caja.modulo.portal.dto.PortalAhorroResponse;
import com.alantek.caja.modulo.portal.dto.PortalAportacionResponse;
import com.alantek.caja.modulo.portal.dto.PortalCreditoResponse;
import com.alantek.caja.modulo.portal.dto.PortalResumenResponse;
import com.alantek.caja.modulo.portal.service.PortalService;
import com.alantek.caja.shared.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('PORTAL:VER')")
    public PortalResumenResponse resumen() {
        return portalService.resumen();
    }

    @GetMapping("/ahorro")
    @PreAuthorize("hasAuthority('PORTAL:VER')")
    public PageResponse<PortalAhorroResponse> ahorro(@PageableDefault(size = 10) Pageable pageable) {
        return portalService.ahorro(pageable);
    }

    @GetMapping("/aportaciones")
    @PreAuthorize("hasAuthority('PORTAL:VER')")
    public PageResponse<PortalAportacionResponse> aportaciones(@PageableDefault(size = 10) Pageable pageable) {
        return portalService.aportaciones(pageable);
    }

    @GetMapping("/creditos")
    @PreAuthorize("hasAuthority('PORTAL:VER')")
    public PageResponse<PortalCreditoResponse> creditos(@PageableDefault(size = 10) Pageable pageable) {
        return portalService.creditos(pageable);
    }
}
