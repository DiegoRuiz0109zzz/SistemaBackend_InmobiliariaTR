package com.sistema.base.api.core.Reportes;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/maestro")
    @PreAuthorize("isAuthenticated()") // Puedes ajustar los roles (ej. hasAuthority('VER_REPORTES'))
    public ResponseEntity<List<ReporteMaestroDTO>> obtenerReporteMaestro() {
        return ResponseEntity.ok(reporteService.obtenerReporteMaestro());
    }
}
