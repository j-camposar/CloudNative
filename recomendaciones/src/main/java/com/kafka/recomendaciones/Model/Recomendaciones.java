package com.kafka.recomendaciones.Model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recomendaciones", indexes = {
    @Index(name = "idx_user_rank", columnList = "user_id, rank_position")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Recomendaciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(name = "score", nullable = false)
    private double score; // Puntaje acumulado calculado (peso total)

    @Column(name = "rank_position", nullable = false)
    private int rankPosition; // Posición del top (1 al 5)

    @Column(name = "category")
    private String category;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    // Constructor útil para instanciar rápidamente al recalcular el top
    public Recomendaciones(String userId, String itemId, double score, int rankPosition, String category, String productType) {
        this.userId = userId;
        this.itemId = itemId;
        this.score = score;
        this.rankPosition = rankPosition;
        this.category = category;
        this.productType = productType;
        this.updatedAt = System.currentTimeMillis();
    }
}