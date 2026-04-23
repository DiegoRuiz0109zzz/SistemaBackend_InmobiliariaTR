package com.sistema.base.api.core.Financiamiento.Pago;

import com.sistema.base.api.core.Financiamiento.Pago.dtos.PagoRequest;
import com.sistema.base.api.core.Financiamiento.Pago.dtos.ProcesarPagoRequest;
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

    @PutMapping("/{id}/procesar")
    @PreAuthorize("hasAuthority('PROCESAR_PAGO')") // O el permiso que use tu rol de Caja
    public ResponseEntity<Pago> procesarPendiente(
            @PathVariable Long id,
            @RequestBody ProcesarPagoRequest request) {

        Pago pagoProcesado = pagoService.procesarPagoPendiente(
                id,
                request.getMetodoPago(),
                request.getNumeroOperacion(),
                request.getFotoVoucherUrl()
        );
        return ResponseEntity.ok(pagoProcesado);
    }
}
