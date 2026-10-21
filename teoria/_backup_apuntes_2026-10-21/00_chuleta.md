# Chuleta rápida de PSP

## Hilos
- Crear y arrancar: `new Thread(runnable).start()`. **Nunca** `run()`.
- `Runnable` es una interfaz funcional, así que también vale `new Thread(() -> { ... })`.
- Esperar: `join()`. Dormir: `Thread.sleep(ms)`. Parar: `interrupt()` + comprobar `isInterrupted()`.
- Para un recurso compartido: `synchronized (LOCK_COMPARTIDO) { ... }` o una colección `Concurrent...`.
- Nada de "comprobar y luego actuar" en dos pasos: haz `while ((x = cola.poll()) != null)`.
- **El orden en que se ejecutan los hilos no está garantizado.**

## Concurrencia
- Para esperar a otro hilo, `while (condición) wait();` dentro de `synchronized`, y avisar con `notifyAll()`.
- Pool de hilos: `Executors.newFixedThreadPool(n)` → `submit(...)` → `shutdown()` + `awaitTermination()`.
- Si la tarea devuelve algo, usa un `Callable<T>`: `submit` te da un `Future<T>`, y `get()` espera el resultado.
- Para contadores, `AtomicInteger.incrementAndGet()`.
- Con `ReentrantLock`, `lock()` y `unlock()` **en `finally`**; con `tryLock(t)` no te quedas bloqueado para siempre.
- **Interbloqueo:** todos los hilos cogen los recursos **en el mismo orden**.
- `Semaphore(n)` permite N a la vez; `CountDownLatch(n)` espera a que ocurran N eventos.

## Locks de lectura y escritura
- `ReentrantReadWriteLock`: `readLock()` es compartido (muchos lectores) y `writeLock()` exclusivo (uno, sin lectores).
- Con el `readLock` cogido **no** se puede pedir el `writeLock` (bloqueo). Al revés sí.
- `(true)` = justo: sin starvation, pero más lento.
- `StampedLock`: `tryOptimisticRead()` → copiar a locales → `validate(stamp)`; si da `false`, repetir con `readLock()`.
- La lectura optimista **no bloquea** a los escritores. `StampedLock` **no es reentrante**.
- No compensa con lecturas cortísimas, con muchas escrituras o con pocos hilos.

> Las lambdas, las colecciones y los streams están en el proyecto **AC** (`C:\DAM\Segundo\AC`).
