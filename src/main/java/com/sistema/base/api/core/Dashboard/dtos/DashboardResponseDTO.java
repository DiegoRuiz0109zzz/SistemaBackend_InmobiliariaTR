package com.sistema.base.api.core.Dashboard.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDTO {
    private DashboardKpiDTO kpis;
    private List<MensualChartDTO> ventasPorMes;
    private List<MensualChartDTO> proyeccionCobrosPorMes;
}
