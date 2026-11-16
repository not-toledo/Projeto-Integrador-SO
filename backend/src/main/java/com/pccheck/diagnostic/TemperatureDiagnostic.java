package com.pccheck.diagnostic;

import com.pccheck.agent.model.CollectorResult;
import com.pccheck.agent.model.SensorInfo;
import java.time.Instant;
import java.util.List;

/** Compara a temperatura real da CPU (quando o sistema a informa) com limites configuráveis. */
public class TemperatureDiagnostic implements Diagnostic {

    static final String COMPONENT = "Temperatura";
    static final String ID = "temperature:cpu";

    private final HealthThresholds thresholds;

    public TemperatureDiagnostic(HealthThresholds thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public String name() {
        return "TemperatureDiagnostic";
    }

    @Override
    public List<Alert> evaluate(HealthInput input, Instant now) {
        CollectorResult<SensorInfo> sensors = input.sensors();
        Double temp = (sensors != null && sensors.isOk() && sensors.data() != null)
                ? sensors.data().cpuTemperatureCelsius() : null;

        if (temp == null) {
            String reason = (sensors != null && sensors.message() != null) ? " Detalhe: " + sensors.message() : "";
            return List.of(new Alert(ID, COMPONENT, Severity.INFO, AlertKind.UNAVAILABLE,
                    "Temperatura da CPU não disponível",
                    "O sistema não informou uma leitura de temperatura. Isso é comum (depende do hardware, do driver "
                            + "e de permissões) e não indica defeito; a temperatura não foi estimada. Para obter a leitura, "
                            + "abra o LibreHardwareMonitor como administrador e ative Options > Remote Web Server > Run." + reason,
                    null, null, now));
        }

        String observed = Formatting.celsius(temp);
        if (temp >= thresholds.cpuTempCriticalC()) {
            return List.of(new Alert(ID, COMPONENT, Severity.CRITICAL, AlertKind.PREVENTIVE,
                    "Temperatura da CPU muito alta",
                    "A leitura atual está no nível crítico configurado. É um alerta preventivo (verifique ventilação, "
                            + "poeira e carga de trabalho); uma leitura isolada não comprova defeito.",
                    observed, "Crítico a partir de " + Formatting.celsius(thresholds.cpuTempCriticalC()), now));
        }
        if (temp >= thresholds.cpuTempWarningC()) {
            return List.of(new Alert(ID, COMPONENT, Severity.WARNING, AlertKind.PREVENTIVE,
                    "Temperatura da CPU elevada",
                    "A leitura atual passou do limite de atenção configurado. Pode ocorrer sob carga alta; "
                            + "acompanhe se persiste em repouso.",
                    observed, "Atenção a partir de " + Formatting.celsius(thresholds.cpuTempWarningC()), now));
        }
        return List.of(new Alert(ID, COMPONENT, Severity.INFO, AlertKind.NORMAL,
                "Temperatura da CPU normal",
                "A leitura atual está abaixo do limite de atenção configurado.",
                observed, "Atenção a partir de " + Formatting.celsius(thresholds.cpuTempWarningC()), now));
    }
}
