package com.sistema.base.api.core.Financiamiento.Pago.Serie;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class SerieController {

    private final SerieService serieService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Serie>> listar() {
        return ResponseEntity.ok(serieService.listarTodas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Serie> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(serieService.obtenerPorId(id));
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_SERIE')")
    public ResponseEntity<Serie> crear(@RequestBody Serie serie) {
        return ResponseEntity.ok(serieService.guardar(serie));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_SERIE')")
    public ResponseEntity<Serie> actualizar(@PathVariable Long id, @RequestBody Serie serie) {
        return ResponseEntity.ok(serieService.actualizar(id, serie));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_SERIE')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        serieService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-activo")
    @PreAuthorize("hasAuthority('EDITAR_SERIE')")
    public ResponseEntity<Serie> toggleActivo(@PathVariable Long id, @RequestParam boolean activo) {
        return ResponseEntity.ok(serieService.cambiarEstadoActivo(id, activo));
    }
}
