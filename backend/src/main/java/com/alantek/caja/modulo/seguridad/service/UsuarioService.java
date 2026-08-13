package com.alantek.caja.modulo.seguridad.service;

import com.alantek.caja.modulo.seguridad.dto.UsuarioRequest;
import com.alantek.caja.modulo.seguridad.dto.UsuarioResponse;
import com.alantek.caja.modulo.seguridad.entity.Rol;
import com.alantek.caja.modulo.seguridad.entity.Usuario;
import com.alantek.caja.modulo.seguridad.repository.RolRepository;
import com.alantek.caja.modulo.seguridad.repository.UsuarioRepository;
import com.alantek.caja.shared.audit.AuditService;
import com.alantek.caja.shared.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder,
                          AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return toResponse(usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado: " + id)));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("La contraseña es obligatoria al crear un usuario");
        }
        if (usuarioRepository.findByUsername(request.username()).isPresent()) {
            throw new BusinessException("El username ya existe: " + request.username());
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setEmail(request.email());
        usuario.setRoles(cargarRoles(request.rolIds()));

        Usuario saved = usuarioRepository.save(usuario);
        auditService.registrar("usuarios", saved.getId(), "CREAR", null, request);
        return toResponse(saved);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado: " + id));

        String usuarioAnterior = usuario.getUsername();
        if (request.username() != null && !request.username().isBlank() && !request.username().equals(usuario.getUsername())) {
            if (usuarioRepository.findByUsername(request.username()).isPresent()) {
                throw new BusinessException("El username ya existe: " + request.username());
            }
            usuario.setUsername(request.username());
        }
        if (request.nombreCompleto() != null && !request.nombreCompleto().isBlank()) {
            usuario.setNombreCompleto(request.nombreCompleto());
        }
        if (request.email() != null) {
            usuario.setEmail(request.email());
        }
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.rolIds() != null) {
            usuario.setRoles(cargarRoles(request.rolIds()));
        }

        Usuario saved = usuarioRepository.save(usuario);
        auditService.registrar("usuarios", saved.getId(), "EDITAR", usuarioAnterior, request);
        return toResponse(saved);
    }

    @Transactional
    public void cambiarEstado(Long id, String estado) {
        String estadoFinal = estado == null ? "ACTIVO" : estado.toUpperCase();
        if (!List.of("ACTIVO", "BLOQUEADO", "INACTIVO").contains(estadoFinal)) {
            throw new BusinessException("Estado inválido: " + estado);
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado: " + id));
        String anterior = usuario.getEstado();
        usuario.setEstado(estadoFinal);
        usuarioRepository.save(usuario);
        auditService.registrar("usuarios", id, "EDITAR", anterior, estadoFinal);
    }

    private Set<Rol> cargarRoles(Set<Long> rolIds) {
        Set<Rol> roles = new HashSet<>();
        if (rolIds != null) {
            for (Long rolId : rolIds) {
                roles.add(rolRepository.findById(rolId)
                        .orElseThrow(() -> new BusinessException("Rol no encontrado: " + rolId)));
            }
        }
        return roles;
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                usuario.getEstado(),
                usuario.getRoles().stream().map(Rol::getNombre).sorted().toList(),
                usuario.getUltimoAcceso(),
                usuario.getCreatedAt());
    }
}
