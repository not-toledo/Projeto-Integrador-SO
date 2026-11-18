package com.pccheck.config;

import com.pccheck.diagnostic.DiskHealthThresholds;
import com.pccheck.diagnostic.HealthThresholds;
import com.pccheck.diagnostic.MemoryPressureThresholds;
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

    @Bean
    public MemoryPressureThresholds memoryPressureThresholds(
            @Value("${pccheck.health.memory-pressure.commit-warning-percent:90}") double commitWarningPercent,
            @Value("${pccheck.health.memory-pressure.page-out-warning-bytes-per-sec:1048576}") double pageOutWarning,
            @Value("${pccheck.health.memory-pressure.window-samples:30}") int windowSamples,
            @Value("${pccheck.health.memory-pressure.min-samples:10}") int minSamples) {
        return new MemoryPressureThresholds(commitWarningPercent, pageOutWarning, windowSamples, minSamples);
    }

    @Bean
    public DiskHealthThresholds diskHealthThresholds(
            @Value("${pccheck.health.disk.life-warning-percent:20}") double lifeWarning,
            @Value("${pccheck.health.disk.life-critical-percent:5}") double lifeCritical,
            @Value("${pccheck.health.disk.hdd-temp-warning-c:55}") double hddTemp,
            @Value("${pccheck.health.disk.ssd-temp-warning-c:70}") double ssdTemp) {
        return new DiskHealthThresholds(lifeWarning, lifeCritical, hddTemp, ssdTemp);
    }
}
