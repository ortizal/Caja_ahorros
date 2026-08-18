package com.alantek.caja.modulo.seguridad.service;

import com.alantek.caja.modulo.seguridad.dto.AuditoriaResponse;
import com.alantek.caja.modulo.seguridad.dto.PermisoResponse;
import com.alantek.caja.modulo.seguridad.dto.RolResponse;
import com.alantek.caja.modulo.seguridad.entity.Auditoria;
import com.alantek.caja.modulo.seguridad.entity.Permiso;
import com.alantek.caja.modulo.seguridad.entity.Rol;
import com.alantek.caja.modulo.seguridad.repository.AuditoriaRepository;
import com.alantek.caja.modulo.seguridad.repository.PermisoRepository;
import com.alantek.caja.modulo.seguridad.repository.RolRepository;
import com.alantek.caja.shared.PageResponse;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final AuditService auditService;

    public RolService(RolRepository rolRepository,
                      PermisoRepository permisoRepository,
                      AuditoriaRepository auditoriaRepository,
                      AuditService auditService) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<RolResponse> listar(Pageable pageable) {
        Page<Rol> page = rolRepository.findAll(pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<PermisoResponse> listarPermisos(Pageable pageable) {
        Page<Permiso> page = permisoRepository.findAll(pageable);
        return PageResponse.of(page, p -> new PermisoResponse(p.getId(), p.getModulo(), p.getAccion()));
    }

    @Transactional
    public RolResponse asignarPermisos(Long rolId, Set<Long> permisoIds) {
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new BusinessException("Rol no encontrado: " + rolId));

        Set<Permiso> permisos = new HashSet<>();
        if (permisoIds != null) {
            for (Long permisoId : permisoIds) {
                permisos.add(permisoRepository.findById(permisoId)
                        .orElseThrow(() -> new BusinessException("Permiso no encontrado: " + permisoId)));
            }
        }
        rol.setPermisos(permisos);
        rolRepository.save(rol);
        auditService.registrar("roles", rolId, "EDITAR", null,
                permisos.stream().map(Permiso::getAuthority).toList());
        return toResponse(rol);
    }

    public PageResponse<AuditoriaResponse> listarAuditoria(String tabla, Instant desde, Instant hasta, Pageable pageable) {
        Instant desdeFinal = desde == null ? Instant.EPOCH : desde;
        Instant hastaFinal = hasta == null ? Instant.parse("2099-12-31T23:59:59Z") : hasta;
        Page<Auditoria> page = auditoriaRepository.filtrar(tabla, desdeFinal, hastaFinal, pageable);
        return PageResponse.of(page, this::toAuditoriaResponse);
    }

    private AuditoriaResponse toAuditoriaResponse(Auditoria a) {
        return new AuditoriaResponse(
                a.getId(), a.getUsuarioId(), a.getTablaAfectada(), a.getRegistroId(),
                a.getAccion(), a.getValorAnterior(), a.getValorNuevo(), a.getIp(), a.getCreatedAt());
    }

    private RolResponse toResponse(Rol rol) {
        return new RolResponse(
                rol.getId(),
                rol.getNombre(),
                rol.getDescripcion(),
                rol.getPermisos().stream()
                        .sorted(Comparator.comparing(Permiso::getModulo).thenComparing(Permiso::getAccion))
                        .map(p -> new PermisoResponse(p.getId(), p.getModulo(), p.getAccion()))
                        .toList());
    }
}
