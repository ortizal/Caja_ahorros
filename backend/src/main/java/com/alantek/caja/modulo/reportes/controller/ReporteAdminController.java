package com.alantek.caja.modulo.reportes.controller;

import com.alantek.caja.modulo.reportes.entity.Reporte;
import com.alantek.caja.modulo.reportes.service.ReporteAdminService;
import com.alantek.caja.shared.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reportes-admin")
@PreAuthorize("hasAuthority('SEGURIDAD:VER')")
public class ReporteAdminController {

    private final ReporteAdminService service;

    public ReporteAdminController(ReporteAdminService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<Reporte> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String buscar) {
        return service.listar(page, size, buscar);
    }

    @GetMapping("/{id}")
    public Reporte obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @GetMapping("/{id}/jrxml")
    public ResponseEntity<String> obtenerJrxml(@PathVariable Long id) {
        Reporte reporte = service.obtenerPorId(id);
        return ResponseEntity.ok(reporte.getJrxml());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SEGURIDAD:CREAR')")
    public ResponseEntity<Reporte> crear(@RequestBody Reporte reporte) {
        Reporte creado = service.crear(reporte);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public Reporte actualizar(@PathVariable Long id, @RequestBody Reporte reporte) {
        return service.actualizar(id, reporte);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('SEGURIDAD:EDITAR')")
    public Reporte toggleActivo(@PathVariable Long id) {
        return service.toggleActivo(id);
    }
}
