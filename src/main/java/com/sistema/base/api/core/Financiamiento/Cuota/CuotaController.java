package com.sistema.base.api.core.Financiamiento.Cuota;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cuotas")
@RequiredArgsConstructor
public class CuotaController {

    private final CuotaService cuotaService;

    @GetMapping("/contrato/{contratoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Cuota>> listarPorContrato(@PathVariable Long contratoId) {
        return ResponseEntity.ok(cuotaService.listarPorContrato(contratoId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_CUOTA')") // Si decides crear este permiso
    public ResponseEntity<Cuota> actualizar(@PathVariable Long id, @RequestBody Cuota cuota) {
        return ResponseEntity.ok(cuotaService.actualizar(id, cuota));
    }
}
