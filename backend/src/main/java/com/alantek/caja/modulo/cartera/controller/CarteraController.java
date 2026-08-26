package com.alantek.caja.modulo.cartera.controller;

import com.alantek.caja.modulo.cartera.dto.CarteraItemResponse;
import com.alantek.caja.modulo.cartera.dto.DashboardGraficosResponse;
import com.alantek.caja.modulo.cartera.dto.DashboardResumenResponse;
import com.alantek.caja.modulo.cartera.dto.MoraClienteDetalleResponse;
import com.alantek.caja.modulo.cartera.dto.MoraClienteResponse;
import com.alantek.caja.modulo.cartera.dto.MorosidadResponse;
import com.alantek.caja.modulo.cartera.service.CarteraService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CarteraController {

    private final CarteraService carteraService;

    public CarteraController(CarteraService carteraService) {
        this.carteraService = carteraService;
    }

    @GetMapping("/cartera")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public List<CarteraItemResponse> listarCartera(@RequestParam(required = false) String estado,
                                                   @RequestParam(required = false) Long socioId) {
        return carteraService.listarCartera(estado, socioId);
    }

    @GetMapping("/cartera/morosidad")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public MorosidadResponse morosidad() {
        return carteraService.morosidad();
    }

    @GetMapping("/mora/clientes")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public List<MoraClienteResponse> clientesConMora() {
        return carteraService.clientesConMora();
    }

    @GetMapping("/mora/clientes/{socioId}")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public MoraClienteDetalleResponse detalleMoraCliente(@PathVariable Long socioId) {
        return carteraService.detalleMoraCliente(socioId);
    }

    @GetMapping("/dashboard/resumen")
    @PreAuthorize("isAuthenticated()")
    public DashboardResumenResponse resumen() {
        return carteraService.resumen();
    }

    @GetMapping("/dashboard/graficos")
    @PreAuthorize("isAuthenticated()")
    public DashboardGraficosResponse graficos() {
        return carteraService.graficos();
    }

    @GetMapping(value = "/reportes/cartera", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('CREDITOS:VER')")
    public ResponseEntity<byte[]> exportarCartera() {
        byte[] csv = generarCsv(carteraService.listarCartera(null, null));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cartera.csv\"")
                .body(csv);
    }

    private byte[] generarCsv(List<CarteraItemResponse> items) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("cuota_id;credito_id;socio_codigo;socio;producto;numero_cuota;vencimiento;"
                + "capital;cuota_total;mora;total_pagar;estado;dias_vencido\n");
        for (CarteraItemResponse item : items) {
            sb.append(item.id()).append(';')
                    .append(item.creditoId()).append(';')
                    .append(item.socioCodigo()).append(';')
                    .append(item.socioNombre()).append(';')
                    .append(item.nombreProducto() == null ? "" : item.nombreProducto()).append(';')
                    .append(item.numeroCuota()).append(';')
                    .append(item.fechaVencimiento()).append(';')
                    .append(item.saldoCapital().toPlainString()).append(';')
                    .append(item.cuotaTotal().toPlainString()).append(';')
                    .append(item.mora().toPlainString()).append(';')
                    .append(item.totalPagar().toPlainString()).append(';')
                    .append(item.estado()).append(';')
                    .append(item.diasVencido()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
