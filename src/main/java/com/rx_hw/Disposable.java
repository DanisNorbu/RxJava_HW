package com.rx_hw;


/**
 * Интерфейс для отмены подписки на Observable
 */
public interface Disposable {
    /** Прервать дальнейшую эмиссию событий */
    void dispose();
    /** Проверить, отменена ли подписка */
    boolean isDisposed();
}

