package com.kafka.recomendaciones.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kafka.recomendaciones.Model.Recomendaciones;
import com.kafka.recomendaciones.Service.RecomendacionesService;

import java.util.List;

@RestController
@RequestMapping("/api/recomendaciones")
@CrossOrigin(origins = "*") // Permite llamadas desde tu frontend en React sin bloqueos CORS
public class RecomendacionesController {
    
    @Autowired 
    private  RecomendacionesService recomendacionesServices;

    /**
     * Endpoint GET para que React consulte el Top 5 de recomendaciones de un usuario.
     * Ejemplo de llamada: GET /api/recommendations?userId=123
     */
    @GetMapping
    public ResponseEntity<List<Recomendaciones>> getRecommendations(@RequestParam("userId") String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<Recomendaciones> top5 = recomendacionesServices.obtenerTop5(userId);
        return ResponseEntity.ok(top5);
    }
}