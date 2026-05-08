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
    public ResponseEntity<List<CuotaPreview>> simular(@RequestBody SimulacionRequest request) {
        return ResponseEntity.ok(contratoService.simularCronograma(request));
    }

    // 1. CREA EL CONTRATO EN BASE DE DATOS (Y genera Hito Automático en Bitácora)
    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_CONTRATO')")
    public ResponseEntity<Contrato> crear(@RequestBody ContratoRequest request) {
        return ResponseEntity.ok(contratoService.generarContrato(request));
    }

    // ✅ NUEVO: ENDPOINT PARA ACTUALIZAR DATOS (Detecta cambios y genera hito de MODIFICACIÓN)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_CONTRATO')") // Asegúrate de tener este permiso o usa isAuthenticated()
    public ResponseEntity<Contrato> actualizar(@PathVariable Long id, @RequestBody ContratoRequest request) {
        return ResponseEntity.ok(contratoService.actualizarContrato(id, request));
    }

    // 2. ENDPOINT QUE REGISTRA EL HITO OFICIAL Y ACTUALIZA LA FECHA (Reemplaza a generar-documento)
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

    // 3. LA VISTA PREVIA SIGUE SIENDO EL MOTOR PARA VER EL PDF AL VUELO
    @GetMapping("/{id}/vista-previa")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> vistaPreviaContrato(@PathVariable Long id) {
        byte[] pdfBytes = contratoService.generarVistaPreviaPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // "inline" permite que se abra en el navegador sin descargarlo obligatoriamente
        headers.setContentDispositionFormData("inline", "Vista_Previa_Contrato_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}