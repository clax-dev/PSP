# Tema 2: Concurrencia

> **Ejercicios:** `ejercicios/src/tema2_concurrencia/` (vacía; aquí irán los ejercicios del tema)
> **Código de teoría:** `teoria/src/t2_concurrencia/` (archivos C01 a C05). Continúan el ejemplo de la cafetería del tema 1.
> **Requisito:** el tema 1 (crear hilos, `join`, `synchronized`).

---

## 1. Qué añade este tema

En el tema 1 los hilos solo tenían que **no pisarse** (`synchronized`). Aquí además tienen que **coordinarse**:

| Necesito… | Herramienta | Demo |
|---|---|---|
| Que un hilo **espere** a que otro le deje algo | `wait()` / `notifyAll()` | C01 |
| **Reutilizar** hilos en lugar de crear uno por tarea | `ExecutorService` (pool) | C02 |
| Que una tarea **devuelva un resultado** | `Callable` + `Future` | C02 |
| Un **contador** seguro sin `synchronized` | `AtomicInteger` | C03 |
| Un cerrojo con **tiempo máximo** de espera | `ReentrantLock` + `tryLock` | C03 |
| **Evitar** que los hilos se bloqueen para siempre | Orden fijo de recursos | C04 |
| Limitar a **N** hilos a la vez | `Semaphore` | C05 |
| Esperar a que **ocurra algo** (todos han llegado) | `CountDownLatch` | C05 |

---

## 2. Productor / consumidor: `wait()` y `notifyAll()` (C01)

Un **cocinero** (productor) deja platos en una **barra** de 3 huecos, y los **camareros** (consumidores) los recogen.
- Si la barra está **llena**, el cocinero espera.
- Si está **vacía**, el camarero espera.

```java
public synchronized void dejar(String plato) throws InterruptedException {
    while (platos.size() == capacidad) {   // WHILE, nunca if
        wait();                            // duermo y SUELTO el cerrojo
    }
    platos.add(plato);
    notifyAll();                           // aviso a los que esperan
}
```

| Método | Qué hace |
|---|---|
| `wait()` | El hilo se duerme **y suelta el cerrojo** hasta que alguien le avise |
| `notifyAll()` | Despierta a **todos** los hilos que esperan en ese cerrojo |
| `notify()` | Despierta a **uno** cualquiera (más arriesgado: mejor `notifyAll`) |

**Reglas de oro:**
1. `wait` y `notify` **solo dentro de `synchronized`** sobre el mismo objeto. Si no, salta `IllegalMonitorStateException`.
2. La condición se comprueba con **`while`**, nunca con `if`: al despertar, otro hilo puede haberse adelantado.
3. `wait()` lanza `InterruptedException`.

---

## 3. Pool de hilos: `ExecutorService` (C02)

Crear un hilo por tarea es caro. Un **pool** es una plantilla **fija** de hilos que se van turnando las tareas.

```java
ExecutorService pool = Executors.newFixedThreadPool(3);   // 3 hilos reutilizables
pool.submit(() -> atender(cliente));                       // entregar tareas
pool.shutdown();                                           // no aceptar más
pool.awaitTermination(5, TimeUnit.SECONDS);                // esperar (como join)
```

> ⚠ Sin `shutdown()` el programa **no termina**: los hilos del pool se quedan vivos esperando trabajo.

### Runnable frente a Callable

| | `Runnable` | `Callable<T>` |
|---|---|---|
| Método | `run()` | `call()` |
| ¿Devuelve resultado? | No | **Sí**, un `T` |
| ¿Puede lanzar excepciones comprobadas? | No | Sí |

`submit(callable)` devuelve un **`Future<T>`**, que es como un resguardo del resultado:
- `future.get()` espera a que la tarea acabe y devuelve el resultado.
- `future.isDone()` dice si ya ha terminado.

Los dos son interfaces funcionales, así que se escriben como **lambdas**: `() -> { ... }` y `() -> { ...; return valor; }`.

### `Callable` como clase propia (no lambda)

Cuando la tarea es larga, en lugar de una lambda se hace una **clase** que implementa `Callable<T>`. Es lo mismo que hacer una clase que implementa `Runnable`, como el `Camarero` de la cafetería, pero con `call()` en vez de `run()`:

| Pieza | Qué es |
|---|---|
| `implements Callable<T>` | La `T` es **el tipo de lo que devuelve** la tarea. `Callable<Integer>` devuelve un `Integer`, `Callable<Path>` devuelve un `Path`… |
| `public T call() throws Exception` | El trabajo de la tarea. Termina con `return` de un valor de tipo `T`. Puede lanzar excepciones (por ejemplo, `IOException` al escribir un fichero) |
| Atributos + constructor | Los datos que necesita la tarea se le pasan **al crearla**, por el constructor, igual que el `ID_CAMARERO` |

> ⚠️ El tipo de `implements Callable<T>` y el que devuelve `call()` **tienen que coincidir**. Si pones `Callable<Path>`, el método es `public Path call()`.
> ⚠️ `Callable` no tiene genérico "por defecto": sin `<T>` devuelve `Object` y luego hay que hacer *cast*. Pon siempre el tipo.

### ¿Y si la tarea tiene que devolver **dos** cosas?

Un método solo puede devolver **un** valor. Para devolver dos, se meten en **una caja** y se devuelve la caja. En Java moderno, la caja más cómoda es un **`record`**:

| Opción | Cómo | Cuándo |
|---|---|---|
| Devolver solo una, y sacar la otra de ella | `Callable<Path>` + quien lo recibe hace `ruta.toFile()` | Si una se puede obtener de la otra (como `Path` ↔ `File`) |
| Un `record` con las dos | `record Resultado(File fichero, Path ruta) {}` + `Callable<Resultado>` | Cuando el enunciado pide devolver las dos |

Después, quien recibe el resultado lo saca de la caja con los getters del record: `resultado.fichero()` y `resultado.ruta()`.

### Recoger el resultado: `Future.get()` y sus excepciones

`future.get()` lanza dos excepciones comprobadas, y hay que tratarlas:

| Excepción | Cuándo |
|---|---|
| `InterruptedException` | Si interrumpen al hilo que está esperando en el `get()` |
| `ExecutionException` | Si **la tarea** lanzó una excepción dentro de `call()`. La original va dentro: `e.getCause()` |

> 💡 Si el `call()` falla, por ejemplo con una `IOException` al escribir, **no ves el error hasta que haces `get()`**, y te llega envuelto en una `ExecutionException`. Para saber qué pasó de verdad, imprime `e.getCause()`.
> ⚠️ No olvides `shutdown()` del pool al final, o el programa no termina.

---

## 4. Atómicos y `ReentrantLock` (C03)

**`AtomicInteger`**: sus operaciones se hacen "de un golpe", sin que otro hilo se meta en medio. Es lo ideal para contadores.
```java
AtomicInteger contador = new AtomicInteger(0);
contador.incrementAndGet();   // seguro sin synchronized
```

**`ReentrantLock`**: un cerrojo "manual", con más control que `synchronized`.
```java
lock.lock();
try {
    // sección crítica
} finally {
    lock.unlock();            // ⚠ SIEMPRE en finally
}

if (lock.tryLock(100, TimeUnit.MILLISECONDS)) { ... } else { /* hago otra cosa */ }
```

| Situación | Usa |
|---|---|
| Un contador o un valor simple | `AtomicInteger` / `AtomicLong`… |
| Un bloque de código normal | `synchronized` (lo más sencillo) |
| Necesitas `tryLock`, un tiempo máximo o un orden justo de llegada | `ReentrantLock` |

---

## 5. Interbloqueo (deadlock) (C04)

Dos hilos se esperan **para siempre**, cada uno por el recurso que tiene el otro:

```
Camarero-A tiene la cafetera  y espera el molinillo
Camarero-B tiene el molinillo y espera la cafetera   → el programa se congela, sin ningún error
```

**Condiciones de Coffman.** Tienen que cumplirse **las 4 a la vez**:
1. **Exclusión mutua:** el recurso solo lo usa un hilo cada vez.
2. **Retención y espera:** tengo un recurso y espero otro sin soltar el primero.
3. **No expropiación:** nadie me lo puede quitar.
4. **Espera circular:** A espera a B y B espera a A.

**Cómo evitarlo** (basta con romper una de las 4):
- ✅ **Mismo orden para todos**: todos cogen primero la cafetera y después el molinillo. Es lo más sencillo.
- ✅ `tryLock` con tiempo máximo: si no consigo el segundo recurso, suelto el primero.

---

## 6. `Semaphore` y `CountDownLatch` (C05)

**`Semaphore`** es un contador de **permisos**. Limita cuántos hilos usan algo **a la vez**:
```java
Semaphore mesas = new Semaphore(2);   // 2 mesas
mesas.acquire();                      // coger una (espera si no quedan)
try { ... } finally { mesas.release(); }
```

**`CountDownLatch`** es una **cuenta atrás**: unos hilos esperan hasta que otros la hagan llegar a 0.
```java
CountDownLatch listos = new CountDownLatch(3);
listos.countDown();   // "yo ya estoy"
listos.await();       // espera a que lleguen todos
```

| | `join()` | `CountDownLatch` |
|---|---|---|
| Espera a… | que un hilo **termine** | que ocurra un **evento** (aunque los hilos sigan vivos) |
| ¿Reutilizable? | — | No: se usa una sola vez |

---

## 7. Errores típicos de concurrencia

| Error | Consecuencia | Solución |
|---|---|---|
| `if` en lugar de `while` antes de `wait()` | Se coge un plato que ya no existe | `while (condición) wait();` |
| `wait()` fuera de `synchronized` | `IllegalMonitorStateException` | Ponerlo dentro del bloque sincronizado |
| `unlock()` o `release()` fuera de `finally` | Si hay una excepción, el cerrojo no se libera nunca | Siempre en `finally` |
| Olvidar `pool.shutdown()` | El programa no termina | `shutdown()` + `awaitTermination()` |
| Coger recursos en orden distinto | Interbloqueo | Mismo orden en todos los hilos |
