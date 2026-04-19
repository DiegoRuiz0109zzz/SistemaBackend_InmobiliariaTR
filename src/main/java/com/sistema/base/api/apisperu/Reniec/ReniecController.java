package com.sistema.base.api.apisperu.Reniec;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reniec")
@RequiredArgsConstructor
public class ReniecController {

    private final ReniecService reniecService;

    @GetMapping("/consulta/{dni}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> consultarDNI(@PathVariable String dni) {
        try {
            Map<String, Object> datos = reniecService.consultarDNI(dni);
            return ResponseEntity.ok(Map.of("success", true, "data", datos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
