package com.sistema.base.api.core.Lotizacion.Manzana;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/manzanas")
@RequiredArgsConstructor
public class ManzanaController {

    private final ManzanaService manzanaService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Manzana>> listar() {
        return ResponseEntity.ok(manzanaService.listarTodosActivos());
    }

    // Ruta en cascada
    @GetMapping("/etapa/{etapaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Manzana>> listarPorEtapa(@PathVariable Long etapaId) {
        return ResponseEntity.ok(manzanaService.listarPorEtapa(etapaId));
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_MANZANA')")
    public ResponseEntity<Manzana> crear(@RequestBody Manzana manzana) {
        return ResponseEntity.ok(manzanaService.guardar(manzana));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_MANZANA')")
    public ResponseEntity<Manzana> actualizar(@PathVariable Long id, @RequestBody Manzana manzana) {
        return ResponseEntity.ok(manzanaService.actualizar(id, manzana));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_MANZANA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        manzanaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
