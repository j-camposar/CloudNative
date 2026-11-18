package com.kafka.recomendaciones.Service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.kafka.recomendaciones.DTO.InteractionRequestsDTO;
import com.kafka.recomendaciones.Model.Interacciones;
import com.kafka.recomendaciones.Repository.InteraccionesRepository;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class InteraccionService {
    private final KafkaTemplate<String, InteractionRequestsDTO> kafkaTemplate;
    private final InteraccionesRepository interaccionesRepository;

    // Inyección por constructor limpia
    public InteraccionService(InteraccionesRepository interaccionesRepository,
                              KafkaTemplate<String, InteractionRequestsDTO> kafkaTemplate) {
        this.interaccionesRepository = interaccionesRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    private static final String TOPIC_RECOMENDACION = "interacciones-user";

    public void interaccionEvento(String idCliente,  InteractionRequestsDTO request) {
        int weight = switch (request.getAction().toLowerCase()) {
            case "purchase" -> 3;
            case "cart"     -> 2;
            case "view"     -> 1;
            default         -> 1;
        };
        // Construir el evento enriquecido
        Interacciones evento = new Interacciones(
                request.getClientId(),
                request.getItemId(),
                request.getAction(),
                weight,
                request.getCategory(),
                request.getProductType()
        );
        interaccionesRepository.save(evento);
        kafkaTemplate.send(TOPIC_RECOMENDACION, idCliente, request);
        log.info("Evento de intento enviado a Kafka para el cliente: {}", idCliente);
    }
}
