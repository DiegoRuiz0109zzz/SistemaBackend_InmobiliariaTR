package com.sistema.base.api.core.Financiamiento.Pago;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    // ACEPTAR MULTIPART PARA REGISTRAR PAGO DIRECTO
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREAR_PAGO')")
    public ResponseEntity<Pago> registrar(
            @RequestParam("cuotaId") Long cuotaId,
            @RequestParam("montoAbonado") Double montoAbonado,
            @RequestParam("metodoPago") String metodoPago,
            @RequestParam(value = "numeroOperacion", required = false) String numeroOperacion,
            @RequestParam(value = "voucher", required = false) MultipartFile voucherFile) {

        return ResponseEntity.ok(pagoService.registrarPago(cuotaId, montoAbonado, metodoPago, numeroOperacion, voucherFile));
    }

    // ACEPTAR MULTIPART PARA PROCESAR PAGO PENDIENTE
    @PutMapping(value = "/{id}/procesar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROCESAR_PAGO')")
    public ResponseEntity<Pago> procesarPendiente(
            @PathVariable Long id,
            @RequestParam("metodoPago") String metodoPago,
            @RequestParam(value = "numeroOperacion", required = false) String numeroOperacion,
            @RequestParam(value = "voucher", required = false) MultipartFile voucherFile) {

        Pago pagoProcesado = pagoService.procesarPagoPendiente(id, metodoPago, numeroOperacion, voucherFile);
        return ResponseEntity.ok(pagoProcesado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_PAGO')")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        pagoService.anularPago(id);
        return ResponseEntity.noContent().build();
    }
}