package com.alantek.caja.shared.reports;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.poi.export.JRXlsxExporter;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JasperReportService {

    private final Map<String, JasperReport> cache = new ConcurrentHashMap<>();

    public byte[] generarReporte(String nombreReporte, JRBeanCollectionDataSource dataSource,
                                  Map<String, Object> parametros, String formato) {
        try {
            JasperReport jasperReport = cache.computeIfAbsent(nombreReporte, this::compilar);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if ("xlsx".equalsIgnoreCase(formato)) {
                JRXlsxExporter exporter = new JRXlsxExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
                exporter.exportReport();
            } else {
                JRPdfExporter exporter = new JRPdfExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
                exporter.exportReport();
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando reporte " + nombreReporte + ": " + e.getMessage(), e);
        }
    }

    private JasperReport compilar(String nombre) {
        try (InputStream is = getClass().getResourceAsStream("/reports/" + nombre + ".jrxml")) {
            if (is == null) {
                throw new RuntimeException("Template no encontrado: reports/" + nombre + ".jrxml");
            }
            return JasperCompileManager.compileReport(is);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error compilando reporte " + nombre + ": " + e.getMessage(), e);
        }
    }
}
