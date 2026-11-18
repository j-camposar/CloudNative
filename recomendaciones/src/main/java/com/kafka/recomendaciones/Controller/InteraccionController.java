package com.kafka.recomendaciones.Controller;

import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kafka.recomendaciones.DTO.InteractionRequestsDTO;
import com.kafka.recomendaciones.Service.InteraccionService;

@RestController 
@RequestMapping ("/api/interacciones")
public class InteraccionController {
    private final InteraccionService interaccionService;

    // Inyección por constructor limpia y segura (sin @Autowired repetidos)
    public InteraccionController(InteraccionService interaccionService) {
        this.interaccionService = interaccionService;
    }
    @PostMapping
    public ResponseEntity<String> trackInteraction(@RequestBody InteractionRequestsDTO request) {

       interaccionService.interaccionEvento(request.getClientId(), request );
        return ResponseEntity.ok("Evento de interacción enviado a Kafka exitosamente (Peso: " + request.getAction().toLowerCase() + ")");
    }
    @PostMapping("/generar-lote")
    public ResponseEntity<String> generarInteraccionesAlAzar(@RequestParam(defaultValue = "50") int cantidad) {
        // Catálogo base para simular variedad basada en tus productos reales
        List<String> usuarios = List.of("user_123");
        List<String> items = List.of("item_101", "item_102", "item_103", "item_105", "item_107", "item_109", "item_112", "item_116");
        List<String> acciones = List.of("view", "cart", "purchase");
        List<String> categorias = List.of("electronica", "ropa", "hogar", "deportes");
        List<String> productTypes = List.of("standard", "premium");

        Random random = new Random();
        int exitosos = 0;

        try {
            for (int i = 0; i < cantidad; i++) {
                // Seleccionar datos al azar de las listas
                String clientId = usuarios.get(random.nextInt(usuarios.size()));
                String itemId = items.get(random.nextInt(items.size()));
                String action = acciones.get(random.nextInt(acciones.size()));
                String category = categorias.get(random.nextInt(categorias.size()));
                String productType = productTypes.get(random.nextInt(productTypes.size()));

                // Construir el DTO de la petición
                InteractionRequestsDTO dto = new InteractionRequestsDTO();
                dto.setClientId(clientId);
                dto.setItemId(itemId);
                dto.setAction(action);
                dto.setCategory(category);
                dto.setProductType(productType);

                // Enviar al servicio que dispara el evento a Kafka
                interaccionService.interaccionEvento(clientId, dto);
                exitosos++;
            }

            return ResponseEntity.ok("¡Lote generado con éxito! Se enviaron " + exitosos + " eventos de interacción al azar a Kafka.");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al generar el lote masivo: " + e.getMessage());
        }
    }
}
