package com.sistema.base.api.core.Lotizacion.Lote;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final LoteService loteService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Lote>> listar() {
        return ResponseEntity.ok(loteService.listarTodosActivos());
    }

    // Ruta en cascada
    @GetMapping("/manzana/{manzanaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Lote>> listarPorManzana(@PathVariable Long manzanaId) {
        return ResponseEntity.ok(loteService.listarPorManzana(manzanaId));
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_LOTE')")
    public ResponseEntity<Lote> crear(@RequestBody Lote lote) {
        return ResponseEntity.ok(loteService.guardar(lote));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_LOTE')")
    public ResponseEntity<Lote> actualizar(@PathVariable Long id, @RequestBody Lote lote) {
        return ResponseEntity.ok(loteService.actualizar(id, lote));
    }

    @GetMapping("/calcular-costo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> calcularCosto(
            @RequestParam Double area,
            @RequestParam Double precioMetroCuadrado) {

        Double precioCosto = loteService.calcularCostoBase(area, precioMetroCuadrado);

        // Devolvemos un JSON simple con la respuesta
        return ResponseEntity.ok(java.util.Map.of(
                "area", area,
                "precioMetroCuadrado", precioMetroCuadrado,
                "precioCosto", precioCosto
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_LOTE')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        loteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
