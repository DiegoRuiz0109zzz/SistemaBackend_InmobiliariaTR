package com.sistema.base.api.core.Financiamiento.Contrato;

import com.sistema.base.api.core.Financiamiento.Contrato.dtos.ContratoRequest;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.CuotaPreview;
import com.sistema.base.api.core.Financiamiento.Contrato.dtos.SimulacionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    // Endpoint 1: El Simulador que llama tu Frontend para pintar la tabla previa
    @PostMapping("/simular")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CuotaPreview>> simular(@RequestBody SimulacionRequest request) {
        return ResponseEntity.ok(contratoService.simularCronograma(request));
    }

    // Endpoint 2: Guardado real del Contrato y sus Cuotas
    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_CONTRATO')")
    public ResponseEntity<Contrato> crear(@RequestBody ContratoRequest request) {
        return ResponseEntity.ok(contratoService.generarContrato(request));
    }

    @GetMapping("/{id}/imprimir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> imprimirContrato(@PathVariable Long id) {
        byte[] pdfBytes = contratoService.generarDocumentoPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // "inline" permite que se abra en el navegador en lugar de descargarse automáticamente
        headers.setContentDispositionFormData("inline", "Documento_Contrato_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}