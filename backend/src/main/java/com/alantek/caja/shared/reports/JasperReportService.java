package com.alantek.caja.shared.reports;

import com.alantek.caja.modulo.reportes.entity.Reporte;
import com.alantek.caja.modulo.reportes.repository.ReporteRepository;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JasperReportService {

    private final Map<String, JasperReport> cache = new ConcurrentHashMap<>();
    private final ReporteRepository reporteRepository;

    public JasperReportService(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

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
        Optional<Reporte> dbReporte = reporteRepository.findByNombre(nombre);
        if (dbReporte.isPresent()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(
                    dbReporte.get().getJrxml().getBytes(StandardCharsets.UTF_8))) {
                return JasperCompileManager.compileReport(bais);
            } catch (Exception e) {
                throw new RuntimeException("Error compilando reporte DB '" + nombre + "': " + e.getMessage(), e);
            }
        }
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

    public void invalidarCache(String nombre) {
        cache.remove(nombre);
    }
}
