package com.kafka.recomendaciones.Component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.kafka.recomendaciones.DTO.InteractionRequestsDTO;
import com.kafka.recomendaciones.Model.Interacciones;
import com.kafka.recomendaciones.Repository.InteraccionesRepository;
import com.kafka.recomendaciones.Service.RecomendacionesService;

import java.util.HashMap;
import java.util.Map;

@Component
public class InteraccionComponent {
    private final InteraccionesRepository interaccionesRepository;
    private final RecomendacionesService recomendacionesService;
    private final RestTemplate restTemplate;

    @Value("${recommendation.python.url}")
    private String pythonFastApiUrl;

    public InteraccionComponent(InteraccionesRepository interaccionesRepository, 
                                RecomendacionesService recomendacionesService,
                            RestTemplate restTemplate) {
        this.interaccionesRepository = interaccionesRepository;
        this.recomendacionesService = recomendacionesService;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "interacciones-user", groupId = "recommendation-consumer-group")
    public void consumirInteraccion(InteractionRequestsDTO dto) {
        try {
            if (dto == null) {
                System.err.println("El DTO recibido es nulo.");
                return;
            }

            System.out.println("Procesando evento de Kafka para usuario: " + dto.getClientId() + " | Item: " + dto.getItemId());

            int weight = switch (dto.getAction().toLowerCase()) {
                case "purchase" -> 3;
                case "cart"     -> 2;
                default         -> 1;
            };

            // Interacciones eventoSql = new Interacciones(
            //         dto.getClientId(),
            //         dto.getItemId(),
            //         dto.getAction(),
            //         weight,
            //         dto.getCategory(),
            //         dto.getProductType()
            // );

            // interaccionesRepository.save(eventoSql);
            // recomendacionesService.calcularYGuardarTop5(dto.getClientId());

            // Enviar a Python de forma limpia y directa
            enviarEventoAPython(dto, weight);

            System.out.println("Interacción procesada y sincronizada para: " + dto.getClientId());

        } catch (Exception e) {
            System.err.println("Error al procesar el mensaje del consumidor de Kafka: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void enviarEventoAPython(InteractionRequestsDTO dto, int weight) {
        System.out.println(">>> INICIANDO ENVÍO A PYTHON. URL CONFIGURADA: " + pythonFastApiUrl);
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", dto.getClientId());
            body.put("itemId", dto.getItemId());
            body.put("action", dto.getAction());
            body.put("weight", weight);
            body.put("category", dto.getCategory() != null ? dto.getCategory() : "general");
            body.put("productType", dto.getProductType() != null ? dto.getProductType() : "standard");

            System.out.println(">>> Payload preparado para FastAPI: " + body);

            restTemplate.postForObject(pythonFastApiUrl, body, String.class);
            
            System.out.println(">>> Evento enviado exitosamente a Python.");

        } catch (Exception e) {
            System.err.println(">>> ERROR CRÍTICO AL CONECTAR CON PYTHON: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}