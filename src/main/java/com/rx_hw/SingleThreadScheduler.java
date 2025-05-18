package com.rx_hw;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler для последовательного выполнения: один поток
 */
public class SingleThreadScheduler implements Scheduler {
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    @Override
    public void execute(Runnable task) {
        exec.submit(task);
    }
}
