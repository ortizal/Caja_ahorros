package com.alantek.caja.modulo.email.service;

import com.alantek.caja.modulo.email.dto.EmailConfiguracionRequest;
import com.alantek.caja.modulo.email.dto.EmailConfiguracionResponse;
import com.alantek.caja.modulo.email.dto.EmailPlantillaRequest;
import com.alantek.caja.modulo.email.dto.EmailPlantillaResponse;
import com.alantek.caja.modulo.email.entity.EmailConfiguracion;
import com.alantek.caja.modulo.email.entity.EmailPlantilla;
import com.alantek.caja.modulo.email.repository.EmailConfiguracionRepository;
import com.alantek.caja.modulo.email.repository.EmailPlantillaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailConfigService {

    private final EmailConfiguracionRepository configRepo;
    private final EmailPlantillaRepository plantillaRepo;

    public EmailConfigService(EmailConfiguracionRepository configRepo, EmailPlantillaRepository plantillaRepo) {
        this.configRepo = configRepo;
        this.plantillaRepo = plantillaRepo;
    }

    // ── Configuracion ─────────────────────────────────────────────────────

    public Optional<EmailConfiguracionResponse> obtenerConfiguracion() {
        return configRepo.findFirstByActivoTrue().or(() -> configRepo.findAll().stream().findFirst())
                .map(this::toConfigResponse);
    }

    @Transactional
    public EmailConfiguracionResponse guardarConfiguracion(EmailConfiguracionRequest request) {
        EmailConfiguracion config = configRepo.findFirstByActivoTrue().orElseGet(EmailConfiguracion::new);
        config.setMetodo(request.metodo());
        config.setSmtpHost(request.smtpHost());
        config.setSmtpPort(request.smtpPort());
        config.setSmtpUsername(request.smtpUsername());
        config.setSmtpPassword(request.smtpPassword());
        config.setSmtpUseTls(request.smtpUseTls() != null ? request.smtpUseTls() : true);
        config.setSmtpUseSsl(request.smtpUseSsl() != null ? request.smtpUseSsl() : false);
        config.setApiUrl(request.apiUrl());
        config.setApiKey(request.apiKey());
        config.setApiProvider(request.apiProvider());
        config.setFromEmail(request.fromEmail());
        config.setFromName(request.fromName());
        config.setActivo(request.activo() != null ? request.activo() : true);
        return toConfigResponse(configRepo.save(config));
    }

    @Transactional
    public Map<String, Object> eliminarConfiguracion(Long id) {
        configRepo.deleteById(id);
        return Map.of("message", "Configuracion eliminada");
    }

    // ── Plantillas ────────────────────────────────────────────────────────

    public List<EmailPlantillaResponse> listarPlantillas(String modulo) {
        if (modulo != null && !modulo.isBlank()) {
            return plantillaRepo.findByModuloOrderByNombreAsc(modulo).stream().map(this::toPlantillaResponse).toList();
        }
        return plantillaRepo.findAllByOrderByModuloAscNombreAsc().stream().map(this::toPlantillaResponse).toList();
    }

    public Optional<EmailPlantillaResponse> obtenerPlantilla(Long id) {
        return plantillaRepo.findById(id).map(this::toPlantillaResponse);
    }

    @Transactional
    public EmailPlantillaResponse guardarPlantilla(EmailPlantillaRequest request) {
        EmailPlantilla plantilla = new EmailPlantilla();
        plantilla.setModulo(request.modulo());
        plantilla.setNombre(request.nombre());
        plantilla.setAsunto(request.asunto());
        plantilla.setCuerpoHtml(request.cuerpoHtml());
        plantilla.setVariables(request.variables());
        plantilla.setActivo(request.activo() != null ? request.activo() : true);
        return toPlantillaResponse(plantillaRepo.save(plantilla));
    }

    @Transactional
    public EmailPlantillaResponse actualizarPlantilla(Long id, EmailPlantillaRequest request) {
        EmailPlantilla plantilla = plantillaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));
        plantilla.setModulo(request.modulo());
        plantilla.setNombre(request.nombre());
        plantilla.setAsunto(request.asunto());
        plantilla.setCuerpoHtml(request.cuerpoHtml());
        plantilla.setVariables(request.variables());
        plantilla.setActivo(request.activo() != null ? request.activo() : true);
        return toPlantillaResponse(plantillaRepo.save(plantilla));
    }

    @Transactional
    public Map<String, Object> eliminarPlantilla(Long id) {
        plantillaRepo.deleteById(id);
        return Map.of("message", "Plantilla eliminada");
    }

    @Transactional
    public EmailPlantillaResponse togglePlantilla(Long id) {
        EmailPlantilla plantilla = plantillaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));
        plantilla.setActivo(!plantilla.getActivo());
        return toPlantillaResponse(plantillaRepo.save(plantilla));
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private EmailConfiguracionResponse toConfigResponse(EmailConfiguracion c) {
        return new EmailConfiguracionResponse(
                c.getId(), c.getMetodo(), c.getSmtpHost(), c.getSmtpPort(),
                c.getSmtpUsername(), c.getSmtpUseTls(), c.getSmtpUseSsl(),
                c.getApiUrl(), c.getApiProvider(), c.getFromEmail(), c.getFromName(), c.getActivo()
        );
    }

    private EmailPlantillaResponse toPlantillaResponse(EmailPlantilla p) {
        return new EmailPlantillaResponse(
                p.getId(), p.getModulo(), p.getNombre(), p.getAsunto(),
                p.getCuerpoHtml(), p.getVariables(), p.getActivo()
        );
    }
}
