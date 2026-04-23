package com.sistema.base.api.core.Financiamiento.Pago;

import com.sistema.base.api.core.Financiamiento.Pago.dtos.PagoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @GetMapping("/cuota/{cuotaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Pago>> listarPorCuota(@PathVariable Long cuotaId) {
        return ResponseEntity.ok(pagoService.listarPorCuota(cuotaId));
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_PAGO')")
    public ResponseEntity<Pago> registrar(@RequestBody PagoRequest request) {
        return ResponseEntity.ok(pagoService.registrarPago(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_PAGO')")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        pagoService.anularPago(id);
        return ResponseEntity.noContent().build();
    }
}
