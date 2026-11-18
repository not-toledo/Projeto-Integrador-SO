package com.pccheck.agent.collector;

import com.pccheck.agent.model.DiskHealthInfo;
import com.pccheck.agent.sensors.SensorReading;
import com.pccheck.agent.sensors.SensorSource;
import com.pccheck.agent.sensors.SensorsUnavailableException;

/**
 * Saúde dos discos (SMART) lida pelo leitor de sensores do PCCheck. A leitura do SMART exige administrador; sem
 * ela, ou sem o leitor (Linux), o resultado é "não disponível" com o motivo, nunca um disco presumido saudável.
 */
public class DiskHealthCollector implements Collector<DiskHealthInfo> {

    static final String NO_DISKS =
            "O leitor de sensores não encontrou discos com dados SMART (o disco ou o controlador podem não informá-los).";

    private final SensorSource sensors;

    public DiskHealthCollector(SensorSource sensors) {
        this.sensors = sensors;
    }

    @Override
    public String name() {
        return "DiskHealthCollector";
    }

    @Override
    public DiskHealthInfo collect() throws CollectorUnavailableException {
        SensorReading reading;
        try {
            reading = sensors.read();
        } catch (SensorsUnavailableException e) {
            throw new CollectorUnavailableException(e.getMessage());
        }
        if (reading.disks().isEmpty()) {
            throw new CollectorUnavailableException(reading.diskLimitation() != null ? reading.diskLimitation() : NO_DISKS);
        }
        return new DiskHealthInfo(reading.disks(), reading.diskLimitation());
    }
}
