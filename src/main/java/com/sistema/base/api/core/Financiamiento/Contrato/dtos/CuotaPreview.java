package com.sistema.base.api.core.Financiamiento.Contrato.dtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class CuotaPreview {
    private Integer numeroCuota;
    private Double monto;
    private LocalDate fechaVencimiento;
}