package com.alantek.caja.shared.audit;

import com.alantek.caja.modulo.seguridad.entity.Auditoria;
import com.alantek.caja.modulo.seguridad.repository.AuditoriaRepository;
import com.alantek.caja.shared.security.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditoriaRepository auditoriaRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public AuditService(AuditoriaRepository auditoriaRepository,
                        CurrentUserService currentUserService,
                        ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    public void registrar(String tabla, Long registroId, String accion, Object valorAnterior, Object valorNuevo) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuarioId(currentUserService.getCurrentUser().map(u -> u.id()).orElse(null));
        auditoria.setTablaAfectada(tabla);
        auditoria.setRegistroId(registroId);
        auditoria.setAccion(accion);
        auditoria.setValorAnterior(toJson(valorAnterior));
        auditoria.setValorNuevo(toJson(valorNuevo));
        auditoria.setIp(obtenerIp());
        auditoriaRepository.save(auditoria);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String obtenerIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = req.getRemoteAddr();
            }
            return ip;
        }
        return null;
    }
}
