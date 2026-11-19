package com.pccheck.diagnostic;

/**
 * Limites da saúde dos discos, configuráveis em application.properties ({@code nexcore.health.disk.*}).
 *
 * <p>Origem dos valores padrão (conservadores; consulte a ficha técnica do seu disco):
 * <ul>
 *   <li>Temperatura: discos rígidos comuns operam até cerca de 60 °C (ex.: Seagate ST1000DM003: 0 a 60 °C); o aviso
 *       vem antes, em 55 °C. SSDs costumam operar até 70 °C.</li>
 *   <li>Vida útil restante (SSD): heurística do projeto: Atenção com 20% ou menos, Crítico com 5% ou menos.</li>
 * </ul>
 *
 * @param lifeWarningPercent  vida útil restante (%) a partir da qual há Atenção
 * @param lifeCriticalPercent vida útil restante (%) a partir da qual é Crítico
 * @param hddTempWarningC     temperatura (°C) de Atenção para HDDs
 * @param ssdTempWarningC     temperatura (°C) de Atenção para SSDs e discos de tipo desconhecido
 */
public record DiskHealthThresholds(double lifeWarningPercent, double lifeCriticalPercent,
                                   double hddTempWarningC, double ssdTempWarningC) {

    public DiskHealthThresholds {
        if (!(lifeCriticalPercent >= 0 && lifeWarningPercent > lifeCriticalPercent && lifeWarningPercent <= 100)) {
            throw new IllegalArgumentException("Vida útil: é preciso 0 <= crítico < atenção <= 100.");
        }
        if (!(hddTempWarningC > 0 && ssdTempWarningC > 0)) {
            throw new IllegalArgumentException("Temperatura dos discos: os limites devem ser positivos.");
        }
    }

    public static DiskHealthThresholds defaults() {
        return new DiskHealthThresholds(20.0, 5.0, 55.0, 70.0);
    }
}
