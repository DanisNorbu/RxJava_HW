package com.rx_hw;


public class Main {
    public static void main(String[] args) {
        // Пример использования библиотеки
        Observable<Integer> source = Observable.create(observer -> {
            for (int i = 1; i <= 5; i++) {
                observer.onNext(i);
            }
        });

        // Подписка с цепочкой операторов и переключением потоков
        source
                .subscribeOn(new IOThreadScheduler())
                .map(i -> i * 10)
                .filter(i -> i % 20 == 0)
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(Integer item) {
                        System.out.println("Received: " + item + " on thread " + Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("Error: " + t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("Stream completed");
                    }
                });

        // Чтобы дать асинхронным задачам время выполниться
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}
    }
}
