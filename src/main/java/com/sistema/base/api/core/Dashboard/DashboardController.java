package com.sistema.base.api.core.Dashboard;

import com.sistema.base.api.core.Dashboard.dtos.DashboardResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardResponseDTO> getDashboard(
            @RequestParam(required = false) Long urbanizacionId,
            @RequestParam(required = false) Long etapaId,
            @RequestParam(required = false) Long manzanaId,
            @RequestParam(required = false) Integer anio
    ) {
        DashboardResponseDTO data = dashboardService.getDashboardData(urbanizacionId, etapaId, manzanaId, anio);
        return ResponseEntity.ok(data);
    }
}
