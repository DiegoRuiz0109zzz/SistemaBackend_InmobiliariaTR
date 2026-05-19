package com.sistema.base.api.core.Financiamiento.Comision.dtos;

import lombok.Data;

@Data
public class ActualizarPagoRequest {
    private String estadoPago; // PENDIENTE, PAGADO, ANULADO
    private String observacionPago;
}
