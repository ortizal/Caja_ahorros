package com.alantek.caja.modulo.reportes.service;

import com.alantek.caja.modulo.caja.entity.CajaApertura;
import com.alantek.caja.modulo.caja.entity.CajaMovimiento;
import com.alantek.caja.modulo.caja.repository.CajaAperturaRepository;
import com.alantek.caja.modulo.caja.repository.CajaMovimientoRepository;
import com.alantek.caja.modulo.caja.service.TipoMovimiento;
import com.alantek.caja.modulo.cartera.service.CarteraService;
import com.alantek.caja.modulo.seguridad.entity.Usuario;
import com.alantek.caja.modulo.seguridad.repository.UsuarioRepository;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    public record TablaReporte(String titulo, List<String> encabezados, List<List<String>> filas) {
    }

    private final SocioRepository socioRepository;
    private final CarteraService carteraService;
    private final CajaAperturaRepository cajaAperturaRepository;
    private final CajaMovimientoRepository cajaMovimientoRepository;
    private final UsuarioRepository usuarioRepository;

    public ReporteService(SocioRepository socioRepository,
                          CarteraService carteraService,
                          CajaAperturaRepository cajaAperturaRepository,
                          CajaMovimientoRepository cajaMovimientoRepository,
                          UsuarioRepository usuarioRepository) {
        this.socioRepository = socioRepository;
        this.carteraService = carteraService;
        this.cajaAperturaRepository = cajaAperturaRepository;
        this.cajaMovimientoRepository = cajaMovimientoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public TablaReporte socios() {
        List<String> encabezados = List.of("Codigo", "Identificacion", "Nombres", "Apellidos",
                "Telefono", "Email", "Estado", "FechaIngreso");
        List<List<String>> filas = socioRepository.findAll().stream()
                .sorted(Comparator.comparing(Socio::getCodigo))
                .map(s -> List.of(
                        nvl(s.getCodigo()),
                        nvl(s.getIdentificacion()),
                        nvl(s.getNombres()),
                        nvl(s.getApellidos()),
                        nvl(s.getTelefono()),
                        nvl(s.getEmail()),
                        nvl(s.getEstado()),
                        s.getFechaIngreso() == null ? "" : s.getFechaIngreso().toString()))
                .collect(Collectors.toList());
        return new TablaReporte("Socios", encabezados, filas);
    }

    public TablaReporte cartera() {
        List<String> encabezados = List.of("CuotaId", "CreditoId", "Socio", "Producto", "NoCuota",
                "Vencimiento", "Capital", "CuotaTotal", "Mora", "TotalPagar", "Estado", "DiasVencido");
        List<List<String>> filas = carteraService.listarCartera(null, null).stream()
                .map(c -> List.of(
                        String.valueOf(c.id()),
                        String.valueOf(c.creditoId()),
                        nvl(c.socioCodigo()) + " - " + nvl(c.socioNombre()),
                        nvl(c.nombreProducto()),
                        String.valueOf(c.numeroCuota()),
                        c.fechaVencimiento() == null ? "" : c.fechaVencimiento().toString(),
                        c.saldoCapital().toPlainString(),
                        c.cuotaTotal().toPlainString(),
                        c.mora().toPlainString(),
                        c.totalPagar().toPlainString(),
                        nvl(c.estado()),
                        String.valueOf(c.diasVencido())))
                .collect(Collectors.toList());
        return new TablaReporte("Cartera de creditos", encabezados, filas);
    }

    public TablaReporte caja() {
        List<String> encabezados = List.of("Apertura", "Fecha", "Cajero", "SaldoInicial",
                "Ingresos", "Egresos", "SaldoFinal", "Estado");
        List<CajaApertura> aperturas = cajaAperturaRepository.findAll();
        Map<Long, List<CajaMovimiento>> movimientos = cajaMovimientoRepository.findAll().stream()
                .collect(Collectors.groupingBy(CajaMovimiento::getCajaAperturaId));
        Map<Long, Usuario> usuarios = usuarioRepository.findAll().stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u));
        List<List<String>> filas = new ArrayList<>();
        for (CajaApertura apertura : aperturas) {
            BigDecimal ingresos = BigDecimal.ZERO;
            BigDecimal egresos = BigDecimal.ZERO;
            for (CajaMovimiento movimiento : movimientos.getOrDefault(apertura.getId(), List.of())) {
                TipoMovimiento tipo = TipoMovimiento.from(movimiento.getTipo());
                if (tipo != null && tipo.isEgreso()) {
                    egresos = egresos.add(movimiento.getMonto());
                } else {
                    ingresos = ingresos.add(movimiento.getMonto());
                }
            }
            BigDecimal saldoFinal = apertura.getSaldoInicial().add(ingresos).subtract(egresos);
            Usuario cajero = apertura.getCajeroId() == null ? null : usuarios.get(apertura.getCajeroId());
            filas.add(List.of(
                    String.valueOf(apertura.getId()),
                    apertura.getFecha() == null ? "" : apertura.getFecha().toString(),
                    cajero == null ? "" : nvl(cajero.getNombreCompleto()) + " (" + nvl(cajero.getUsername()) + ")",
                    apertura.getSaldoInicial().toPlainString(),
                    ingresos.toPlainString(),
                    egresos.toPlainString(),
                    saldoFinal.toPlainString(),
                    nvl(apertura.getEstado())));
        }
        return new TablaReporte("Caja", encabezados, filas);
    }

    public byte[] generarCsv(TablaReporte tabla) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(String.join(";", tabla.encabezados())).append('\n');
        for (List<String> fila : tabla.filas()) {
            sb.append(String.join(";", fila)).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generarXlsx(TablaReporte tabla) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(tabla.titulo());
            Row header = sheet.createRow(0);
            for (int i = 0; i < tabla.encabezados().size(); i++) {
                header.createCell(i).setCellValue(tabla.encabezados().get(i));
            }
            int filaIndex = 1;
            for (List<String> fila : tabla.filas()) {
                Row row = sheet.createRow(filaIndex++);
                for (int j = 0; j < fila.size(); j++) {
                    row.createCell(j).setCellValue(fila.get(j));
                }
            }
            for (int i = 0; i < tabla.encabezados().size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generarPdf(TablaReporte tabla) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph(tabla.titulo(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        PdfPTable table = new PdfPTable(tabla.encabezados().size());
        table.setWidthPercentage(100);
        for (String encabezado : tabla.encabezados()) {
            table.addCell(new Phrase(encabezado, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        }
        for (List<String> fila : tabla.filas()) {
            for (String celda : fila) {
                table.addCell(new Phrase(celda, FontFactory.getFont(FontFactory.HELVETICA, 8)));
            }
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private String nvl(String valor) {
        return valor == null ? "" : valor;
    }
}
