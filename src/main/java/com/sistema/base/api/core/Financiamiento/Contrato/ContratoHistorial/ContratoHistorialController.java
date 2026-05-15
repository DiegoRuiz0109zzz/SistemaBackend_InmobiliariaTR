package com.sistema.base.api.core.Financiamiento.Contrato.ContratoHistorial;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/contrato-historial")
@RequiredArgsConstructor
public class ContratoHistorialController {

    private final ContratoHistorialService contratoHistorialService;

    @GetMapping("/contrato/{contratoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ContratoHistorial>> listarPorContrato(@PathVariable Long contratoId) {
        return ResponseEntity.ok(contratoHistorialService.listarPorContrato(contratoId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContratoHistorial> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(contratoHistorialService.obtenerPorId(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELIMINAR_HISTORIAL')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        contratoHistorialService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ El visor histórico se mantiene perfecto para auditoría
    @GetMapping("/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> verPdfHistorico(@PathVariable Long id) {
        try {
            ContratoHistorial historial = contratoHistorialService.obtenerPorId(id);

            if (historial.getRutaDocumentoPdf() == null) {
                throw new RuntimeException("Este registro no tiene un archivo físico asociado.");
            }

            Path rutaArchivo = Paths.get(historial.getRutaDocumentoPdf());
            Resource recurso = new UrlResource(rutaArchivo.toUri());

            if (recurso.exists() || recurso.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recurso.getFilename() + "\"")
                        .body(recurso);
            } else {
                throw new RuntimeException("No se pudo leer el archivo PDF");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}