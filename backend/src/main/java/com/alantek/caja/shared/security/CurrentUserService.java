package com.alantek.caja.shared.security;

import com.alantek.caja.security.UserPrincipal;
import com.alantek.caja.shared.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUserService {

    public Optional<UserPrincipal> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public Long requireUserId() {
        return getCurrentUser()
                .map(UserPrincipal::id)
                .orElseThrow(() -> new BusinessException("Usuario no autenticado"));
    }

    public String requireUsername() {
        return getCurrentUser()
                .map(UserPrincipal::username)
                .orElseThrow(() -> new BusinessException("Usuario no autenticado"));
    }
}
