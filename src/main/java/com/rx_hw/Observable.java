package com.rx_hw;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

/**
 * Основной класс-источник реактивного потока
 */
public class Observable<T> {
    /**
     * Функциональный интерфейс для эмиттера данных
     */
    @FunctionalInterface
    public interface OnSubscribe<T> {
        void call(Observer<? super T> observer);
    }

    private final OnSubscribe<T> onSubscribe;

    private Observable(OnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    /**
     * Создаёт Observable из логики эмиссии
     */
    public static <T> Observable<T> create(OnSubscribe<T> source) {
        return new Observable<>(source);
    }

    /**
     * Простая подписка без возможности отмены
     */
    public void subscribe(Observer<? super T> observer) {
        try {
            onSubscribe.call(observer);
            observer.onComplete();
        } catch (Throwable t) {
            observer.onError(t);
        }
    }

    /**
     * Подписка с возможностью отмены (Disposable), эмиссия в отдельном потоке
     */
    public Disposable subscribeWithDisposable(Observer<? super T> observer) {
        AtomicBoolean disposed = new AtomicBoolean(false);

        Observer<T> safe = new Observer<T>() {
            @Override public void onNext(T item) {
                if (!disposed.get()) observer.onNext(item);
            }
            @Override public void onError(Throwable t) {
                if (!disposed.get()) observer.onError(t);
            }
            @Override public void onComplete() {
                if (!disposed.get()) observer.onComplete();
            }
        };

        Disposable disp = new Disposable() {
            @Override public void dispose() { disposed.set(true); }
            @Override public boolean isDisposed() { return disposed.get(); }
        };

        new Thread(() -> {
            try {
                onSubscribe.call(safe);
                safe.onComplete();
            } catch (Throwable t) {
                safe.onError(t);
            }
        }).start();

        return disp;
    }

    /**
     * Оператор преобразования элементов
     */
    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return create(observer ->
                this.subscribe(new Observer<T>() {
                    @Override public void onNext(T item) {
                        R mapped;
                        try { mapped = mapper.apply(item); }
                        catch (Throwable e) { observer.onError(e); return; }
                        observer.onNext(mapped);
                    }
                    @Override public void onError(Throwable t) { observer.onError(t); }
                    @Override public void onComplete() { observer.onComplete(); }
                })
        );
    }

    /**
     * Оператор фильтрации
     */
    public Observable<T> filter(Predicate<? super T> predicate) {
        return create(observer ->
                this.subscribe(new Observer<T>() {
                    @Override public void onNext(T item) {
                        boolean pass;
                        try { pass = predicate.test(item); }
                        catch (Throwable e) { observer.onError(e); return; }
                        if (pass) observer.onNext(item);
                    }
                    @Override public void onError(Throwable t) { observer.onError(t); }
                    @Override public void onComplete() { observer.onComplete(); }
                })
        );
    }

    /**
     * Оператор flatMap: развёртывает каждый элемент в новый Observable
     */
    public <R> Observable<R> flatMap(Function<? super T, ? extends Observable<? extends R>> mapper) {
        return create(emitter ->
                this.subscribe(new Observer<T>() {
                    AtomicInteger wip = new AtomicInteger(1);
                    AtomicBoolean error = new AtomicBoolean(false);

                    @Override public void onNext(T t) {
                        Observable<? extends R> o;
                        try { o = mapper.apply(t); }
                        catch (Throwable e) { emitter.onError(e); error.set(true); return; }
                        wip.incrementAndGet();
                        o.subscribe(new Observer<R>() {
                            @Override public void onNext(R r) { emitter.onNext(r); }
                            @Override public void onError(Throwable e) { emitter.onError(e); error.set(true); }
                            @Override public void onComplete() {
                                if (wip.decrementAndGet() == 0 && !error.get()) {
                                    emitter.onComplete();
                                }
                            }
                        });
                    }

                    @Override public void onError(Throwable e) {
                        emitter.onError(e);
                        error.set(true);
                    }

                    @Override public void onComplete() {
                        if (wip.decrementAndGet() == 0 && !error.get()) {
                            emitter.onComplete();
                        }
                    }
                })
        );
    }

    /**
     * Оператор subscribeOn: подписка и эмиссия в указанном Scheduler
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        return create(observer ->
                scheduler.execute(() -> {
                    try {
                        onSubscribe.call(observer);
                        observer.onComplete();
                    } catch (Throwable t) {
                        observer.onError(t);
                    }
                })
        );
    }

    /**
     * Оператор observeOn: переключает поток для onNext/onError/onComplete
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        return create(observer ->
                this.subscribe(new Observer<T>() {
                    @Override public void onNext(T item) { scheduler.execute(() -> observer.onNext(item)); }
                    @Override public void onError(Throwable t) { scheduler.execute(() -> observer.onError(t)); }
                    @Override public void onComplete() { scheduler.execute(observer::onComplete); }
                })
        );
    }
}
