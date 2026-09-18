package com.alantek.caja.modulo.balance.controller;

import com.alantek.caja.modulo.balance.dto.BalanceComprobacionResponse;
import com.alantek.caja.modulo.balance.dto.BalancePersonalResponse;
import com.alantek.caja.modulo.balance.dto.BalanceSocialResponse;
import com.alantek.caja.modulo.balance.dto.BalanceSituacionResponse;
import com.alantek.caja.modulo.balance.service.BalanceService;
import com.alantek.caja.modulo.reportes.service.ReporteService;
import com.lowagie.text.DocumentException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/balance")
public class BalanceController {

    private final BalanceService balanceService;
    private final ReporteService reporteService;

    public BalanceController(BalanceService balanceService, ReporteService reporteService) {
        this.balanceService = balanceService;
        this.reporteService = reporteService;
    }

    @GetMapping("/situacion")
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public BalanceSituacionResponse balanceSituacion() {
        return balanceService.balanceSituacion();
    }

    @GetMapping("/comprobacion")
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public BalanceComprobacionResponse balanceComprobacion(
            @RequestParam Integer anio,
            @RequestParam Integer mes) {
        return balanceService.balanceComprobacion(anio, mes);
    }

    @GetMapping("/social")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public BalanceSocialResponse balanceSocial() {
        return balanceService.balanceSocial();
    }

    @GetMapping("/personal")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public BalancePersonalResponse balancePersonal(@RequestParam Long socioId) {
        return balanceService.balancePersonal(socioId);
    }

    @GetMapping("/situacion.pdf")
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public ResponseEntity<byte[]> situacionPdf() throws DocumentException {
        BalanceSituacionResponse resp = balanceService.balanceSituacion();
        byte[] pdf = reporteService.generarPdf(balanceService.toTablaSituacion(resp));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_situacion.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/situacion.xlsx")
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public ResponseEntity<byte[]> situacionXlsx() throws IOException {
        BalanceSituacionResponse resp = balanceService.balanceSituacion();
        byte[] xlsx = reporteService.generarXlsx(balanceService.toTablaSituacion(resp));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_situacion.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @GetMapping("/comprobacion.pdf")
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public ResponseEntity<byte[]> comprobacionPdf(
            @RequestParam Integer anio, @RequestParam Integer mes) throws DocumentException {
        BalanceComprobacionResponse resp = balanceService.balanceComprobacion(anio, mes);
        byte[] pdf = reporteService.generarPdf(balanceService.toTablaComprobacion(resp));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_comprobacion_" + anio + "_" + mes + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/comprobacion.xlsx")
    @PreAuthorize("hasAuthority('CONTABILIDAD:VER')")
    public ResponseEntity<byte[]> comprobacionXlsx(
            @RequestParam Integer anio, @RequestParam Integer mes) throws IOException {
        BalanceComprobacionResponse resp = balanceService.balanceComprobacion(anio, mes);
        byte[] xlsx = reporteService.generarXlsx(balanceService.toTablaComprobacion(resp));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_comprobacion_" + anio + "_" + mes + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @GetMapping("/social.pdf")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public ResponseEntity<byte[]> socialPdf() throws DocumentException {
        BalanceSocialResponse resp = balanceService.balanceSocial();
        byte[] pdf = reporteService.generarPdf(balanceService.toTablaSocial(resp));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_social.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/social.xlsx")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public ResponseEntity<byte[]> socialXlsx() throws IOException {
        BalanceSocialResponse resp = balanceService.balanceSocial();
        byte[] xlsx = reporteService.generarXlsx(balanceService.toTablaSocial(resp));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_social.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @GetMapping("/personal.pdf")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public ResponseEntity<byte[]> personalPdf(@RequestParam Long socioId) throws DocumentException {
        BalancePersonalResponse resp = balanceService.balancePersonal(socioId);
        byte[] pdf = reporteService.generarPdf(balanceService.toTablaPersonal(resp));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_personal_" + socioId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/personal.xlsx")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public ResponseEntity<byte[]> personalXlsx(@RequestParam Long socioId) throws IOException {
        BalancePersonalResponse resp = balanceService.balancePersonal(socioId);
        byte[] xlsx = reporteService.generarXlsx(balanceService.toTablaPersonal(resp));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_personal_" + socioId + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}