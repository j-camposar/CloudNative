package com.kafka.pago.Component;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import com.kafka.pago.DTO.IntentoPagoDTO;
import com.kafka.pago.Model.EstadoPagoCliente;
import com.kafka.pago.Repository.EstadoPagoClienteRepositorio;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component 
public class ConsumidorComponent {
    private static final String TOPIC_NOMBRE = "payment-attempts";
 
   
    @Autowired 
    private EstadoPagoClienteRepositorio estadoRepositorio;

    @KafkaListener(topics = TOPIC_NOMBRE, groupId = "payment-velocity-group")
    @Transactional
    public void consumirIntentoPago(IntentoPagoDTO evento) {
        log.info("Consumidor Kafka: Procesando intento para el cliente: {}", evento.getIdCliente());
        
        LocalDateTime ahora = LocalDateTime.now();
        
        // Busca el registro en la base de datos o crea uno nuevo si es su primer intento
        EstadoPagoCliente estado = estadoRepositorio.findById(evento.getIdCliente())
                .orElseGet(() -> {
                    EstadoPagoCliente nuevoEstado = new EstadoPagoCliente();
                    nuevoEstado.setIdCliente(evento.getIdCliente());
                    nuevoEstado.setCantidadIntentos(0);
                    nuevoEstado.setBloqueado(false);
                    return nuevoEstado;
                });

        // 1. Verificar si el cliente se encuentra actualmente bloqueado por el castigo de los 30 minutos
        if (estado.isBloqueado() && estado.getBloqueadoHasta() != null) {
            if (ahora.isBefore(estado.getBloqueadoHasta())) {
                log.warn("Consumidor: El cliente {} intentó procesar pago pero sigue bloqueado hasta {}", evento.getIdCliente(), estado.getBloqueadoHasta());
                return; 
            } else {
                // Si ya expiraron los 30 minutos de castigo, se limpia el bloqueo y se reinicia
                estado.setBloqueado(false);
                estado.setCantidadIntentos(0);
                estado.setBloqueadoHasta(null);
            }
        }

        // 2. Lógica de Ventana de Tiempo para el Velocity Check (Ej: 10 segundos)
        if (estado.getActualizadoEn() != null) {
            long segundosDesdeUltimoIntento = java.time.Duration.between(estado.getActualizadoEn(), ahora).getSeconds();
            
            if (segundosDesdeUltimoIntento > 10) {
                // Pasaron más de 10 segundos: se considera una nueva sesión normal, reiniciamos el contador a 0
                log.info("Cliente {}: Pasaron {} segundos desde el último intento. Ventana de 10s reseteada.", evento.getIdCliente(), segundosDesdeUltimoIntento);
                estado.setCantidadIntentos(0);
            }
        }

        // 3. Incrementar el contador dentro de la ventana de alta frecuencia
        estado.setCantidadIntentos(estado.getCantidadIntentos() + 1);
        estado.setActualizadoEn(ahora);

        // 4. Regla de Velocity Check: Si acumula 3 o más intentos en menos de 10 segundos -> Bloqueo de 30 min
        if (estado.getCantidadIntentos() >= 3) {
            estado.setBloqueado(true);
            estado.setBloqueadoHasta(ahora.plusMinutes(30));
            log.error("¡ALERTA FRAUDE DESDE KAFKA! Cliente {} realizó {} intentos en menos de 10 segundos. Bloqueado por 30 minutos.", 
                    evento.getIdCliente(), estado.getCantidadIntentos());
        }

        // Guardar el estado actualizado en MySQL
        estadoRepositorio.save(estado);
        log.info("Consumidor Kafka: Cliente {} actualizado. Intentos en la ráfaga actual: {}", evento.getIdCliente(), estado.getCantidadIntentos());
    }
   
}
