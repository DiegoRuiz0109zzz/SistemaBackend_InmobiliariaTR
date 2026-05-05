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
    public ResponseEntity<?> simular(@RequestBody SimulacionRequest request) {
        try {
            List<CuotaPreview> cronograma = contratoService.simularCronograma(request);
            return ResponseEntity.ok(cronograma);
        } catch (IllegalArgumentException e) {
            // Capturamos el error inteligente que armamos en el Service
            // y lo devolvemos como un 400 Bad Request en formato JSON
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Para cualquier otro error inesperado
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Ocurrió un error inesperado durante la simulación."));
        }
    }

    @PostMapping("/simular/resumen")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> simularResumen(@RequestBody SimulacionRequest request) {
        try {
            Map<String, Object> resumen = contratoService.calcularResumenSimulacion(request);
            // Respondemos 200 OK con exito = true
            return ResponseEntity.ok(Map.of(
                    "exito", true,
                    "datos", resumen
            ));
        } catch (IllegalArgumentException e) {
            // Respondemos 200 OK, pero con exito = false y el mensaje de alerta
            return ResponseEntity.ok(Map.of(
                    "exito", false,
                    "mensaje", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("exito", false, "mensaje", "Error crítico en el servidor."));
        }
    }

    // 1. CREA EL CONTRATO EN BASE DE DATOS (Sin PDF todavía)
    @PostMapping("/")
    @PreAuthorize("hasAuthority('CREAR_CONTRATO')")
    public ResponseEntity<Contrato> crear(@RequestBody ContratoRequest request) {
        return ResponseEntity.ok(contratoService.generarContrato(request));
    }

    // 2. ENDPOINT QUE GENERA EL PDF, EL HISTORIAL Y ACTUALIZA LA FECHA (Solo 1 vez por estado)
    @PostMapping("/{id}/generar-documento")
    @PreAuthorize("isAuthenticated()") // O el permiso que estés usando ('EDITAR_CONTRATO')
    public ResponseEntity<?> generarDocumento(@PathVariable Long id) {
        try {
            // Ya no recibimos el nuevoEstado. El backend lo leerá solo.
            String observacion = "Generación automática de documento oficial según el estado actual en BD.";
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
        // "inline" permite que se abra en el navegador
        headers.setContentDispositionFormData("inline", "Vista_Previa_Contrato_" + id + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}