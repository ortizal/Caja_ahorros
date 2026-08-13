package com.alantek.caja.security;

import com.alantek.caja.modulo.seguridad.entity.Permiso;
import com.alantek.caja.modulo.seguridad.entity.Rol;
import com.alantek.caja.modulo.seguridad.entity.Usuario;
import com.alantek.caja.modulo.seguridad.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        Set<String> permisos = usuario.getRoles().stream()
                .flatMap(r -> r.getPermisos().stream())
                .map(Permiso::getAuthority)
                .collect(Collectors.toSet());

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (String permiso : permisos) {
            authorities.add(new SimpleGrantedAuthority(permiso));
        }
        for (Rol rol : usuario.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.getNombre()));
        }

        return new UserPrincipal(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPasswordHash(),
                usuario.getNombreCompleto(),
                permisos,
                authorities);
    }
}
