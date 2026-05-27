package com.sistema.base.api.core.Financiamiento.Pago;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Pago>> listarPagos() {
        return ResponseEntity.ok(pagoService.listarPagos());
    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREAR_PAGO')")
    public ResponseEntity<List<Pago>> registrar( // ✅ Cambiado de Pago a List<Pago>
                                                 @RequestParam("cuotaId") Long cuotaId,
                                                 @RequestParam("montoAbonado") Double montoAbonado,
                                                 @RequestParam("metodoPago") String metodoPago,
                                                 @RequestParam(value = "numeroOperacion", required = false) String numeroOperacion,
                                                 @RequestParam(value = "descripcion", required = false) String descripcion,
                                                 @RequestParam(value = "voucher", required = false) MultipartFile voucherFile) {

        return ResponseEntity.ok(pagoService.registrarPago(cuotaId, montoAbonado, metodoPago, numeroOperacion, descripcion, voucherFile));
    }

    @PutMapping(value = "/{id}/procesar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROCESAR_PAGO')")
    public ResponseEntity<List<Pago>> procesarPendiente( // ✅ Cambiado de Pago a List<Pago>
                                                         @PathVariable Long id,
                                                         @RequestParam("metodoPago") String metodoPago,
                                                         @RequestParam(value = "numeroOperacion", required = false) String numeroOperacion,
                                                         @RequestParam(value = "descripcion", required = false) String descripcion,
                                                         @RequestParam(value = "voucher", required = false) MultipartFile voucherFile) {

        List<Pago> pagosProcesados = pagoService.procesarPagoPendiente(id, metodoPago, numeroOperacion, descripcion, voucherFile);
        return ResponseEntity.ok(pagosProcesados);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_PAGO')")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        pagoService.anularPago(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // NUEVO ENDPOINT: Recalcular días de retraso de un contrato modificado en BD
    // =========================================================================
    @PostMapping("/recalcular-atrasos/contrato/{contratoId}")
    @PreAuthorize("hasAuthority('PROCESAR_PAGO')")
    public ResponseEntity<String> recalcularAtrasos(@PathVariable Long contratoId) {
        pagoService.recalcularAtrasosPorContrato(contratoId);
        return ResponseEntity.ok("Sincronización y recálculo de días de retraso completado para el contrato N°: " + contratoId);
    }

    @GetMapping("/{id}/nota-venta")
    @PreAuthorize("isAuthenticated()") // O el permiso específico que desees
    public ResponseEntity<byte[]> descargarNotaVenta(@PathVariable Long id) {

        // Llamamos al método que construimos en el PagoService
        byte[] pdfBytes = pagoService.generarNotaVentaPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        // "inline" indica al navegador que debe intentar mostrar el PDF en una pestaña nueva, en lugar de forzar la descarga como un archivo
        headers.setContentDispositionFormData("inline", "Nota_Venta_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}