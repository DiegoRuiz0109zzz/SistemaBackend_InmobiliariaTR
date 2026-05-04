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
}
