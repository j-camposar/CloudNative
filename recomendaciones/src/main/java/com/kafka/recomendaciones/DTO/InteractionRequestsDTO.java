package com.kafka.recomendaciones.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class InteractionRequestsDTO {
    private String clientId;
    private String itemId;
    private String action; // 'ver' (1), 'agregar a carrito' (2), 'comprar' (3)
    private String category;
    private String productType;
}
