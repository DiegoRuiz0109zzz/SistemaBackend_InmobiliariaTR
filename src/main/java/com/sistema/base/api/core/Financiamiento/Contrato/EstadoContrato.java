package com.sistema.base.api.core.Financiamiento.Contrato;

public enum EstadoContrato {
    SEPARADO,   // Abono parcial de inicial
    ACTIVO,     // Venta Final concretada
    RESUELTO,   // Contrato anulado por falta de pago
    FINALIZADO  // Todo pagado al 100%
}
