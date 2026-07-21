package com.sistema.base.api.core.Dashboard.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensualChartDTO {
    private String mes;
    private Long cantidadContratos;
    private Double montoVendido;

    private Double montoRecaudado; // ✅ NUEVO CAMPO

    // Constructor compatible con la consulta existente en ContratoRepository
    public MensualChartDTO(String mes, Long cantidadContratos, Double montoVendido) {
        this.mes = mes;
        this.cantidadContratos = cantidadContratos;
        this.montoVendido = montoVendido;
        this.montoRecaudado = 0.0;
    }
}
