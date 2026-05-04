package com.sistema.base.api.core.Dashboard.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiDTO {
    // Lotes Globales
    private Long totalLotes;
    private Long lotesVendidos;
    private Long lotesDisponibles;
    
    // Valor Lotes Globales
    private Double valorTotal;
    private Double valorVendido;
    private Double valorDisponible;
    private Double valorPotencial;
    
    // Porcentajes Lotes
    private Double porcentajeVentasCantidad;
    private Double porcentajeVentasMonto;
    
    // Financiero
    private Double totalCobrado;
    private Double totalPorCobrar;
    private Double porcentajeRecaudacion;
}
