package com.pccheck.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pccheck.os.CommandRunner.Result;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchedulerStatusTest {

    @Test
    void windowsComTarefaInstaladaMostraProximaExecucao() {
        List<List<String>> calls = new ArrayList<>();
        SchedulerStatus s = SchedulerStatus.query("Windows 10", cmd -> {
            calls.add(cmd);
            return new Result(0, "\"\\NEXCORE\\Coleta\",\"18/11/2026 10:15:00\",\"Pronto\"\r\n");
        });
        assertEquals("INSTALLED", s.state());
        assertEquals("Próxima execução: 18/11/2026 10:15:00 · Estado: Pronto", s.details());
        assertEquals(List.of("schtasks", "/Query", "/TN", "NEXCORE\\Coleta", "/FO", "CSV", "/NH"), calls.get(0));
    }

    @Test
    void windowsSemTarefa() {
        SchedulerStatus s = SchedulerStatus.query("Windows 11",
                cmd -> new Result(1, "ERRO: O sistema não pode encontrar o arquivo especificado."));
        assertEquals("NOT_INSTALLED", s.state());
        assertNull(s.details());
    }

    @Test
    void linuxComLinhaMarcadaNoCrontab() {
        String crontab = "0 3 * * * /usr/bin/backup\n*/15 * * * * java -jar /home/u/nexcore-agent.jar --registrar-coleta "
                + SchedulerStatus.CRON_MARKER + "\n";
        SchedulerStatus s = SchedulerStatus.query("Linux", cmd -> new Result(0, crontab));
        assertEquals("INSTALLED", s.state());
        assertTrue(s.details().startsWith("*/15 * * * *"));
    }

    @Test
    void linuxSemCrontabNaoEInstalado() {
        SchedulerStatus s = SchedulerStatus.query("Linux", cmd -> new Result(1, "no crontab for u"));
        assertEquals("NOT_INSTALLED", s.state());
    }

    @Test
    void agendadorIndisponivelOuLentoEDesconhecido() {
        assertEquals("UNKNOWN", SchedulerStatus.query("Linux", cmd -> {
            throw new IOException("crontab: comando não encontrado");
        }).state());
        assertEquals("UNKNOWN", SchedulerStatus.query("Windows 10", cmd -> null).state());
    }
}
