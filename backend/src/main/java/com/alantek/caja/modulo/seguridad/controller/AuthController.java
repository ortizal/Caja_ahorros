package com.alantek.caja.modulo.seguridad.controller;

import com.alantek.caja.modulo.seguridad.dto.LoginRequest;
import com.alantek.caja.modulo.seguridad.dto.LoginResponse;
import com.alantek.caja.modulo.seguridad.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpReq) {
        return ResponseEntity.ok(authService.login(request, httpReq));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(authService.refresh(stripBearer(authorization)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(stripBearer(authorization));
        return ResponseEntity.noContent().build();
    }

    private String stripBearer(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }
}
