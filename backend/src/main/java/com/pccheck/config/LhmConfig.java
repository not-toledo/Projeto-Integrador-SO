package com.pccheck.config;

import com.pccheck.agent.lhm.LhmClient;
import com.pccheck.agent.lhm.LhmSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fonte de GPU e sensores: servidor web local do LibreHardwareMonitor (nexcore.lhm.url).
 * Só endereços locais são aceitos; URL em branco desativa a integração.
 */
@Configuration
public class LhmConfig {

    @Bean
    public LhmSource lhmSource(@Value("${nexcore.lhm.url:" + LhmClient.DEFAULT_URL + "}") String url) {
        return LhmClient.create(url);
    }
}
