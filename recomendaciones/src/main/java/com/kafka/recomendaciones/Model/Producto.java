package com.kafka.recomendaciones.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Producto {
    private String id;           // Coincide con el itemId de Interacciones
    private String nombre;
    private String categoria;
    private String productType;  // Coincide con productType de Interacciones
    private String marca;
    private double precio;
    private double calificacion; // Ej: 4.8
    private String imagenUrl;
    private int stock;
}