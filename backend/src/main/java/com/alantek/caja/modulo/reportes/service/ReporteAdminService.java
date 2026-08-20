package com.alantek.caja.modulo.reportes.service;

import com.alantek.caja.modulo.reportes.entity.Reporte;
import com.alantek.caja.modulo.reportes.repository.ReporteRepository;
import com.alantek.caja.shared.PageResponse;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReporteAdminService {

    private final ReporteRepository repository;
    private final Map<Long, JasperReport> cache = new ConcurrentHashMap<>();

    public ReporteAdminService(ReporteRepository repository) {
        this.repository = repository;
    }

    public PageResponse<Reporte> listar(int page, int size, String buscar) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        Page<Reporte> result;
        if (buscar != null && !buscar.isBlank()) {
            result = repository
                    .findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCaseOrEntidadContainingIgnoreCase(
                            buscar, buscar, buscar, pageable);
        } else {
            result = repository.findAll(pageable);
        }
        return PageResponse.of(result);
    }

    public Reporte obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado con id: " + id));
    }

    public Reporte crear(Reporte reporte) {
        if (repository.existsByNombre(reporte.getNombre())) {
            throw new RuntimeException("Ya existe un reporte con el nombre: " + reporte.getNombre());
        }
        validarJrxml(reporte.getJrxml());
        reporte.setCreatedAt(Instant.now());
        Reporte guardado = repository.save(reporte);
        return guardado;
    }

    public Reporte actualizar(Long id, Reporte datos) {
        Reporte existente = obtenerPorId(id);
        if (!existente.getNombre().equals(datos.getNombre())
                && repository.existsByNombre(datos.getNombre())) {
            throw new RuntimeException("Ya existe un reporte con el nombre: " + datos.getNombre());
        }
        validarJrxml(datos.getJrxml());
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setTitulo(datos.getTitulo());
        existente.setEntidad(datos.getEntidad());
        existente.setFormatoDefault(datos.getFormatoDefault());
        existente.setOrientacion(datos.getOrientacion());
        existente.setParametros(datos.getParametros());
        existente.setJrxml(datos.getJrxml());
        existente.setActivo(datos.getActivo());
        existente.setUpdatedAt(Instant.now());
        Reporte guardado = repository.save(existente);
        cache.remove(id);
        return guardado;
    }

    public void eliminar(Long id) {
        Reporte reporte = obtenerPorId(id);
        reporte.setActivo(false);
        reporte.setUpdatedAt(Instant.now());
        repository.save(reporte);
        cache.remove(id);
    }

    public Reporte toggleActivo(Long id) {
        Reporte reporte = obtenerPorId(id);
        reporte.setActivo(!reporte.getActivo());
        reporte.setUpdatedAt(Instant.now());
        Reporte guardado = repository.save(reporte);
        cache.remove(id);
        return guardado;
    }

    public JasperReport compilarDesdeDb(Long id) {
        return cache.computeIfAbsent(id, this::compilarReporte);
    }

    public JasperReport compilarDesdeDbPorNombre(String nombre) {
        Reporte reporte = repository.findByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado: " + nombre));
        return compilarDesdeDb(reporte.getId());
    }

    public void invalidarCache(Long id) {
        cache.remove(id);
    }

    private JasperReport compilarReporte(Long id) {
        Reporte reporte = obtenerPorId(id);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(
                reporte.getJrxml().getBytes(StandardCharsets.UTF_8))) {
            return JasperCompileManager.compileReport(bais);
        } catch (Exception e) {
            throw new RuntimeException("Error compilando reporte '" + reporte.getNombre() + "': " + e.getMessage(), e);
        }
    }

    private void validarJrxml(String jrxml) {
        if (jrxml == null || jrxml.isBlank()) {
            throw new RuntimeException("El contenido JRXML no puede estar vacío");
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(
                jrxml.getBytes(StandardCharsets.UTF_8))) {
            JasperCompileManager.compileReport(bais);
        } catch (Exception e) {
            throw new RuntimeException("Error de compilación JRXML: " + e.getMessage(), e);
        }
    }
}
