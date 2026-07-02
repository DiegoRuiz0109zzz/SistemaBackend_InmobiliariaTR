package com.sistema.base.api.core.Financiamiento.Pago;

import com.sistema.base.api.core.Financiamiento.Pago.Sunat.PagoSunatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
    public ResponseEntity<Page<Pago>> listarPagos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) TipoComprobante tipoComprobante,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        Page<Pago> pagosPaginados = pagoService.listarPagosPaginadosConFiltros(
                page, size, metodoPago, tipoComprobante, estado, fechaDesde, fechaHasta
        );

        return ResponseEntity.ok(pagosPaginados);
    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREAR_PAGO')")
    public ResponseEntity<List<Pago>> registrar(
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
    public ResponseEntity<List<Pago>> procesarPendiente(
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

    @PostMapping("/recalcular-atrasos/contrato/{contratoId}")
    @PreAuthorize("hasAuthority('PROCESAR_PAGO')")
    public ResponseEntity<String> recalcularAtrasos(@PathVariable Long contratoId) {
        pagoService.recalcularAtrasosPorContrato(contratoId);
        return ResponseEntity.ok("Sincronización y recálculo de días de retraso completado para el contrato N°: " + contratoId);
    }

    @GetMapping("/{id}/nota-venta")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargarNotaVenta(@PathVariable Long id) {
        byte[] pdfBytes = pagoService.generarNotaVentaPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Nota_Abono_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/comprobante/{numeroComprobante}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargarNotaAbonoPdf(@PathVariable String numeroComprobante) {
        byte[] pdfBytes = pagoService.generarNotaAbonoMultiPdf(numeroComprobante);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Nota_Abono_" + numeroComprobante + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/recibo/{numeroComprobante}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargarReciboIngresoPdf(@PathVariable String numeroComprobante) {
        byte[] pdfBytes = pagoService.generarReciboIngresoPdf(numeroComprobante);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Recibo_Ingreso_" + numeroComprobante + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PostMapping(value = "/conciliar/{numeroRecibo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROCESAR_PAGO')")
    public ResponseEntity<List<Pago>> conciliarCaja(
            @PathVariable String numeroRecibo,
            @RequestParam("bancos") List<String> bancos,
            @RequestParam("operaciones") List<String> operaciones,
            @RequestParam("montos") List<Double> montos,
            @RequestParam(value = "vouchers", required = false) List<MultipartFile> vouchers) {

        List<Pago> pagosOficiales = pagoService.conciliarReciboCaja(numeroRecibo, bancos, operaciones, montos, vouchers);
        return ResponseEntity.ok(pagosOficiales);
    }

    @PostMapping(value = "/recibo/{numeroRecibo}/firmado", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROCESAR_PAGO')")
    public ResponseEntity<String> subirReciboFirmado(
            @PathVariable String numeroRecibo,
            @RequestParam("archivo") org.springframework.web.multipart.MultipartFile archivoFirmado) {

        pagoService.subirReciboFirmado(numeroRecibo, archivoFirmado);
        return ResponseEntity.ok("Documento firmado subido y enlazado correctamente al historial del contrato.");
    }

    @GetMapping("/reporte-caja")
    @PreAuthorize("isAuthenticated()") // O usa "hasAuthority('VER_REPORTES')"
    public ResponseEntity<java.util.Map<String, Object>> obtenerReporteCaja() {
        return ResponseEntity.ok(pagoService.obtenerReporteCajaFisica());
    }

//    @PostMapping("/{pagoId}/emitir-sunat")
//    @PreAuthorize("hasAuthority('PROCESAR_PAGO')")
//    public ResponseEntity<Pago> emitirSunat(
//            @PathVariable Long pagoId,
//            @RequestParam TipoComprobante tipoComprobante, // Ej: BOLETA o FACTURA
//            @RequestParam String serie) {                  // Ej: B001 o F001
//
//        Pago pagoActualizado = pagoService.emitirComprobanteSunat(pagoId, tipoComprobante, serie);
//        return ResponseEntity.ok(pagoActualizado);
//    }

    @PostMapping("/facturacion/registrar-pago")
    @PreAuthorize("hasAuthority('CREAR_PAGO')")
    public ResponseEntity<Pago> registrarPagoYEmitirSunatJson(@RequestBody PagoSunatRequest request) {

        Pago pagoRealizado = pagoService.registrarPagoYEmitirSunat(
                request.getCuotaId(),
                request.getMontoAbonado(),
                request.getMetodoPago(),
                request.getDescripcion(),
                request.getTipoComprobante(),
                request.getSerie(),
                request.getTipoIgv(),
                request.getTipoDoc(),
                // ✅ AQUÍ ESTÁN LOS 3 ARGUMENTOS FALTANTES
                request.getRuc(),
                request.getRazonSocial(),
                request.getDireccionFactura()
        );

        return ResponseEntity.ok(pagoRealizado);
    }

}