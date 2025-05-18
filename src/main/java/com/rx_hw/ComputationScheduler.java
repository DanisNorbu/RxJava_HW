package com.rx_hw;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler для вычислений: фиксированный пул по количеству ядер
 */
public class ComputationScheduler implements Scheduler {
    private final ExecutorService exec = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    @Override
    public void execute(Runnable task) {
        exec.submit(task);
    }
}
