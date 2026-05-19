package com.sistema.base.api.core.Financiamiento.Comision;

import com.sistema.base.api.core.Financiamiento.Comision.dtos.ActualizarPagoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comisiones")
@RequiredArgsConstructor
public class ComisionController {

    private final ComisionService comisionService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Comision>> listarTodas() {
        return ResponseEntity.ok(comisionService.listarTodas());
    }

    @GetMapping("/contrato/{contratoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Comision>> listarPorContrato(@PathVariable Long contratoId) {
        return ResponseEntity.ok(comisionService.listarPorContrato(contratoId));
    }

    @PutMapping("/{id}/cambiar-estado")
    @PreAuthorize("hasAuthority('PAGAR_COMISION')")
    public ResponseEntity<Comision> cambiarEstadoPago(
            @PathVariable Long id,
            @RequestBody ActualizarPagoRequest request) {
        try {
            // Convertimos el String del frontend al ENUM de la base de datos de manera segura
            EstadoComision nuevoEstado = EstadoComision.valueOf(request.getEstadoPago().toUpperCase());

            Comision comisionActualizada = comisionService.actualizarEstadoPago(
                    id,
                    nuevoEstado,
                    request.getObservacionPago()
            );

            return ResponseEntity.ok(comisionActualizada);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado de pago no válido. Use: PENDIENTE, PAGADO o ANULADO.");
        }
    }
}