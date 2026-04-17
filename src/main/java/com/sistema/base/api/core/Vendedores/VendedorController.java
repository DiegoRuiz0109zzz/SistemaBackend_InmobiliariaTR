package com.sistema.base.api.core.Vendedores;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vendedores")
@RequiredArgsConstructor
public class VendedorController {

    private final VendedorService vendedorService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Vendedor>> listar() {
        return ResponseEntity.ok(vendedorService.listarTodosActivos());
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_VENDEDOR')")
    public ResponseEntity<Vendedor> crear(@RequestBody Vendedor vendedor) {
        return ResponseEntity.ok(vendedorService.guardar(vendedor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_VENDEDOR')")
    public ResponseEntity<Vendedor> actualizar(@PathVariable Long id, @RequestBody Vendedor vendedor) {
        return ResponseEntity.ok(vendedorService.actualizar(id, vendedor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_VENDEDOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        vendedorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
