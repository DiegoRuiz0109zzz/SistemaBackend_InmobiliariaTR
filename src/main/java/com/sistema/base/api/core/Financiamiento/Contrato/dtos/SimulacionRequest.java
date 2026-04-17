package com.sistema.base.api.core.Financiamiento.Contrato.dtos;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SimulacionRequest {
    private Double precioTotal;
    private Double montoInicial;
    private Integer cantidadCuotas;
    private LocalDate fechaInicioPago; // Cuándo paga la primera cuota
}
