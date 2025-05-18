package com.rx_hw;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler для I/O задач: использует CachedThreadPool
 */
public class IOThreadScheduler implements Scheduler {
    private final ExecutorService exec = Executors.newCachedThreadPool();

    @Override
    public void execute(Runnable task) {
        exec.submit(task);
    }
}
