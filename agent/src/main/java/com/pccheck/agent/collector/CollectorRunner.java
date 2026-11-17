package com.pccheck.agent.collector;

import com.pccheck.agent.model.CollectorResult;

/** Executa um coletor isolando qualquer falha em um CollectorResult. */
public final class CollectorRunner {

    private CollectorRunner() {
    }

    public static <T> CollectorResult<T> run(Collector<T> collector) {
        String name = collector.name();
        try {
            T data = collector.collect();
            if (data == null) {
                return CollectorResult.unavailable(name, "O sistema nao forneceu esta informacao.");
            }
            return CollectorResult.ok(name, data);
        } catch (CollectorUnavailableException e) {
            return CollectorResult.unavailable(name, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CollectorResult.error(name, "Coleta interrompida.");
        } catch (Exception | LinkageError e) {
            // LinkageError cobre falhas de bibliotecas nativas (ex.: JNA) sem derrubar os outros coletores.
            return CollectorResult.error(name, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
