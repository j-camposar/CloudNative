package com.kafka.recomendaciones.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "interacciones")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Interacciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Identificador único autoincremental en la BD relacional

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "weight", nullable = false)
    private int weight; // 1: view, 2: cart, 3: purchase

    @Column(name = "category")
    private String category;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    public Interacciones(String userId, String itemId, String action, int weight, String category, String productType) {
        this.userId = userId;
        this.itemId = itemId;
        this.action = action;
        this.weight = weight;
        this.category = category;
        this.productType = productType;
        this.timestamp = System.currentTimeMillis();
    }
}
