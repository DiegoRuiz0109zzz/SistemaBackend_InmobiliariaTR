package com.sistema.base.api.core.Financiamiento.Contrato.dtos;

import lombok.Data;
import java.util.List;

@Data
public class SimulacionResponseDTO {
    private List<CuotaPreview> cronograma;
    private String mensajeSugerencia;
}