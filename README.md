# Кастомная реализация RXJava

Проект представляет собой собственную реализацию базовой функциональности библиотеки RxJava с поддержкой реактивных потоков, управления потоками выполнения (Schedulers), обработки ошибок и основных операторов преобразования данных.

---

## 1. Архитектура системы

1. **Observer<T>** — интерфейс наблюдателя с методами:

    * `onNext(T item)` — получение элемента;
    * `onError(Throwable t)` — обработка ошибки;
    * `onComplete()` — сигнал завершения потока.

2. **Observable<T>** — главный класс-источник реактивного потока:

    * **`create(OnSubscribe<T>)`** — фабрика, создающая Observable из лямбды эмиссии;
    * **`subscribe(Observer<? super T>)`** — синхронная подписка;
    * **`subscribeWithDisposable(Observer)`** — асинхронная подписка с возможностью отмены (Disposable);
    * Блоки-операторы:

        * `map(Function<? super T, ? extends R>)`;
        * `filter(Predicate<? super T>)`;
        * `flatMap(Function<? super T, Observable<? extends R>>)`;
        * `subscribeOn(Scheduler)`;
        * `observeOn(Scheduler)`.

3. **Disposable** — интерфейс для отмены подписки (`dispose()`, `isDisposed()`).

4. **Schedulers** — управление потоками:

    * **Scheduler** — интерфейс с методом `execute(Runnable)`.
    * **IOThreadScheduler** — `Executors.newCachedThreadPool()` для I/O задач.
    * **ComputationScheduler** — `Executors.newFixedThreadPool(#cpu)` для вычислений.
    * **SingleThreadScheduler** — `Executors.newSingleThreadExecutor()` для последовательных задач.

---

## 2. Принципы работы Schedulers

* **IOThreadScheduler**

    * Использует **неограниченный пул** потоков (`CachedThreadPool`).
    * Оптимально для задач ввода-вывода (сеть, файловая система).

* **ComputationScheduler**

    * Фиксированный пул по количеству ядер ЦПУ.
    * Используется для CPU-bound задач (математические вычисления).

* **SingleThreadScheduler**

    * Один поток, последовательная обработка.
    * Идеален для упорядоченных задач или доступа к не потокобезопасным ресурсам.

* **`subscribeOn`**

    * Переносит **весь** процесс эмиссии (вызов `onSubscribe`) в указанный Scheduler.

* **`observeOn`**

    * Переносит **вызовы** `onNext`, `onError`, `onComplete` в указанный Scheduler.

---

## 3. Операторы и обработка ошибок

### Операторы

1. **map(Function\<T,R>)**

    * Применяет функцию к каждому элементу и эмитит результат.
    * В случае исключения в функции mapper трансляция ошибок идет сразу в `onError()`.

2. **filter(Predicate<T>)**

    * Пропускает только те элементы, для которых предикат возвращает `true`.
    * Исключения в предикате также направляются в `onError()`.

3. **flatMap(Function\<T, Observable<R>>)**

    * Для каждого входного элемента создаёт вложенный `Observable` и объединяет все выходные элементы.
    * Управление завершением и ошибками осуществляется через внутренний счётчик активных потоков (WIP) и флаг ошибки.

### Обработка ошибок

* В каждом операторе (`map`, `filter`, `flatMap`) обёртки гарантируют, что при возникновении `Throwable`:

    * сразу вызывается `observer.onError(throwable)`;
    * дальнейшая эмиссия приостанавливается.
* Тестирование ошибок покрывает все три оператора и подписку с `Disposable`, подтверждая вызов `onError()`.

---

## 3. Процесс тестирования

* Использован **JUnit 5 (Jupiter)** для юнит-тестов.

* **Основные сценарии**:

    1. **testMapAndFilter** — проверка `map` и `filter` на последовательном потоке.
    2. **testSubscribeOnObserveOn** — корректность переноса потоков `subscribeOn`/`observeOn`.
    3. **testFlatMap** — развёртывание вложенных Observable.
    4. **testDisposableStopsEmission** — остановка эмиссии через `dispose()`.
    5. **testMapError**, **testFilterError**, **testFlatMapError** — проверка передачи ошибок в `onError`.

* Асинхронные тесты используют `CountDownLatch` для синхронизации.

* Покрытие основных операторов и многопоточных сценариев.

---

## 4. Примеры использования

```java
// Создание потока чисел 1..5
Observable<Integer> source = Observable.create(obs -> {
    for (int i = 1; i <= 5; i++) obs.onNext(i);
});

// Цепочка операторов с переключением потоков
source
    .subscribeOn(new IOThreadScheduler())
    .map(i -> i * 10)
    .filter(i -> i % 20 == 0)
    .observeOn(new SingleThreadScheduler())
    .subscribe(new Observer<Integer>() {
        @Override public void onNext(Integer item) {
            System.out.println("Received: " + item + " on " + Thread.currentThread().getName());
        }
        @Override public void onError(Throwable t) {
            System.err.println("Error: " + t.getMessage());
        }
        @Override public void onComplete() {
            System.out.println("Completed");
        }
    });
// Дадим асинхронным задачам время
Thread.sleep(500);
```
---
