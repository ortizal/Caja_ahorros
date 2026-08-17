package com.alantek.caja.modulo.portal.controller;

import com.alantek.caja.modulo.portal.dto.PortalAhorroResponse;
import com.alantek.caja.modulo.portal.dto.PortalAportacionResponse;
import com.alantek.caja.modulo.portal.dto.PortalCreditoResponse;
import com.alantek.caja.modulo.portal.dto.PortalResumenResponse;
import com.alantek.caja.modulo.portal.service.PortalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public List<PortalAhorroResponse> ahorro() {
        return portalService.ahorro();
    }

    @GetMapping("/aportaciones")
    @PreAuthorize("hasAuthority('PORTAL:VER')")
    public List<PortalAportacionResponse> aportaciones() {
        return portalService.aportaciones();
    }

    @GetMapping("/creditos")
    @PreAuthorize("hasAuthority('PORTAL:VER')")
    public List<PortalCreditoResponse> creditos() {
        return portalService.creditos();
    }
}
