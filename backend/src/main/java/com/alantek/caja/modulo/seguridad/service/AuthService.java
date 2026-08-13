package com.alantek.caja.modulo.seguridad.service;

import com.alantek.caja.modulo.seguridad.dto.LoginRequest;
import com.alantek.caja.modulo.seguridad.dto.LoginResponse;
import com.alantek.caja.modulo.seguridad.entity.Rol;
import com.alantek.caja.modulo.seguridad.entity.Sesion;
import com.alantek.caja.modulo.seguridad.entity.Usuario;
import com.alantek.caja.modulo.seguridad.repository.SesionRepository;
import com.alantek.caja.modulo.seguridad.repository.UsuarioRepository;
import com.alantek.caja.security.CustomUserDetailsService;
import com.alantek.caja.security.JwtService;
import com.alantek.caja.security.UserPrincipal;
import com.alantek.caja.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final SesionRepository sesionRepository;
    private final CustomUserDetailsService userDetailsService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UsuarioRepository usuarioRepository,
                       SesionRepository sesionRepository,
                       CustomUserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.sesionRepository = sesionRepository;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpReq) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw new BusinessException("El usuario está " + usuario.getEstado().toLowerCase());
        }

        String token = jwtService.generateToken(principal);

        usuario.setUltimoAcceso(Instant.now());
        usuarioRepository.save(usuario);

        Sesion sesion = new Sesion();
        sesion.setUsuarioId(usuario.getId());
        sesion.setToken(token);
        sesion.setIp(obtenerIp(httpReq));
        sesion.setUserAgent(truncate(httpReq.getHeader("User-Agent"), 255));
        sesionRepository.save(sesion);

        Set<String> roles = usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet());

        return new LoginResponse(token, usuario.getId(), usuario.getUsername(),
                usuario.getNombreCompleto(), roles, principal.permisos());
    }

    @Transactional
    public LoginResponse refresh(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Token no proporcionado");
        }
        String username = jwtService.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isValid(token, userDetails)) {
            throw new BusinessException("Token inválido o expirado");
        }
        UserPrincipal principal = (UserPrincipal) userDetails;
        Usuario usuario = usuarioRepository.findByUsername(principal.getUsername()).orElseThrow();
        String nuevoToken = jwtService.generateToken(principal);
        Set<String> roles = usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet());
        return new LoginResponse(nuevoToken, usuario.getId(), usuario.getUsername(),
                usuario.getNombreCompleto(), roles, principal.permisos());
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        sesionRepository.findByToken(token).ifPresent(sesion -> {
            sesion.setCerradaAt(Instant.now());
            sesionRepository.save(sesion);
        });
    }

    private String obtenerIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
