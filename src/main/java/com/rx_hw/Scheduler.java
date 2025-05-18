package com.rx_hw;

/**
 * Интерфейс планировщика задач для асинхронного выполнения
 */
public interface Scheduler {
    /** Выполнить задачу в соответствующем потоке/пуле */
    void execute(Runnable task);
}
