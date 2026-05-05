package com.sistema.base.api.core.Financiamiento.Contrato.dtos;

import com.sistema.base.api.core.Financiamiento.Cuota.TipoCuota;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class CuotaPreview {
    private Integer numeroCuota;
    private Double monto;
    private LocalDate fechaVencimiento;
    private TipoCuota tipoCuota;
}