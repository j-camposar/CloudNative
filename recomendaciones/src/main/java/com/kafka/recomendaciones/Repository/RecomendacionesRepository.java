package com.kafka.recomendaciones.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kafka.recomendaciones.Model.Recomendaciones;

import java.util.List;

public interface RecomendacionesRepository extends JpaRepository<Recomendaciones, Long> {
    
    // Obtiene las recomendaciones ordenadas del 1 al 5 para un usuario específico
    List<Recomendaciones> findByUserIdOrderByRankPositionAsc(String userId);
    
    // Opcional: limpiar las recomendaciones anteriores del usuario antes de guardar el nuevo Top 5
    void deleteByUserId(String userId);
}