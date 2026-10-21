# Tema 1: Hilos, la parte técnica (para consultar)

> **Este archivo no se lee de un tirón: se consulta.** Cuando estés programando y no recuerdes cómo se escribe algo, búscalo aquí.
> Cada apartado tiene **plantilla** (cópiala), **métodos**, **⚠ reglas** y **errores típicos**.
> Si no entiendes **para qué sirve** algo, vuelve a `1_HISTORIA.md`.

**Índice:**
1. [Crear y arrancar hilos](#1-crear-y-arrancar-hilos)
2. [Ciclo de vida](#2-ciclo-de-vida)
3. [`join`, `sleep` e `interrupt`](#3-join-sleep-e-interrupt)
4. [⭐ Plantilla completa: varios hilos + esperar + resultado](#4--plantilla-completa-varios-hilos--esperar--resultado)
5. [`synchronized`](#5-synchronized)
6. [Colecciones seguras para hilos](#6-colecciones-seguras-para-hilos)

---

## 1. Crear y arrancar hilos
🧪 `H01_CrearHilos`

### Plantilla: `implements Runnable` (la recomendada)
```java
public class Camarero implements Runnable {
    private final int id;                    // los datos, por el constructor

    public Camarero(int id) {
        this.id = id;
    }

    @Override
    public void run() {                      // el ENCARGO
        System.out.println("Camarero " + id + " en " + Thread.currentThread().getName());
    }
}

// lanzarlo
Thread t = new Thread(new Camarero(1), "Hilo-Camarero-1");   // el nombre es opcional
t.start();
```

### Otras dos formas
```java
// lambda (Runnable es una interfaz funcional)
new Thread(() -> System.out.println("hola")).start();

// extends Thread (peor: ya no puedes heredar de otra clase)
class Cocinero extends Thread {
    public void run() { ... }
}
new Cocinero().start();
```

### `start()` frente a `run()`
| Llamada | Qué pasa |
|---|---|
| `t.start()` | Crea un **hilo nuevo** que ejecuta `run()` ✅ |
| `t.run()` | Ejecuta `run()` **en el hilo actual**, sin concurrencia ❌ |

### ⚠ Reglas
- `start()` **una sola vez** por hilo: la segunda lanza `IllegalThreadStateException`.
- **El orden de ejecución no está garantizado.** Nunca supongas que un hilo va antes que otro.

---

## 2. Ciclo de vida
```
NEW ──start()──▶ RUNNABLE ◀──▶ BLOCKED / WAITING / TIMED_WAITING
                    │
                    └── fin de run() ──▶ TERMINATED
```

| Estado | Cuándo |
|---|---|
| `NEW` | Creado, sin `start()` |
| `RUNNABLE` | Ejecutándose o listo para ejecutarse |
| `BLOCKED` | Esperando un candado (`synchronized`) que tiene otro |
| `WAITING` | Esperando sin límite (`join()`, `wait()`) |
| `TIMED_WAITING` | Esperando con límite (`sleep(ms)`, `join(ms)`) |
| `TERMINATED` | Ha acabado `run()` |

Se consulta con `t.getState()` o con `t.isAlive()` (`true` si ya ha arrancado y aún no ha terminado).

---

## 3. `join`, `sleep` e `interrupt`
🧪 `H02_JoinSleepInterrupt`

### Métodos
| Método | Qué hace |
|---|---|
| `t.join()` | El hilo que lo llama **espera** a que `t` termine |
| `t.join(ms)` | Igual, pero espera como mucho `ms` |
| `Thread.sleep(ms)` | Duerme el hilo **actual** |
| `t.interrupt()` | **Pide** a `t` que pare (no lo mata) |
| `Thread.currentThread().isInterrupted()` | ¿Me han pedido que pare? |
| `Thread.currentThread()` | El hilo que ejecuta esa línea |
| `getName()` / `setName()` | Nombre del hilo |

### Plantilla: `sleep` con su excepción
```java
try {
    Thread.sleep(100);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();   // vuelvo a poner la "nota" (el catch la borra)
}
```

### Plantilla: un hilo que se puede interrumpir
```java
Thread t = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // trabajar...
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            break;                         // me han interrumpido mientras dormía: salgo
        }
    }
    System.out.println("salgo limpiamente");
});
t.start();
...
t.interrupt();
```

### ⚠ Reglas
- `join()` y `sleep()` lanzan `InterruptedException`: hace falta un `try/catch` o un `throws`.
- `sleep` es **estático**: duerme al hilo **que lo ejecuta**, no a otro.

---

## 4. ⭐ Plantilla completa: varios hilos + esperar + resultado
Es el esquema de `abrirCafeteria`, y vale para cualquier ejercicio del tipo *"N hilos que trabajan y al final muestro un resumen"*.

```java
public static void main(String[] args) throws InterruptedException {
    // 1. lo compartido
    Queue<String> cola = new ConcurrentLinkedQueue<>(List.of("Cliente#1", "Cliente#2", "Cliente#3"));

    // 2. crear los hilos
    Thread[] camareros = new Thread[3];
    for (int i = 0; i < camareros.length; i++) {
        camareros[i] = new Thread(new Camarero(i + 1, cola));   // lo compartido, por el constructor
    }

    // 3. arrancarlos (TODOS primero)
    for (Thread t : camareros) t.start();

    // 4. esperarlos (TODOS después)
    for (Thread t : camareros) t.join();

    // 5. resultado: aquí ya han terminado todos
    System.out.println("Cola vacía: " + cola.isEmpty());
}
```

### ⚠ `start` y `join` van en bucles separados
```java
// ❌ MAL: arranco uno y lo espero antes de arrancar el siguiente → van de uno en uno
for (Thread t : camareros) {
    t.start();
    t.join();
}
```
> Es el mismo error que el `get()` dentro del bucle del `submit` del tema 2.

### ⚠ No hace falta `System.exit(0)`
Después de los `join()` todos han terminado, y el programa acaba solo.

---

## 5. `synchronized`
🧪 `H03_CondicionCarreraSynchronized`

### Plantilla: bloque con un candado compartido
```java
private static final Object LOCK = new Object();   // ⚠ static final: EL MISMO para todos

synchronized (LOCK) {
    // sección crítica: de uno en uno
}
```

### Plantilla: método sincronizado
```java
public synchronized void cobrar(int euros) {       // el candado es "this" (el objeto)
    caja += euros;
}
```
> ⚠ `synchronized` en un método **de instancia** usa como candado **ese objeto**. Si cada hilo tiene **su propio objeto**, no se protegen entre sí. El objeto tiene que ser **compartido**, por ejemplo pasando el mismo `StoreStats` a todos por el constructor.

### ⚠ Reglas
- **El candado tiene que ser compartido.** Uno por hilo no protege nada.
- **Solo lo imprescindible dentro.**
- Se libera **solo** al salir del bloque, incluso con una excepción.
- `contador++` **no es atómico** (son tres pasos: leer, sumar y escribir).

### Vocabulario
| Término | Qué es |
|---|---|
| Recurso compartido | Dato al que acceden varios hilos |
| Condición de carrera | El resultado depende del orden de los hilos |
| Sección crítica | El código que solo debe ejecutar un hilo a la vez |
| Exclusión mutua | Garantía de que solo entra uno |

---

## 6. Colecciones seguras para hilos
🧪 `H04_ColeccionesConcurrentes`

| Normal | Segura para hilos | Cuándo |
|---|---|---|
| `LinkedList` como `Queue` | `ConcurrentLinkedQueue` | Cola de clientes o tareas |
| `HashMap` | `ConcurrentHashMap` | Contar por clave, cachés |
| `ArrayList` | `CopyOnWriteArrayList` | Lista que se lee mucho y cambia poco |

### ⚠ Comprobar y luego actuar
```java
// ❌ MAL: dos operaciones → hueco en medio
while (!cola.isEmpty()) {
    String c = cola.poll();           // otro hilo se lo ha llevado → c == null
}

// ✅ BIEN: una sola operación
String c;
while ((c = cola.poll()) != null) {   // poll() devuelve null si estaba vacía
    atender(c);
}
```

### Contar por clave sin perder cuentas
```java
ConcurrentHashMap<Integer, Integer> atendidos = new ConcurrentHashMap<>();
atendidos.merge(idCamarero, 1, Integer::sum);     // suma 1 de forma atómica
```
