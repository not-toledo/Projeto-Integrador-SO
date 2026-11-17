package com.pccheck.config;

import com.pccheck.diagnostic.HealthThresholds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Limites do Health Check, configuráveis em application.properties (pccheck.health.*). */
@Configuration
public class HealthConfig {

    @Bean
    public HealthThresholds healthThresholds(
            @Value("${pccheck.health.cpu-temp-warning-c:80}") double cpuTempWarningC,
            @Value("${pccheck.health.cpu-temp-critical-c:95}") double cpuTempCriticalC,
            @Value("${pccheck.health.disk-free-warning-percent:15}") double diskFreeWarningPercent,
            @Value("${pccheck.health.disk-free-critical-percent:5}") double diskFreeCriticalPercent,
            @Value("${pccheck.health.ram-used-warning-percent:85}") double ramUsedWarningPercent) {
        return new HealthThresholds(cpuTempWarningC, cpuTempCriticalC, diskFreeWarningPercent,
                diskFreeCriticalPercent, ramUsedWarningPercent);
    }
}
