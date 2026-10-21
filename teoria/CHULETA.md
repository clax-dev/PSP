# Chuleta de PSP: las frases clave de todos los temas

> Para repasar en 5 minutos antes de un examen. Si una frase no te dice nada, vuelve al `1_HISTORIA.md` de su tema.

## Tema 1: Hilos → `tema1_hilos/`
| Concepto | Frase clave | Cómo se escribe |
|---|---|---|
| Proceso / hilo | La cafetería y sus camareros, que comparten todo | — |
| Orden | Lo decide el sistema operativo: **no está garantizado** | — |
| Crear | El encargo (`Runnable`) y el camarero (`Thread`) | `new Thread(new Camarero(1)).start()` |
| `start` / `run` | `start` = otro trabaja; `run` = lo haces tú | **Nunca** `run()` |
| `join` | Espero a que acabes | Todos los `start()` y **después** todos los `join()` |
| `interrupt` | Una nota pidiendo que pare (no lo mata) | `isInterrupted()` |
| Condición de carrera | Dos tocan lo mismo y se pisan | `contador++` no es atómico |
| `synchronized` | Candado, **el mismo para todos**; solo lo imprescindible | `synchronized (LOCK) { }` con `static final` |
| Comprobar y actuar | Pregunta y coge en un solo paso | `while ((c = cola.poll()) != null)` |

## Tema 2: Concurrencia → `tema2_concurrencia/`
| Concepto | Frase clave | Cómo se escribe |
|---|---|---|
| `wait` / `notifyAll` | Me duermo, suelto y me avisan | `while (no puedo) wait();` dentro de `synchronized` |
| Pool | Plantilla fija de camareros que se turnan | `Executors.newFixedThreadPool(3)` … `shutdown()` |
| `Callable` | Tarea que **devuelve** algo | `implements Callable<Path>` → `public Path call()` |
| `Future` | El ticket del pedido | `submit` → ticket · `get()` → resultado |
| Dos bucles | Primero repartir todo, después recoger todo | Bucle de `submit` + bucle de `get` |
| Errores en `call()` | Viajan dentro del ticket | `get()` lanza `ExecutionException` → `getCause()` |
| `AtomicInteger` | Sumar de un golpe | `incrementAndGet()` |
| `ReentrantLock` | Candado con paciencia limitada | `lock()` + `try` + `finally { unlock(); }` · `tryLock(t)` |
| Interbloqueo | Todos cogen las cosas en el mismo orden | 4 condiciones de Coffman; rompe una |
| `Semaphore` | Solo caben N | `acquire()` / `release()` en `finally` |
| `CountDownLatch` | No empiezo hasta que lleguen todos | `countDown()` / `await()` |

## Tema 3: Locks de lectura y escritura → `tema3_locks/`
| Concepto | Frase clave | Cómo se escribe |
|---|---|---|
| El problema | Leer a la vez no es peligroso; leer mientras alguien escribe, sí | — |
| `ReentrantReadWriteLock` | Pase de lector compartido + pase de escritor único | `rw.readLock()` / `rw.writeLock()` |
| El escritor | Espera a que salgan todos; mientras escribe, no entra nadie | — |
| Fairness | Por orden de llegada: sin starvation, pero más lento | `new ReentrantReadWriteLock(true)` |
| Subir | Con la lectura no me dan la escritura: me espero a mí mismo | Soltar → pedir escritura → **volver a comprobar** |
| Lectura optimista | Leo sin permiso, compruebo y, si cambió, repito con permiso | `tryOptimisticRead()` → copiar → `validate(s)` → si `false`, `readLock()` |
| `StampedLock` | No es reentrante; la lectura optimista no bloquea a los escritores | `unlockRead(s)` / `unlockWrite(s)` |
| Cuándo no compensa | Lecturas cortísimas, muchas escrituras, pocos hilos | `synchronized` |

## Las 4 reglas que sirven para todo
1. **El orden de los hilos no está garantizado.**
2. **Lo que coges a mano, lo sueltas en un `finally`** (`unlock`, `release`). `synchronized` se suelta solo.
3. **Primero lanzar todo y después esperar todo** (`start`/`join` y `submit`/`get`, en bucles separados).
4. **Entre comprobar y actuar hay un hueco**: hazlo en un paso o vuelve a comprobar.
