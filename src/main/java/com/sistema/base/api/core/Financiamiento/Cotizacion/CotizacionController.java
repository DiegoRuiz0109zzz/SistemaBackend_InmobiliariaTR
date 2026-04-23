package com.sistema.base.api.core.Financiamiento.Cotizacion;

import com.sistema.base.api.core.Financiamiento.Cotizacion.dtos.CotizacionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cotizaciones")
@RequiredArgsConstructor
public class CotizacionController {

    private final CotizacionService cotizacionService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Cotizacion>> listar() {
        cotizacionService.verificarExpiradas(); // Actualiza estados antes de devolver
        return ResponseEntity.ok(cotizacionService.listarTodas());
    }

    @GetMapping("/buscar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Cotizacion>> buscarPorDni(@RequestParam String dni) {
        cotizacionService.verificarExpiradas();
        return ResponseEntity.ok(cotizacionService.buscarPorDniInteresado(dni));
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_CONTRATO')")
    public ResponseEntity<Cotizacion> crear(@RequestBody CotizacionRequest request) {
        return ResponseEntity.ok(cotizacionService.crearCotizacion(request));
    }

    // Este endpoint se llamará cuando el cliente regrese a comprar
    @PutMapping("/{id}/convertir")
    @PreAuthorize("hasAuthority('EDITAR_CONTRATO')")
    public ResponseEntity<Cotizacion> convertirAContrato(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.marcarComoConvertida(id));
    }
}