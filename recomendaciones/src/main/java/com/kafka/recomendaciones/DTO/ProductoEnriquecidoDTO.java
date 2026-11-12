package com.kafka.recomendaciones.DTO;
import lombok.Data;

@Data
public class ProductoEnriquecidoDTO {
    private int rankPosition;
    private double score;
    private String itemId;
    private String nombre;
    private String categoria;
    private String marca;
    private double precio;
    private double calificacion;
    private String imagenUrl;
}