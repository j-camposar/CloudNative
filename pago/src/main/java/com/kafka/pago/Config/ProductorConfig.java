package com.kafka.pago.Config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.kafka.pago.DTO.IntentoPagoDTO;

import tools.jackson.databind.ser.jdk.StringSerializer;

@Configuration
public class ProductorConfig {

    @Bean
    public ProducerFactory<String, IntentoPagoDTO> producerFactory(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, IntentoPagoDTO> kafkaTemplate(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        return new KafkaTemplate<>(producerFactory(bootstrapServers));
    }
}
