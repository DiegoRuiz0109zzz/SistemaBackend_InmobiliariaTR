package com.sistema.base.api.core.Lotizacion.Urbanizacion;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/urbanizaciones")
@RequiredArgsConstructor
public class UrbanizacionController {

    private final UrbanizacionService urbanizacionService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Urbanizacion>> listar() {
        return ResponseEntity.ok(urbanizacionService.listarTodosActivos());}

    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Urbanizacion>> listarPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(urbanizacionService.listarPaginado(page, size, search));
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_URBANIZACION')")
    public ResponseEntity<Urbanizacion> crear(@RequestBody Urbanizacion urbanizacion) {
        return ResponseEntity.ok(urbanizacionService.guardar(urbanizacion));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_URBANIZACION')")
    public ResponseEntity<Urbanizacion> actualizar(@PathVariable Long id, @RequestBody Urbanizacion urbanizacion) {
        return ResponseEntity.ok(urbanizacionService.actualizar(id, urbanizacion));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_URBANIZACION')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        urbanizacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
