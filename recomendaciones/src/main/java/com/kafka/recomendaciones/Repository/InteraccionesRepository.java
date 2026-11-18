package com.kafka.recomendaciones.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kafka.recomendaciones.Model.Interacciones;

import java.util.List;

@Repository
public interface InteraccionesRepository extends JpaRepository<Interacciones, Long> {
    
    // Permite buscar todas las interacciones de un usuario para calcular su ranking
    List<Interacciones> findByUserId(String userId);
}