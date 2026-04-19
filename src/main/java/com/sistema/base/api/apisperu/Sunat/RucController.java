package com.sistema.base.api.apisperu.Sunat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ruc")
@RequiredArgsConstructor
public class RucController {

    private final RucService rucService;

    @GetMapping("/consulta/{ruc}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> consultarRUC(@PathVariable String ruc) {
        try {
            Map<String, Object> datos = rucService.consultarRUC(ruc);
            return ResponseEntity.ok(Map.of("success", true, "data", datos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
