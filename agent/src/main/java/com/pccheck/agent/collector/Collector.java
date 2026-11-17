package com.pccheck.agent.collector;

/** Coletor de um grupo de informações. Retornar null significa "dado não disponível". */
public interface Collector<T> {

    String name();

    T collect() throws Exception;
}
