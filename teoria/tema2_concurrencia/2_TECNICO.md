# Tema 2: Concurrencia, la parte técnica (para consultar)

> **Este archivo no se lee de un tirón: se consulta.** Cuando estés programando y no recuerdes cómo se escribe algo, búscalo aquí.
> Cada herramienta tiene siempre las mismas cuatro partes: **plantilla** (cópiala), **métodos**, **⚠ reglas** y **errores típicos**.
> Si no entiendes **para qué sirve** algo, vuelve a `1_HISTORIA.md`.

**Índice:**
1. [`wait` / `notifyAll`](#1-wait--notifyall)
2. [Pool de hilos](#2-pool-de-hilos-executorservice)
3. [`Callable` + `Future`](#3-callable--future)
4. [⭐ Plantilla completa: pool + Callable + lista de Future](#4--plantilla-completa-pool--callable--lista-de-future)
5. [`AtomicInteger`](#5-atomicinteger)
6. [`ReentrantLock`](#6-reentrantlock)
7. [Interbloqueo](#7-interbloqueo)
8. [`Semaphore`](#8-semaphore)
9. [`CountDownLatch`](#9-countdownlatch)
10. [Imports](#10-imports)

---

## 1. `wait` / `notifyAll`
🧪 `C01_ProductorConsumidor`

### Plantilla
```java
public synchronized void dejar(String plato) throws InterruptedException {
    while (platos.size() == capacidad) {   // WHILE, nunca if
        wait();                            // me duermo y SUELTO el cerrojo
    }
    platos.add(plato);
    notifyAll();                           // aviso a todos los que esperan
}

public synchronized String recoger() throws InterruptedException {
    while (platos.isEmpty()) {
        wait();
    }
    String plato = platos.remove(0);
    notifyAll();
    return plato;
}
```

### Métodos
| Método | Qué hace |
|---|---|
| `wait()` | Duerme el hilo **y suelta el cerrojo** hasta que le avisen |
| `notifyAll()` | Despierta a **todos** los que esperan en ese objeto |
| `notify()` | Despierta a **uno** cualquiera (más arriesgado: usa `notifyAll`) |

### ⚠ Reglas
- `wait` y `notify` **solo dentro de `synchronized`**, sobre el mismo objeto.
- La condición se comprueba con **`while`**: al despertar, otro puede haberse adelantado.
- `wait()` lanza `InterruptedException`.

### Errores típicos
| Error | Qué pasa |
|---|---|
| `if` en vez de `while` | Coges un plato que ya no existe |
| `wait()` fuera de `synchronized` | `IllegalMonitorStateException` |

---

## 2. Pool de hilos (`ExecutorService`)
🧪 `C02_PoolDeHilos`, parte 1

### Plantilla
```java
ExecutorService pool = Executors.newFixedThreadPool(3);   // 3 hilos fijos

pool.submit(() -> atender(cliente));                       // entregar una tarea (Runnable)

pool.shutdown();                                           // no acepta más tareas
pool.awaitTermination(5, TimeUnit.SECONDS);                // (opcional) esperar a que acaben
```

### Métodos
| Método | Qué hace |
|---|---|
| `Executors.newFixedThreadPool(n)` | Crea un pool de **n** hilos |
| `submit(tarea)` | Entrega una tarea. Devuelve un `Future` |
| `shutdown()` | No acepta más tareas; las que ya tiene, las termina |
| `awaitTermination(t, unidad)` | Espera, como mucho `t`, a que terminen. Devuelve `true` si han terminado |
| `isTerminated()` | ¿Han terminado ya todas? |

### ⚠ Reglas
- **Sin `shutdown()` el programa no termina:** los hilos del pool se quedan esperando trabajo.
- Los nombres de los hilos son `pool-1-thread-1`, `-2`… Con 3 hilos y 5 tareas, **algunos se repiten**.

---

## 3. `Callable` + `Future`
🧪 `C02_PoolDeHilos`, parte 2 · 📝 `WriteCSV`

### `Runnable` frente a `Callable`
| | `Runnable` | `Callable<T>` |
|---|---|---|
| Método | `public void run()` | `public T call() throws Exception` |
| ¿Devuelve algo? | No | **Sí, un `T`** |
| ¿Puede lanzar excepciones comprobadas? | No | **Sí** |

### Plantilla: `Callable` como clase propia
```java
public class MiTarea implements Callable<Path> {   // <Path> = lo que devuelve
    private final int numero;                       // los datos, por el constructor

    public MiTarea(int numero) {
        this.numero = numero;
    }

    @Override
    public Path call() throws Exception {           // ⚠ el mismo tipo que en <...>
        System.out.println("Hilo: " + Thread.currentThread().getName());
        // ... trabajo ...
        return resultado;
    }
}
```

### Plantilla: `Callable` como lambda
```java
Callable<Double> tarea = () -> {
    return 2.50;                                    // con return: es un Callable
};
```

### El viaje del tipo `<T>`
```
class MiTarea implements Callable<Path>   →  call() devuelve Path
pool.submit(new MiTarea(1))               →  devuelve Future<Path>
future.get()                              →  devuelve Path
```

### Métodos de `Future`
| Método | Qué hace |
|---|---|
| `get()` | **Espera** a que la tarea acabe y devuelve el resultado |
| `get(t, unidad)` | Igual, pero espera como mucho `t` (si no, `TimeoutException`) |
| `isDone()` | ¿Ya ha terminado? (no espera) |
| `cancel(true)` | Intenta cancelar la tarea |

### Excepciones de `get()`
| Excepción | Cuándo | Qué hacer |
|---|---|---|
| `ExecutionException` | **La tarea** lanzó un error dentro de `call()` | El error real está en `e.getCause()` |
| `InterruptedException` | Interrumpieron al hilo que esperaba en `get()` | Lo normal es propagarla con `throws` |

### Devolver dos cosas: un `record`
```java
record Resultado(File fichero, Path ruta) {}     // la "caja"

public class MiTarea implements Callable<Resultado> {
    public Resultado call() throws Exception {
        ...
        return new Resultado(ruta.toFile(), ruta);
    }
}

// al recoger:
Resultado r = future.get();
r.fichero();   r.ruta();                           // los getters del record
```

### ⚠ Reglas
- **Nunca llames tú a `call()`**: lo llama el hilo del pool. Si lo llamas tú, se ejecuta en el `main`, sin concurrencia.
- El tipo de `implements Callable<T>` y el que devuelve `call()` **tienen que coincidir**.
- Pon siempre el tipo en `Callable<T>`: sin él devuelve `Object`.

---

## 4. ⭐ Plantilla completa: pool + Callable + lista de Future

Es el esquema de `WriteCSV`, y vale para cualquier ejercicio del tipo *"N tareas en un pool de M hilos que devuelven algo"*.

```java
static void main() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(3);      // 1. el pool
    List<Future<Path>> futures = new ArrayList<>();              // 2. la lista de tickets

    for (int i = 1; i <= 5; i++) {                               // 3. BUCLE 1: repartir
        futures.add(pool.submit(new WriteCSV(i)));               //    guardo cada ticket
    }

    for (Future<Path> future : futures) {                        // 4. BUCLE 2: recoger
        Path ruta = future.get();                                //    espera a ESA tarea
        mostrarInfo(ruta);
    }

    pool.shutdown();                                             // 5. cerrar
}
```

### ⚠ Los dos bucles van separados
```java
// ❌ MAL: el get() dentro del bucle del submit
for (int i = 1; i <= 5; i++) {
    Future<Path> f = pool.submit(new WriteCSV(i));
    f.get();          // espera a que acabe ANTES de repartir la siguiente → trabaja 1 hilo cada vez
}
```

### Al pasar la lista a un método
```java
// DECLARACIÓN: tipo + nombre
public static void mostrarRutas(List<Future<Path>> futures) throws Exception { ... }

// LLAMADA: solo la variable
mostrarRutas(futures);          // ✅
mostrarRutas(Path);             // ❌ Path es un TIPO, no una variable
```

---

## 5. `AtomicInteger`
🧪 `C03_AtomicosYLocks`

### Plantilla
```java
AtomicInteger contador = new AtomicInteger(0);
contador.incrementAndGet();      // ++ seguro
contador.get();                  // leer
```

### Métodos
| Método | Equivale a |
|---|---|
| `incrementAndGet()` | `++x` (suma y devuelve el nuevo) |
| `getAndIncrement()` | `x++` (devuelve el viejo y suma) |
| `addAndGet(n)` | `x += n` |
| `get()` / `set(v)` | leer / escribir |
| `compareAndSet(esperado, nuevo)` | "Si vale `esperado`, ponlo a `nuevo`" (de un golpe) |

> También existen `AtomicLong`, `AtomicBoolean` y `AtomicReference<T>`.

---

## 6. `ReentrantLock`
🧪 `C03_AtomicosYLocks`

### Plantilla
```java
private final ReentrantLock lock = new ReentrantLock();   // (true) = justo

lock.lock();
try {
    // sección crítica
} finally {
    lock.unlock();            // ⚠ SIEMPRE en finally
}
```

### Plantilla con `tryLock`
```java
if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {   // espera como mucho 100 ms
    try {
        // sección crítica
    } finally {
        lock.unlock();
    }
} else {
    // no lo he conseguido: hago otra cosa
}
```

### ¿Cuál uso?
| Situación | Usa |
|---|---|
| Un contador o un valor simple | `AtomicInteger` |
| Un bloque de código normal | `synchronized` |
| Necesitas `tryLock`, un tiempo máximo o fairness | `ReentrantLock` |
| Muchas lecturas y pocas escrituras | `ReentrantReadWriteLock` (ver el tema 3) |

---

## 7. Interbloqueo
🧪 `C04_Interbloqueo`

### Las 4 condiciones de Coffman (tienen que darse **todas**)
| # | Condición | En la cafetería |
|---|---|---|
| 1 | Exclusión mutua | La cafetera la usa uno solo |
| 2 | Retención y espera | Tengo la cafetera y espero el molinillo sin soltarla |
| 3 | No expropiación | Nadie me la puede quitar |
| 4 | Espera circular | A espera a B y B espera a A |

### Cómo evitarlo (rompe **una**)
```java
// ✅ MISMO ORDEN en todos los hilos
synchronized (CAFETERA) {
    synchronized (MOLINILLO) {
        // preparar café
    }
}
```
También vale `tryLock` con tiempo máximo: si no consigo el segundo, suelto el primero y reintento.

### ⚠ Cómo se nota
El programa **se congela sin ningún error**. Los hilos quedan en estado `BLOCKED`.

---

## 8. `Semaphore`
🧪 `C05_SemaforoYCountDownLatch`

### Plantilla
```java
Semaphore mesas = new Semaphore(2);   // 2 permisos

mesas.acquire();                      // coger uno (espera si no quedan)
try {
    // usar la mesa
} finally {
    mesas.release();                  // ⚠ devolverlo SIEMPRE
}
```

| Método | Qué hace |
|---|---|
| `acquire()` | Coge un permiso; si no quedan, espera |
| `release()` | Devuelve un permiso |
| `tryAcquire()` | Lo intenta sin esperar (`true` / `false`) |
| `availablePermits()` | Cuántos quedan libres |

---

## 9. `CountDownLatch`
🧪 `C05_SemaforoYCountDownLatch`

### Plantilla
```java
CountDownLatch listos = new CountDownLatch(3);   // cuenta atrás desde 3

// cada hilo, al llegar:
listos.countDown();                               // resta 1

// el que espera:
listos.await();                                   // espera hasta que llegue a 0
```

| | `join()` | `CountDownLatch` |
|---|---|---|
| Espera a… | que un hilo **termine** | que ocurra un **evento** N veces |
| ¿Reutilizable? | — | **No**: de un solo uso |

---

## 10. Imports
```java
import java.util.concurrent.*;                 // ExecutorService, Executors, Callable, Future,
                                               // ExecutionException, TimeUnit, Semaphore, CountDownLatch
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
```
> ⚠ Si IntelliJ te mete un import raro al autocompletar (como `sun.swing.SwingUtilities2.submit`, que te pasó en `WriteCSV`), bórralo.
