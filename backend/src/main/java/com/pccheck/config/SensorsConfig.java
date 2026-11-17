package com.pccheck.config;

import com.pccheck.agent.sensors.SensorHostClient;
import com.pccheck.agent.sensors.SensorSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fonte de GPU e sensores: leitor de sensores do PCCheck (sensor-host), iniciado aqui junto com o backend.
 * Com {@code pccheck.sensors.elevate=true}, o Windows pede permissão de administrador ao iniciar (necessária
 * para temperatura da CPU e ventoinhas). O leitor é encerrado quando o backend termina ({@code close}).
 */
@Configuration
public class SensorsConfig {

    @Bean
    public SensorSource sensorSource(@Value("${pccheck.sensors.enabled:true}") boolean enabled,
                                     @Value("${pccheck.sensors.elevate:true}") boolean elevate,
                                     @Value("${pccheck.sensors.host-path:}") String hostPath) {
        if (!enabled) {
            return SensorSource.unavailable("A leitura de sensores está desativada (pccheck.sensors.enabled=false).");
        }
        return SensorHostClient.start(hostPath, elevate);
    }
}
