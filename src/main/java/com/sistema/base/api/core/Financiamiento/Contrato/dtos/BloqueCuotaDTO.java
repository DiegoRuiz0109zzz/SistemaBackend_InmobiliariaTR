package com.sistema.base.api.core.Financiamiento.Contrato.dtos;

import lombok.Data;

@Data
public class BloqueCuotaDTO {
    private Integer cantidad;
    private Double monto;
    private String tipo; // Para que el frontend decida: "MENSUAL" o "ESPECIAL"
}
