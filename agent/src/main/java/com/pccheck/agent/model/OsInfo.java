package com.pccheck.agent.model;

/** Sistema operacional. Campo nulo = não disponível. */
public record OsInfo(String name, String version, String buildNumber, String architecture, Long uptimeSeconds) {
}
