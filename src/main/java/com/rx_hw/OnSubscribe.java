package com.rx_hw;

/**
 * Функциональный интерфейс, задающий логику эмиссии элементов
 */
@FunctionalInterface
public interface OnSubscribe<T> {
    void call(Observer<? super T> observer);
}

