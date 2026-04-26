package com.sistema.base.api.core.Ubigeo;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ubigeos")
public class UbigeoController {

    private final UbigeoService ubigeoService;

    public UbigeoController(UbigeoService ubigeoService) {
        this.ubigeoService = ubigeoService;
    }

    // Endpoint 1: Carga los departamentos
    @GetMapping("/departamentos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> listarDepartamentos() {
        return ResponseEntity.ok(ubigeoService.getDepartamentos());
    }

    // Endpoint 2: Carga las provincias si le mandas el departamento
    @GetMapping("/provincias/{departamento}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> listarProvincias(@PathVariable String departamento) {
        return ResponseEntity.ok(ubigeoService.getProvincias(departamento));
    }

    // Endpoint 3: Carga los distritos (+ el IdUbigeo) si le mandas Dep y Prov
    @GetMapping("/distritos/{departamento}/{provincia}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Ubigeo>> listarDistritos(
            @PathVariable String departamento,
            @PathVariable String provincia) {
        return ResponseEntity.ok(ubigeoService.getDistritos(departamento, provincia));
    }

    @GetMapping("/{idUbigeo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Ubigeo> obtenerPorId(@PathVariable String idUbigeo) {
        return ResponseEntity.ok(ubigeoService.getUbigeoPorId(idUbigeo));
    }

}
