package com.kafka.pago.Config;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class WebPayConfig {

    @PostConstruct
    public void initWebpay() {
      System.out.println("Configuración de Webpay lista para SDK 6.x");
    }
}
