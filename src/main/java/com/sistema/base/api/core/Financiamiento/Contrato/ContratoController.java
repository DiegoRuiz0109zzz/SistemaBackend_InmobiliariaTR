package com.sistema.base.api.core.Financiamiento.Contrato;

import com.sistema.base.api.core.Financiamiento.Contrato.dtos.ContratoRequest;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.CuotaPreview;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.SimulacionRequest;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.SimulacionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // ✅ NUEVO IMPORT PARA ARCHIVOS

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Contrato>> listar() {
        return ResponseEntity.ok(contratoService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Contrato> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.obtenerPorId(id));
    }

    @PostMapping("/simular")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SimulacionResponseDTO> simularCronograma(@RequestBody SimulacionRequest request) {
        return ResponseEntity.ok(contratoService.simularCronograma(request));
    }

    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_CONTRATO')")
    public ResponseEntity<Contrato> crear(@RequestBody ContratoRequest request) {
        return ResponseEntity.ok(contratoService.generarContrato(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_CONTRATO')")
    public ResponseEntity<Contrato> actualizar(@PathVariable Long id, @RequestBody ContratoRequest request) {
        return ResponseEntity.ok(contratoService.actualizarContrato(id, request));
    }

    @PostMapping(value = "/{id}/subir-documento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDITAR_CONTRATO')") // O el permiso que consideres adecuado
    public ResponseEntity<?> subirDocumentoFirmado(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("motivo") String motivo) {
        try {
            Contrato contratoActualizado = contratoService.subirDocumentoFirmado(id, archivo, motivo);
            return ResponseEntity.ok(contratoActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/registrar-hito-oficial")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> registrarHitoOficial(@PathVariable Long id) {
        try {
            String observacion = "Registro oficial de contrato en bitácora.";
            Contrato contratoActualizado = contratoService.generarNuevoDocumentoContrato(id, observacion);

            return ResponseEntity.ok(contratoActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/vista-previa")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> vistaPreviaContrato(@PathVariable Long id) {
        byte[] pdfBytes = contratoService.generarVistaPreviaPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Vista_Previa_Contrato_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PutMapping("/{id}/completar-medidas")
    @PreAuthorize("hasAuthority('EDITAR_CONTRATO')")
    public ResponseEntity<Contrato> completarMedidas(@PathVariable Long id, @RequestBody ContratoRequest request) {
        return ResponseEntity.ok(contratoService.registrarMedidasYPerimetro(id, request));
    }

    @GetMapping("/{id}/alerta-separacion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> obtenerAlertaSeparacion(@PathVariable Long id) {
        String alerta = contratoService.obtenerAlertaSeparacionVencida(id);

        if (alerta != null) {
            return ResponseEntity.ok(Map.of("mensaje", alerta));
        } else {
            return ResponseEntity.ok(Map.of());
        }
    }

    @GetMapping("/{id}/acta-traspaso-titular")
    @PreAuthorize("hasAuthority('EDITAR_CONTRATO')")
    public ResponseEntity<byte[]> descargarActaTraspasoTitular(
            @PathVariable Long id,
            @RequestParam("anteriorClienteId") Long anteriorClienteId,
            @RequestParam("nuevoClienteId") Long nuevoClienteId) {

        byte[] pdfBytes = contratoService.generarActaTraspasoTitularPdf(id, anteriorClienteId, nuevoClienteId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Acta_Traspaso_Titular_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/acta-cambio-lote")
    @PreAuthorize("hasAuthority('EDITAR_CONTRATO')")
    public ResponseEntity<byte[]> descargarActaCambioLote(
            @PathVariable Long id,
            @RequestParam("loteOrigenId") Long loteOrigenId,
            @RequestParam("loteDestinoId") Long loteDestinoId) {

        byte[] pdfBytes = contratoService.generarActaCambioLotePdf(id, loteOrigenId, loteDestinoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Acta_Cambio_Lote_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/documento-firmado")
    @PreAuthorize("isAuthenticated()") // O el permiso que consideres adecuado
    public ResponseEntity<byte[]> verDocumentoFirmado(@PathVariable Long id) {
        try {
            byte[] pdfBytes = contratoService.obtenerDocumentoFirmadoPdf(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // "inline" hace que se visualice en la pantalla sin forzar descarga
            headers.setContentDispositionFormData("inline", "Contrato_Firmado_" + id + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            // Si el archivo no existe, retornamos un 404 Not Found limpio
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}