package com.rx_hw;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class ObservableFlatMapDisposableTest {

    @Test
    void testDisposableStopsEmission() throws InterruptedException {
        Observable<Integer> source = Observable.create(obs -> {
            for (int i = 1; i <= 5; i++) {
                obs.onNext(i);
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }
        });

        List<Integer> received = new ArrayList<>();
        AtomicReference<Disposable> dispRef = new AtomicReference<>();

        // Сразу сохраняем Disposable в AtomicReference
        Disposable disp = source.subscribeWithDisposable(new Observer<>() {
            @Override
            public void onNext(Integer item) {
                received.add(item);
                if (item >= 3) {
                    // Теперь dispRef.get() уже не null
                    dispRef.get().dispose();
                }
            }
            @Override public void onError(Throwable t) { fail(t); }
            @Override public void onComplete() { fail("Should not complete"); }
        });
        dispRef.set(disp);

        // Ждём, чтобы фоновый поток успел эмитить
        TimeUnit.MILLISECONDS.sleep(100);

        // Ожидаем ровно [1,2,3]
        assertEquals(List.of(1, 2, 3), received);
        assertTrue(disp.isDisposed());
    }
}