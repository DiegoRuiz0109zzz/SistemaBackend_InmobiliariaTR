package com.sistema.base.api.core.Financiamiento.Contrato;

public enum EstadoContrato {
    SEPARADO,   // Abono parcial de inicial
    ACTIVO,     // Venta Final concretada
    DESESTIMIENTO,   // Contrato anulado por falta de pago
    LIBERADO,
    FINALIZADO  // Todo pagado al 100%
}
