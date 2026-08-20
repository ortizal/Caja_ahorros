package com.alantek.caja.modulo.reportes.service;

import com.alantek.caja.modulo.ahorros.entity.CuentaAhorro;
import com.alantek.caja.modulo.ahorros.entity.ProductoAhorro;
import com.alantek.caja.modulo.ahorros.repository.CuentaAhorroRepository;
import com.alantek.caja.modulo.ahorros.repository.ProductoAhorroRepository;
import com.alantek.caja.modulo.aportaciones.entity.Aportacion;
import com.alantek.caja.modulo.aportaciones.repository.AportacionRepository;
import com.alantek.caja.modulo.caja.entity.CajaApertura;
import com.alantek.caja.modulo.caja.entity.CajaMovimiento;
import com.alantek.caja.modulo.caja.repository.CajaAperturaRepository;
import com.alantek.caja.modulo.caja.repository.CajaMovimientoRepository;
import com.alantek.caja.modulo.caja.service.TipoMovimiento;
import com.alantek.caja.modulo.cartera.service.CarteraService;
import com.alantek.caja.modulo.creditos.dto.SimulacionCreditoResponse;
import com.alantek.caja.modulo.creditos.entity.Credito;
import com.alantek.caja.modulo.creditos.entity.ProductoCredito;
import com.alantek.caja.modulo.creditos.repository.CreditoRepository;
import com.alantek.caja.modulo.creditos.repository.ProductoCreditoRepository;
import com.alantek.caja.modulo.creditos.service.AmortizacionService;
import com.alantek.caja.modulo.seguridad.entity.Rol;
import com.alantek.caja.modulo.seguridad.entity.Usuario;
import com.alantek.caja.modulo.seguridad.repository.UsuarioRepository;
import com.alantek.caja.modulo.socios.entity.Socio;
import com.alantek.caja.modulo.socios.repository.SocioRepository;
import com.alantek.caja.shared.exception.BusinessException;
import com.alantek.caja.shared.reports.JasperReportService;
import com.alantek.caja.shared.reports.ReportBeans;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final JasperReportService jasperReportService;
    private final CuentaAhorroRepository cuentaAhorroRepository;
    private final ProductoAhorroRepository productoAhorroRepository;
    private final AportacionRepository aportacionRepository;
    private final CreditoRepository creditoRepository;
    private final ProductoCreditoRepository productoCreditoRepository;
    private final AmortizacionService amortizacionService;

    public ReporteService(SocioRepository socioRepository,
                          CarteraService carteraService,
                          CajaAperturaRepository cajaAperturaRepository,
                          CajaMovimientoRepository cajaMovimientoRepository,
                          UsuarioRepository usuarioRepository,
                          JasperReportService jasperReportService,
                          CuentaAhorroRepository cuentaAhorroRepository,
                          ProductoAhorroRepository productoAhorroRepository,
                          AportacionRepository aportacionRepository,
                          CreditoRepository creditoRepository,
                          ProductoCreditoRepository productoCreditoRepository,
                          AmortizacionService amortizacionService) {
        this.socioRepository = socioRepository;
        this.carteraService = carteraService;
        this.cajaAperturaRepository = cajaAperturaRepository;
        this.cajaMovimientoRepository = cajaMovimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.jasperReportService = jasperReportService;
        this.cuentaAhorroRepository = cuentaAhorroRepository;
        this.productoAhorroRepository = productoAhorroRepository;
        this.aportacionRepository = aportacionRepository;
        this.creditoRepository = creditoRepository;
        this.productoCreditoRepository = productoCreditoRepository;
        this.amortizacionService = amortizacionService;
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

    public byte[] generarReporteJasper(String nombre, Map<String, Object> params, String formato) {
        JRBeanCollectionDataSource dataSource;
        switch (nombre) {
            case "socios" -> dataSource = prepareSocios();
            case "cartera" -> dataSource = prepareCartera(params);
            case "caja" -> dataSource = prepareCaja();
            case "ahorros" -> dataSource = prepareAhorros(params);
            case "aportaciones" -> dataSource = prepareAportaciones(params);
            case "creditos" -> dataSource = prepareCreditos(params);
            case "usuarios" -> dataSource = prepareUsuarios();
            default -> throw new BusinessException("Reporte no encontrado: " + nombre);
        }
        return jasperReportService.generarReporte(nombre, dataSource, params != null ? params : Map.of(), formato);
    }

    private JRBeanCollectionDataSource prepareSocios() {
        List<ReportBeans.SocioData> data = socioRepository.findAll().stream()
                .sorted(Comparator.comparing(Socio::getCodigo))
                .map(s -> new ReportBeans.SocioData(
                        nvl(s.getCodigo()), nvl(s.getIdentificacion()), nvl(s.getNombres()),
                        nvl(s.getApellidos()), nvl(s.getTelefono()), nvl(s.getEmail()),
                        nvl(s.getEstado()),
                        s.getFechaIngreso() == null ? "" : s.getFechaIngreso().toString()))
                .toList();
        return new JRBeanCollectionDataSource(data);
    }

    @SuppressWarnings("unchecked")
    private JRBeanCollectionDataSource prepareCartera(Map<String, Object> params) {
        String estado = params != null ? (String) params.get("estado") : null;
        Long socioId = null;
        if (params != null && params.get("socioId") != null) {
            socioId = Long.valueOf(params.get("socioId").toString());
        }
        List<ReportBeans.CarteraData> data = carteraService.listarCartera(estado, socioId).stream()
                .map(c -> new ReportBeans.CarteraData(
                        nvl(c.socioCodigo()) + " " + nvl(c.socioNombre()),
                        nvl(c.nombreProducto()),
                        String.valueOf(c.numeroCuota()),
                        c.fechaVencimiento() == null ? "" : c.fechaVencimiento().toString(),
                        c.saldoCapital().toPlainString(),
                        c.cuotaTotal().toPlainString(),
                        c.mora().toPlainString(),
                        c.totalPagar().toPlainString(),
                        nvl(c.estado()),
                        String.valueOf(c.diasVencido())))
                .toList();
        return new JRBeanCollectionDataSource(data);
    }

    private JRBeanCollectionDataSource prepareCaja() {
        List<CajaApertura> aperturas = cajaAperturaRepository.findAll();
        Map<Long, List<CajaMovimiento>> movimientos = cajaMovimientoRepository.findAll().stream()
                .collect(Collectors.groupingBy(CajaMovimiento::getCajaAperturaId));
        Map<Long, Usuario> usuarios = usuarioRepository.findAll().stream()
                .collect(Collectors.toMap(Usuario::getId, u -> u));
        List<ReportBeans.CajaData> data = new ArrayList<>();
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
            data.add(new ReportBeans.CajaData(
                    cajero == null ? "" : nvl(cajero.getNombreCompleto()) + " (" + nvl(cajero.getUsername()) + ")",
                    apertura.getFecha() == null ? "" : apertura.getFecha().toString(),
                    apertura.getSaldoInicial().toPlainString(),
                    ingresos.toPlainString(),
                    egresos.toPlainString(),
                    saldoFinal.toPlainString(),
                    nvl(apertura.getEstado())));
        }
        return new JRBeanCollectionDataSource(data);
    }

    private JRBeanCollectionDataSource prepareAhorros(Map<String, Object> params) {
        Long socioId = null;
        if (params != null && params.get("socioId") != null) {
            socioId = Long.valueOf(params.get("socioId").toString());
        }
        List<CuentaAhorro> cuentas = socioId != null
                ? cuentaAhorroRepository.findBySocioIdOrderByFechaAperturaDesc(socioId)
                : cuentaAhorroRepository.findAllByOrderByFechaAperturaDesc();
        List<ReportBeans.AhorroData> data = new ArrayList<>();
        for (CuentaAhorro cuenta : cuentas) {
            Socio socio = socioRepository.findById(cuenta.getSocioId()).orElse(null);
            ProductoAhorro producto = productoAhorroRepository.findById(cuenta.getProductoId()).orElse(null);
            String socioNombre = socio == null ? ""
                    : nvl(socio.getCodigo()) + " " + nvl(socio.getNombres()) + " " + nvl(socio.getApellidos());
            data.add(new ReportBeans.AhorroData(
                    nvl(cuenta.getNumeroCuenta()),
                    socioNombre,
                    producto == null ? "" : nvl(producto.getNombre()),
                    cuenta.getSaldo().toPlainString(),
                    nvl(cuenta.getEstado()),
                    cuenta.getFechaApertura() == null ? "" : cuenta.getFechaApertura().toString()));
        }
        return new JRBeanCollectionDataSource(data);
    }

    private JRBeanCollectionDataSource prepareAportaciones(Map<String, Object> params) {
        Long socioId = null;
        if (params != null && params.get("socioId") != null) {
            socioId = Long.valueOf(params.get("socioId").toString());
        }
        List<Aportacion> aportaciones = socioId != null
                ? aportacionRepository.findBySocioIdOrderByPeriodoDesc(socioId)
                : aportacionRepository.findAllByOrderByPeriodoDesc();
        List<ReportBeans.AportacionData> data = new ArrayList<>();
        for (Aportacion aportacion : aportaciones) {
            Socio socio = socioRepository.findById(aportacion.getSocioId()).orElse(null);
            String socioNombre = socio == null ? ""
                    : nvl(socio.getCodigo()) + " " + nvl(socio.getNombres()) + " " + nvl(socio.getApellidos());
            data.add(new ReportBeans.AportacionData(
                    socioNombre,
                    nvl(aportacion.getPeriodo()),
                    aportacion.getMontoEsperado().toPlainString(),
                    nvl(aportacion.getEstado()),
                    aportacion.getMontoPagado().toPlainString()));
        }
        return new JRBeanCollectionDataSource(data);
    }

    private JRBeanCollectionDataSource prepareCreditos(Map<String, Object> params) {
        Long socioId = null;
        if (params != null && params.get("socioId") != null) {
            socioId = Long.valueOf(params.get("socioId").toString());
        }
        List<Credito> creditos = socioId != null
                ? creditoRepository.findBySocioIdOrderByCreatedAtDesc(socioId)
                : creditoRepository.findAllByOrderByCreatedAtDesc();
        List<ReportBeans.CreditoData> data = new ArrayList<>();
        for (Credito credito : creditos) {
            Socio socio = socioRepository.findById(credito.getSocioId()).orElse(null);
            ProductoCredito producto = productoCreditoRepository.findById(credito.getProductoId()).orElse(null);
            String socioNombre = socio == null ? ""
                    : nvl(socio.getCodigo()) + " " + nvl(socio.getNombres()) + " " + nvl(socio.getApellidos());
            data.add(new ReportBeans.CreditoData(
                    socioNombre,
                    producto == null ? "" : nvl(producto.getNombre()),
                    credito.getMontoDesembolsado().toPlainString(),
                    credito.getTasaInteres().toPlainString(),
                    String.valueOf(credito.getPlazoMeses()),
                    calcularCuotaMensual(credito.getMontoDesembolsado(), credito.getTasaInteres(), credito.getPlazoMeses()),
                    nvl(credito.getEstado()),
                    credito.getFechaDesembolso() == null ? "" : credito.getFechaDesembolso().toString()));
        }
        return new JRBeanCollectionDataSource(data);
    }

    private JRBeanCollectionDataSource prepareUsuarios() {
        List<ReportBeans.UsuarioData> data = usuarioRepository.findAll().stream()
                .map(u -> new ReportBeans.UsuarioData(
                        nvl(u.getUsername()),
                        nvl(u.getNombreCompleto()),
                        nvl(u.getEmail()),
                        nvl(u.getEstado()),
                        u.getRoles().stream().map(Rol::getNombre).sorted().collect(Collectors.joining(", ")),
                        u.getUltimoAcceso() == null ? "" : u.getUltimoAcceso().toString()))
                .toList();
        return new JRBeanCollectionDataSource(data);
    }

    private String calcularCuotaMensual(BigDecimal monto, BigDecimal tasaAnualPct, int plazoMeses) {
        if (tasaAnualPct == null || tasaAnualPct.signum() == 0 || plazoMeses <= 0) {
            return monto == null ? "0.00" : monto.divide(BigDecimal.valueOf(plazoMeses), 2, RoundingMode.HALF_UP).toPlainString();
        }
        BigDecimal tasaMensual = tasaAnualPct.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(tasaMensual).pow(plazoMeses);
        BigDecimal numerador = monto.multiply(tasaMensual).multiply(factor);
        BigDecimal denominador = factor.subtract(BigDecimal.ONE);
        return numerador.divide(denominador, 2, RoundingMode.HALF_UP).toPlainString();
    }

    private String nvl(String valor) {
        return valor == null ? "" : valor;
    }

    public TablaReporte simulacion(BigDecimal monto, BigDecimal tasaInteres, int plazoMeses, String sistema) {
        SimulacionCreditoResponse res = amortizacionService.simular(monto, tasaInteres, plazoMeses, sistema);
        List<String> encabezados = List.of("Cuota", "Vencimiento", "Capital", "Interes", "Cuota Total", "Saldo Capital");
        List<List<String>> filas = res.cuotas().stream()
                .map(c -> List.of(
                        String.valueOf(c.numero()),
                        c.fechaVencimiento().toString(),
                        c.capital().toPlainString(),
                        c.interes().toPlainString(),
                        c.cuota().toPlainString(),
                        c.saldo().toPlainString()))
                .toList();
        String titulo = "Simulacion " + sistema + " | Monto: $" + monto.toPlainString()
                + " | Plazo: " + plazoMeses + " meses | Tasa: " + tasaInteres + "%";
        return new TablaReporte(titulo, encabezados, filas);
    }
}
