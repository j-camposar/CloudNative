package com.kafka.pago.DTO;

import lombok.Data;

@Data
public class PeticionPagoDTO {
    private String clientId;
    private Long amount;
    private String transactionId;
}
