package com.rx_hw;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Юнит-тесты для основных операторов и обработки ошибок
 */
class ObservableTest {

    @Test
    void testMapAndFilter() {
        List<Integer> result = new ArrayList<>();
        Observable<Integer> source = Observable.create(obs -> {
            for (int i = 1; i <= 5; i++) {
                obs.onNext(i);
            }
        });

        source
                .map(i -> i * 2)
                .filter(i -> i % 4 == 0)
                .subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) {
                        result.add(item);
                    }
                    @Override public void onError(Throwable t) {
                        fail("Unexpected error: " + t);
                    }
                    @Override public void onComplete() { }
                });

        assertEquals(List.of(4, 8), result);
    }

    @Test
    void testSubscribeOnObserveOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> threadNames = new ArrayList<>();
        Observable<String> source = Observable.create(obs -> obs.onNext(Thread.currentThread().getName()));

        source
                .subscribeOn(new ComputationScheduler())
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<String>() {
                    @Override public void onNext(String name) {
                        threadNames.add(name);
                        latch.countDown();
                    }
                    @Override public void onError(Throwable t) {
                        fail("Error during execution: " + t);
                    }
                    @Override public void onComplete() { }
                });

        boolean received = latch.await(1, TimeUnit.SECONDS);
        assertTrue(received, "Expected onNext but none received");
        assertFalse(threadNames.isEmpty(), "Thread name not recorded");
        assertTrue(threadNames.get(0).startsWith("pool-"),
                "subscribeOn should run in computation pool");
    }

    @Test
    void testMapError() {
        RuntimeException ex = new RuntimeException("map failed");
        Observable<Integer> source = Observable.create(obs -> obs.onNext(1));
        List<Throwable> errors = new ArrayList<>();

        source
                .<Integer>map(i -> { throw ex; })               // <-- добавили <Integer>
                .subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) { }
                    @Override public void onError(Throwable t) { errors.add(t); }
                    @Override public void onComplete() { }
                });

        assertEquals(1, errors.size(), "Expected one error from map");
        assertSame(ex, errors.get(0), "Error should be the thrown exception");
    }

    @Test
    void testFilterError() {
        RuntimeException ex = new RuntimeException("filter failed");
        Observable<Integer> source = Observable.create(obs -> obs.onNext(1));
        List<Throwable> errors = new ArrayList<>();

        source
                .filter(i -> { throw ex; })
                .subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) { }
                    @Override public void onError(Throwable t) { errors.add(t); }
                    @Override public void onComplete() { }
                });

        assertEquals(1, errors.size(), "Expected one error from filter");
        assertSame(ex, errors.get(0), "Error should be the thrown exception");
    }

    @Test
    void testFlatMapError() {
        RuntimeException ex = new RuntimeException("flatMap failed");
        Observable<Integer> source = Observable.create(obs -> obs.onNext(1));
        List<Throwable> errors = new ArrayList<>();

        source
                .<Integer>flatMap(i -> { throw ex; })
                .subscribe(new Observer<Integer>() {
                    @Override public void onNext(Integer item) { }
                    @Override public void onError(Throwable t) { errors.add(t); }
                    @Override public void onComplete() { }
                });

        assertEquals(1, errors.size(), "Expected one error from flatMap");
        assertSame(ex, errors.get(0), "Error should be the thrown exception");
    }
}
