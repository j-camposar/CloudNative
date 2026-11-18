package com.kafka.recomendaciones.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kafka.recomendaciones.Config.ProductoConfig;
import com.kafka.recomendaciones.DTO.ProductoEnriquecidoDTO;
import com.kafka.recomendaciones.Model.Interacciones;
import com.kafka.recomendaciones.Model.Producto;
import com.kafka.recomendaciones.Model.Recomendaciones;
import com.kafka.recomendaciones.Repository.InteraccionesRepository;
import com.kafka.recomendaciones.Repository.RecomendacionesRepository;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class RecomendacionesService {

    @Autowired 
    private  InteraccionesRepository interaccionesRepository;
    @Autowired 
    private  RecomendacionesRepository recomendacionesRepository;
    @Autowired 
    private  ProductoConfig productCatalog; // Catálogo en memoria

    @Transactional
    public void calcularYGuardarTop5(String userId) {
        List<Interacciones> interacciones = interaccionesRepository.findByUserId(userId);
        if (interacciones.isEmpty()) return;

        Map<String, Double> itemScores = new HashMap<>();
        for (Interacciones inter : interacciones) {
            itemScores.put(inter.getItemId(), itemScores.getOrDefault(inter.getItemId(), 0.0) + inter.getWeight());
        }

        // Ordenar y recortar al Top 5
        var top5Entries = itemScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
            .limit(5)
            .collect(Collectors.toList());

        recomendacionesRepository.deleteByUserId(userId);

        List<Recomendaciones> nuevasRecomendaciones = new ArrayList<>();
        int rank = 1;

        for (Map.Entry<String, Double> entry : top5Entries) {
            String itemId = entry.getKey();
            double score = entry.getValue();

            // Buscar la categoría y tipo desde el catálogo en memoria (o valores por defecto)
            Producto producto = productCatalog.buscarPorId(itemId).orElse(null);
            String categoria = producto != null ? producto.getCategoria() : "general";
            String productType = producto != null ? producto.getProductType() : "standard";

            Recomendaciones rec = new Recomendaciones(
                userId, itemId, score, rank++, categoria, productType
            );
            nuevasRecomendaciones.add(rec);
        }

        recomendacionesRepository.saveAll(nuevasRecomendaciones);
    }

    /**
     * Devuelve las recomendaciones enriquecidas con la información del catálogo en memoria
     */
    public List<Recomendaciones> obtenerTop5(String userId) {
        return recomendacionesRepository.findByUserIdOrderByRankPositionAsc(userId);
    }
}