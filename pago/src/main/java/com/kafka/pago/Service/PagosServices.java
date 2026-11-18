package com.kafka.pago.Service;

import java.time.LocalDateTime;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.kafka.pago.DTO.IntentoPagoDTO;
import com.kafka.pago.DTO.TransaccionRespuestaDTO;
import com.kafka.pago.Model.EstadoPagoCliente;
import com.kafka.pago.Repository.EstadoPagoClienteRepositorio;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagosServices {

    // private final KafkaTemplate<String, IntentoPagoDTO> kafkaTemplate;
    private final EstadoPagoClienteRepositorio estadoRepositorio;

    private static final String TOPIC_NOMBRE = "payment-attempts";

    public boolean verificarSiUsuarioEstaBloqueado(String idCliente) {
        EstadoPagoCliente estado = estadoRepositorio.findById(idCliente).orElse(null);
        
        if (estado == null || !estado.isBloqueado()) {
            return false; // No existe o no está bloqueado
        }

        // Si está bloqueado, verificamos si ya pasaron los 30 minutos
        if (estado.getBloqueadoHasta() != null) {
            LocalDateTime ahora = LocalDateTime.now();
            
            if (ahora.isAfter(estado.getBloqueadoHasta())) {
                // ¡Ya pasaron los 30 minutos! Desbloqueamos automáticamente
                estado.setBloqueado(false);
                estado.setCantidadIntentos(0);
                estado.setBloqueadoHasta(null);
                estado.setActualizadoEn(ahora);
                estadoRepositorio.save(estado);
                
                log.info("El bloqueo por 30 minutos expiró para el cliente {}. Usuario desbloqueado automáticamente.", idCliente);
                return false; // Permitir el pago
            }
        }

        return true; // Sigue bloqueado (aún no transcurren los 30 minutos)
    }

   public TransaccionRespuestaDTO iniciarTransaccionWebpay(String buyOrder, String sessionId, double amount, String returnUrl) {
        try {
            // Usamos directamente el código de comercio y la API key oficial de prueba de Transbank para la versión 6.x
            WebpayPlus.Transaction tx = WebpayPlus.Transaction.buildForIntegration(
                "597055555532", 
                IntegrationApiKeys.WEBPAY
            );
            
            var response = tx.create(
                buyOrder, 
                sessionId, 
                amount, 
                returnUrl
            );
            
            String token = response.getToken();
            String urlWebpay = response.getUrl();
            
            log.info("Token Webpay generado con éxito: {}", token);
            return new TransaccionRespuestaDTO(token, urlWebpay);
            
        } catch (Exception e) {
            log.error("Error al conectar con Transbank: {}", e.getMessage());
            throw new RuntimeException("Error al iniciar pago en Webpay");
        }
    }
    // Método único para producir el intento hacia Kafka (sin duplicados)
    public void producirIntentoPago(String idCliente, Long monto, String idTransaccion) {
        IntentoPagoDTO evento = new IntentoPagoDTO(idCliente, monto, idTransaccion, java.time.LocalDateTime.now());
        // kafkaTemplate.send(TOPIC_NOMBRE, idCliente, evento);
        log.info("Evento de intento enviado a Kafka para el cliente: {}", idCliente);
    }

    // --- Gestión del Callback (Éxito / Fallo o rechazo) ---
    @Transactional
    public void gestionarResultadoCallbackPago(String idCliente, boolean esExitoso) {
        EstadoPagoCliente estado = estadoRepositorio.findById(idCliente).orElse(null);
        if (estado == null) return;

        if (esExitoso) {
            estado.setCantidadIntentos(0);
            estado.setBloqueado(false);
            estado.setBloqueadoHasta(null);
            log.info("Transacción exitosa para {}. Contador de intentos reiniciado a 0.", idCliente);
        } else {
            estado.setCantidadIntentos(0);
            estado.setBloqueado(false);
            estado.setBloqueadoHasta(null);
            log.info("Transacción rechazada/fallida para {}. Intentos reiniciados.", idCliente);
        }
        estadoRepositorio.save(estado);
    }
    // Método único para producir el intento hacia Kafka (sin duplicados)
    

}