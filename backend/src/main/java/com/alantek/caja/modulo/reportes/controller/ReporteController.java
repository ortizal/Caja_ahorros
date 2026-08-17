package com.alantek.caja.modulo.reportes.controller;

import com.alantek.caja.modulo.reportes.service.ReporteService;
import com.alantek.caja.modulo.reportes.service.ReporteService.TablaReporte;
import com.lowagie.text.DocumentException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping(value = "/socios.csv", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public ResponseEntity<byte[]> sociosCsv() {
        return csv(reporteService.socios(), "socios.csv");
    }

    @GetMapping(value = "/socios.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public ResponseEntity<byte[]> sociosXlsx() throws IOException {
        return xlsx(reporteService.socios(), "socios.xlsx");
    }

    @GetMapping(value = "/socios.pdf", produces = "application/pdf")
    @PreAuthorize("hasAuthority('SOCIOS:VER')")
    public ResponseEntity<byte[]> sociosPdf() throws DocumentException {
        return pdf(reporteService.socios(), "socios.pdf");
    }

    @GetMapping(value = "/cartera.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public ResponseEntity<byte[]> carteraXlsx() throws IOException {
        return xlsx(reporteService.cartera(), "cartera.xlsx");
    }

    @GetMapping(value = "/cartera.pdf", produces = "application/pdf")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public ResponseEntity<byte[]> carteraPdf() throws DocumentException {
        return pdf(reporteService.cartera(), "cartera.pdf");
    }

    @GetMapping(value = "/caja.csv", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('CAJA:VER')")
    public ResponseEntity<byte[]> cajaCsv() {
        return csv(reporteService.caja(), "caja.csv");
    }

    @GetMapping(value = "/caja.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAuthority('CAJA:VER')")
    public ResponseEntity<byte[]> cajaXlsx() throws IOException {
        return xlsx(reporteService.caja(), "caja.xlsx");
    }

    @GetMapping(value = "/caja.pdf", produces = "application/pdf")
    @PreAuthorize("hasAuthority('CAJA:VER')")
    public ResponseEntity<byte[]> cajaPdf() throws DocumentException {
        return pdf(reporteService.caja(), "caja.pdf");
    }

    private ResponseEntity<byte[]> csv(TablaReporte tabla, String nombre) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(reporteService.generarCsv(tabla));
    }

    private ResponseEntity<byte[]> xlsx(TablaReporte tabla, String nombre) throws IOException {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(reporteService.generarXlsx(tabla));
    }

    private ResponseEntity<byte[]> pdf(TablaReporte tabla, String nombre) throws DocumentException {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(reporteService.generarPdf(tabla));
    }
}
