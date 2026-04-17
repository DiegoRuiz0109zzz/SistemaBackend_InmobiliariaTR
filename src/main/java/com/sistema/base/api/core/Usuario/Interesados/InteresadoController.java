package com.sistema.base.api.core.Usuario.Interesados;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interesados")
@RequiredArgsConstructor
public class InteresadoController {

    private final InteresadoService interesadoService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Interesado>> listar() {
        return ResponseEntity.ok(interesadoService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Interesado> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(interesadoService.obtenerPorId(id));
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_INTERESADO')")
    public ResponseEntity<Interesado> crear(@RequestBody Interesado interesado) {
        return ResponseEntity.ok(interesadoService.guardar(interesado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_INTERESADO')")
    public ResponseEntity<Interesado> actualizar(@PathVariable Long id, @RequestBody Interesado interesado) {
        return ResponseEntity.ok(interesadoService.actualizar(id, interesado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_INTERESADO')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        interesadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
