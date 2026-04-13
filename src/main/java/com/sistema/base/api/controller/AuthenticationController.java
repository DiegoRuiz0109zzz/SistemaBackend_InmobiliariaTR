package com.sistema.base.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sistema.base.api.model.AuthenticationRequest;
import com.sistema.base.api.model.AuthenticationResponse;
import com.sistema.base.api.model.RegisterRequest;
import com.sistema.base.api.service.AuthenticationService;
import com.sistema.base.api.service.RateLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;
    private final RateLimitService rateLimitService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(
            @RequestBody AuthenticationRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = getClientIp(httpRequest);
        
        // Verificar rate limit
        if (!rateLimitService.isAllowed(clientIp)) {
            long secondsRemaining = rateLimitService.getSecondsUntilReset(clientIp);
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(java.util.Map.of(
                            "error", "Demasiados intentos de login",
                            "message", "Has excedido el límite de intentos. Intenta nuevamente en " + secondsRemaining + " segundos.",
                            "retryAfter", secondsRemaining
                    ));
        }
        
        try {
            AuthenticationResponse response = service.authenticate(request);
            
            // Crear cookie HttpOnly con el token
            ResponseCookie jwtCookie = ResponseCookie.from("jwt", response.getToken())
                    .httpOnly(true)
                    .secure(false) // Cambiar a true en producción (HTTPS)
                    .path("/")
                    .maxAge(9 * 60 * 60) // 9 horas, igual que el token
                    .sameSite("Strict")
                    .build();

            // Login exitoso: resetear contador
            rateLimitService.resetAttempts(clientIp);
            
            // Eliminar el token del objeto de respuesta para mayor seguridad
            response.setToken(null);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(response);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of(
                            "error", "Credenciales inválidas",
                            "message", "Usuario o contraseña incorrectos"
                    ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(java.util.Map.of("message", "Sesión cerrada exitosamente"));
    }

    @PostMapping("/recover-password")
    public ResponseEntity<String> recoverPassword(@RequestBody java.util.Map<String, String> request) {
        try {
            String message = service.recoverPassword(
                request.get("username"),
                request.get("docIdentidad"),
                request.get("telefono")
            );
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}

