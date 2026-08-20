package com.alantek.caja.modulo.reportes.controller;

import com.alantek.caja.modulo.reportes.service.ReporteService;
import com.alantek.caja.modulo.reportes.service.ReporteService.TablaReporte;
import com.lowagie.text.DocumentException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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

    @PostMapping(value = "/simulacion/{formato}", produces = "application/pdf")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public ResponseEntity<byte[]> simulacionReporte(
            @PathVariable String formato,
            @RequestParam BigDecimal monto,
            @RequestParam BigDecimal tasaInteres,
            @RequestParam int plazoMeses,
            @RequestParam(defaultValue = "FRANCES") String sistema) throws DocumentException, IOException {
        TablaReporte tabla = reporteService.simulacion(monto, tasaInteres, plazoMeses, sistema);
        if ("xlsx".equalsIgnoreCase(formato)) {
            return xlsx(tabla, "simulacion.xlsx");
        }
        return pdf(tabla, "simulacion.pdf");
    }

    @GetMapping("/{nombre}/{formato}")
    @PreAuthorize("hasAuthority('SEGURIDAD:VER')")
    public ResponseEntity<byte[]> jasperReport(
            @PathVariable String nombre,
            @PathVariable String formato,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Long socioId) {
        Map<String, Object> params = new HashMap<>();
        if (desde != null) params.put("desde", desde);
        if (hasta != null) params.put("hasta", hasta);
        if (estado != null) params.put("estado", estado);
        if (socioId != null) params.put("socioId", socioId);
        params.put("subtitle", buildSubtitle(nombre, estado, socioId));

        byte[] bytes = reporteService.generarReporteJasper(nombre, params, formato);

        MediaType mediaType = "xlsx".equalsIgnoreCase(formato) ? XLSX : MediaType.APPLICATION_PDF;
        String extension = "xlsx".equalsIgnoreCase(formato) ? ".xlsx" : ".pdf";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + extension + "\"")
                .body(bytes);
    }

    private String buildSubtitle(String nombre, String estado, Long socioId) {
        StringBuilder sb = new StringBuilder();
        if (estado != null) sb.append("Estado: ").append(estado);
        if (socioId != null) {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append("Socio ID: ").append(socioId);
        }
        return sb.isEmpty() ? getDefaultSubtitle(nombre) : sb.toString();
    }

    private String getDefaultSubtitle(String nombre) {
        return switch (nombre) {
            case "socios" -> "Todos los socios";
            case "cartera" -> "Cartera completa";
            case "caja" -> "Resumen de caja";
            case "ahorros" -> "Todas las cuentas de ahorro";
            case "aportaciones" -> "Todas las aportaciones";
            case "creditos" -> "Todos los creditos";
            case "usuarios" -> "Todos los usuarios";
            default -> "";
        };
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
