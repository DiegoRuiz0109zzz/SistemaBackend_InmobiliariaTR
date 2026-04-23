package com.sistema.base.api.core.Financiamiento.Pago.dtos;

import lombok.Data;

@Data
public class ProcesarPagoRequest {
    private String metodoPago;
    private String numeroOperacion;
    private String fotoVoucherUrl;
}