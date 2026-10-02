# Tema 1: Programación con hilos (lo básico)

> **Ejercicio de referencia:** `ejercicios/src/tema1_hilos/Cafeteria.java`
> **Código de teoría:** `teoria/src/t1_hilos/` (archivos H01 a H04; ejecuta varias veces cada uno, porque el orden cambia)

---

## 1. Conceptos

- **Proceso**: un programa en ejecución, con su propia memoria.
- **Hilo**: una línea de ejecución dentro de un proceso. Los hilos de un mismo proceso **comparten memoria**.
- **Concurrencia**: varias tareas avanzan "a la vez". **El orden de ejecución no está garantizado**, porque lo decide el sistema operativo.
- Todo programa empieza con un hilo, el **`main`**.

## 2. Cómo crear un hilo (`H01_CrearHilos`)

```java
// 1. implements Runnable  ← la recomendada (Camarero)
class Camarero implements Runnable { public void run() { ... } }
new Thread(new Camarero(1)).start();

// 2. extends Thread  ← peor opción, porque solo se puede heredar de una clase
class Cocinero extends Thread { public void run() { ... } }
new Cocinero().start();

// 3. lambda  ← Runnable es una interfaz funcional
new Thread(() -> System.out.println("hola")).start();
```

### `start()` frente a `run()`

| | Resultado |
|---|---|
| `t.start()` | Crea un **hilo nuevo** que ejecuta `run()` ✅ |
| `t.run()` | Ejecuta `run()` **en el hilo actual**, sin concurrencia ❌ |

Un hilo solo se puede arrancar una vez. Llamar dos veces a `start()` lanza `IllegalThreadStateException`.

### Ciclo de vida

```
NEW --start()--> RUNNABLE <--> BLOCKED / WAITING / TIMED_WAITING --fin de run()--> TERMINATED
```

## 3. Métodos básicos (`H02_JoinSleepInterrupt`)

| Método | Qué hace |
|---|---|
| `t.join()` | El hilo que lo llama **espera** a que `t` termine |
| `Thread.sleep(ms)` | Duerme el hilo **actual** |
| `t.interrupt()` | **Pide** a `t` que pare; no lo mata a la fuerza |
| `isInterrupted()` | ¿Me han pedido que pare? |
| `Thread.currentThread()` | Devuelve el hilo que ejecuta esa línea |
| `t.isAlive()` / `t.getState()` | ¿Sigue vivo? ¿En qué estado está? |

`join()` y `sleep()` lanzan `InterruptedException`, así que necesitan `try/catch`. Al capturarla, el aviso de interrupción se borra, y por eso conviene volver a marcarlo con `Thread.currentThread().interrupt()`.

**Esquema típico (`abrirCafeteria`):**
1. Preparar los datos compartidos.
2. Crear los hilos.
3. Arrancarlos con `start()`.
4. Esperarlos con `join()`.
5. Mostrar el resultado.

## 4. Condición de carrera y `synchronized` (`H03`)

- **Recurso compartido**: un dato al que acceden varios hilos (una variable `static`, una lista, la consola…).
- **Condición de carrera**: el resultado depende del orden en que se ejecutan los hilos. `contador++` **no es atómico** (son tres pasos: leer, sumar y escribir), así que algunos incrementos se pierden.
- **Sección crítica**: el código que solo debe ejecutar un hilo a la vez.

```java
private static final Object LOCK = new Object();   // el MISMO objeto para todos los hilos
synchronized (LOCK) { /* sección crítica */ }

public synchronized void cobrar(int e) { ... }     // el cerrojo es this
```

> ⚠ El cerrojo tiene que ser **compartido** (`static final`). Si cada hilo tuviera el suyo, no habría exclusión.
> ⚠ Sincroniza **solo lo imprescindible**, porque dentro del bloque los hilos van de uno en uno.

En `Cafeteria`, el `synchronized (LOCK)` sirve para que las tres líneas del diálogo de cada camarero salgan juntas, sin mezclarse con las de otro.

## 5. Colecciones seguras para hilos (`H04`)

| Normal | Segura para hilos |
|---|---|
| `LinkedList` usada como `Queue` | `ConcurrentLinkedQueue` |
| `HashMap` | `ConcurrentHashMap` |
| `ArrayList` | `CopyOnWriteArrayList` |

### ⚠ "Comprobar y luego actuar"

Aunque cada operación de la colección sea segura, **dos operaciones seguidas no lo son**:

```java
while (!cola.isEmpty()) {          // compruebo que queda 1 cliente...
    String c = cola.poll();        // ...pero otro hilo se lo ha llevado → c vale null ❌
}

String c;
while ((c = cola.poll()) != null) { ... }   // ✅ una sola operación atómica
```
