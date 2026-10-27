package com.helpflow.support.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling      // habilita la tarea de escalamiento
public class AppConfig {

    // Reloj inyectable: en pruebas se reemplaza por uno fijo
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
