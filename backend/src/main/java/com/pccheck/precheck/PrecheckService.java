package com.pccheck.precheck;

import com.pccheck.diagnostic.HealthThresholds;
import com.pccheck.dto.GameRequirementsDto;
import com.pccheck.dto.InstalledGamesDto;
import com.pccheck.steam.SteamRequirementsService;
import com.pccheck.steam.SteamUnavailableException;
import com.pccheck.steam.local.InstalledGamesService;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Pré-checagem antes de jogar. Cada parte é independente: sem os requisitos (Steam fora do ar), sem a Steam local
 * ou sem algum dado do sistema, as demais verificações continuam.
 */
@Service
public class PrecheckService {

    private final SteamRequirementsService requirements;
    private final InstalledGamesService installed;
    private final HealthThresholds thresholds;
    private final SystemProbe probe = new SystemProbe();

    public PrecheckService(SteamRequirementsService requirements, InstalledGamesService installed,
                           HealthThresholds thresholds) {
        this.requirements = requirements;
        this.installed = installed;
        this.thresholds = thresholds;
    }

    public PrecheckDto check(long appId) {
        GameRequirementsDto game = null;
        String error = null;
        try {
            game = requirements.get(appId, false);
        } catch (SteamUnavailableException | IllegalArgumentException e) {
            error = e.getMessage();
        }
        InstalledGamesDto local;
        try {
            local = installed.installed();
        } catch (RuntimeException e) {
            local = null;
        }
        return PrecheckEvaluator.evaluate(appId, game, error, local, probe.sample(), thresholds, Instant.now());
    }
}
