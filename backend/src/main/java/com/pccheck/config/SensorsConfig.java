package com.pccheck.config;

import com.pccheck.agent.sensors.SensorHostClient;
import com.pccheck.agent.sensors.SensorSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fonte de GPU e sensores: leitor de sensores do NEXCORE (sensor-host), iniciado aqui junto com o backend.
 * Com {@code nexcore.sensors.elevate=true}, o Windows pede permissão de administrador ao iniciar (necessária
 * para temperatura da CPU e ventoinhas). O leitor é encerrado quando o backend termina ({@code close}).
 */
@Configuration
public class SensorsConfig {

    @Bean
    public SensorSource sensorSource(@Value("${nexcore.sensors.enabled:true}") boolean enabled,
                                     @Value("${nexcore.sensors.elevate:true}") boolean elevate,
                                     @Value("${nexcore.sensors.host-path:}") String hostPath) {
        if (!enabled) {
            return SensorSource.unavailable("A leitura de sensores está desativada (nexcore.sensors.enabled=false).");
        }
        return SensorHostClient.start(hostPath, elevate);
    }
}
