# Chuleta rápida de PSP

## Hilos
- Crear y arrancar: `new Thread(runnable).start()`. **Nunca** `run()`.
- `Runnable` es una interfaz funcional, así que también vale `new Thread(() -> { ... })`.
- Esperar: `join()`. Dormir: `Thread.sleep(ms)`. Parar: `interrupt()` + comprobar `isInterrupted()`.
- Para un recurso compartido: `synchronized (LOCK_COMPARTIDO) { ... }` o una colección `Concurrent...`.
- Nada de "comprobar y luego actuar" en dos pasos: haz `while ((x = cola.poll()) != null)`.
- **El orden en que se ejecutan los hilos no está garantizado.**

> Las lambdas, las colecciones y los streams están en el proyecto **AC** (`C:\DAM\Segundo\AC`).
