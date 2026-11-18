package com.kafka.pago.DTO;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntentoPagoDTO {
    private String idCliente;
    private Long monto;
    private String idTransaccion;
    private LocalDateTime timestamp;
}

