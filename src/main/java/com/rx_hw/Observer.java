package com.rx_hw;

/**
 * Интерфейс наблюдателя, получает события от Observable
 */
public interface Observer<T> {
    void onNext(T item);
    void onError(Throwable t);
    void onComplete();
}
