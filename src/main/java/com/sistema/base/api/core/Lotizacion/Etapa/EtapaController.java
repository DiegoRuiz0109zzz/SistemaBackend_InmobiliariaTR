package com.sistema.base.api.core.Lotizacion.Etapa;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/etapas")
@RequiredArgsConstructor
public class EtapaController {

    private final EtapaService etapaService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Etapa>> listar() {
        return ResponseEntity.ok(etapaService.listarTodosActivos());
    }

    // Ruta en cascada
    @GetMapping("/urbanizacion/{urbanizacionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Etapa>> listarPorUrbanizacion(@PathVariable Long urbanizacionId) {
        return ResponseEntity.ok(etapaService.listarPorUrbanizacion(urbanizacionId));
    }

    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Etapa>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long urbanizacionId) { // Recibe el ID del Dropdown
        return ResponseEntity.ok(etapaService.listarPaginado(page, size, search, urbanizacionId));
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_ETAPA')")
    public ResponseEntity<Etapa> crear(@RequestBody Etapa etapa) {
        return ResponseEntity.ok(etapaService.guardar(etapa));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_ETAPA')")
    public ResponseEntity<Etapa> actualizar(@PathVariable Long id, @RequestBody Etapa etapa) {
        return ResponseEntity.ok(etapaService.actualizar(id, etapa));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_ETAPA')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        etapaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
