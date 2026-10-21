# Tema 2: Concurrencia, preguntas para comprobarte

> **Cómo usar este archivo:**
> - Lee la pregunta, **contesta en voz alta** (o en un papel) y solo después abre la respuesta.
> - Marca con ✅ las que te salen y repite las demás otro día.
> - Las de ⭐ son las que más se preguntan, o en las que más te has equivocado tú.

## Progreso
- [ ] Bloque 1: wait / notifyAll (1-3)
- [ ] Bloque 2: pool de hilos (4-6)
- [ ] Bloque 3: Callable y Future (7-14)
- [ ] Bloque 4: atómicos y ReentrantLock (15-17)
- [ ] Bloque 5: interbloqueo (18-19)
- [ ] Bloque 6: Semaphore y CountDownLatch (20-22)
- [ ] Bloque 7: ¿qué herramienta uso? (23-25)

---

## Bloque 1: `wait` / `notifyAll`

### 1. ¿Qué hace un hilo cuando llama a `wait()`?
<details><summary>Respuesta</summary>

Se duerme **y suelta el cerrojo**, para que otros hilos puedan entrar. Sigue dormido hasta que alguien llama a `notifyAll()` (o `notify()`) sobre el mismo objeto.
</details>

### 2. ⭐ ¿Por qué se comprueba la condición con `while` y no con `if`?
<details><summary>Respuesta</summary>

Porque al despertar **otro hilo puede haberse adelantado**. Con `while`, el hilo vuelve a mirar si la condición sigue cumpliéndose; con `if`, actuaría aunque ya no se cumpla.

Por ejemplo, en la cafetería: dos camareros despiertan porque hay **un** plato. El primero lo coge; el segundo, si usara `if`, intentaría coger un plato que ya no existe.
</details>

### 3. ¿Qué pasa si llamas a `wait()` fuera de un bloque `synchronized`?
<details><summary>Respuesta</summary>

Salta `IllegalMonitorStateException`. `wait` y `notify` tienen que estar dentro de un `synchronized` **sobre el mismo objeto**.
</details>

---

## Bloque 2: Pool de hilos

### 4. ¿Por qué usar un pool en lugar de `new Thread` para cada tarea?
<details><summary>Respuesta</summary>

Porque crear hilos es caro y demasiados a la vez saturan el ordenador. Un pool tiene una **plantilla fija** de hilos que se **reutilizan**: cuando uno acaba una tarea, coge la siguiente.
</details>

### 5. ⭐ Tengo un pool de 3 hilos y le mando 5 tareas. ¿Cuántos nombres de hilo distintos veré?
<details><summary>Respuesta</summary>

**3** (`pool-1-thread-1`, `-2` y `-3`), con **algunos repetidos**. Las tareas son 5, pero los trabajadores siguen siendo 3.
</details>

### 6. ⭐ ¿Qué pasa si se te olvida el `shutdown()`?
<details><summary>Respuesta</summary>

**El programa no termina.** Los hilos del pool se quedan vivos esperando más tareas, y tienes que pararlo a mano (el cuadrado rojo de IntelliJ).
</details>

---

## Bloque 3: `Callable` y `Future`

### 7. ¿Qué diferencia hay entre `Runnable` y `Callable`?
<details><summary>Respuesta</summary>

| | `Runnable` | `Callable<T>` |
|---|---|---|
| Método | `run()` | `call()` |
| ¿Devuelve algo? | No | Sí, un `T` |
| ¿Excepciones comprobadas? | No | Sí |
</details>

### 8. ⭐ ¿Quién llama al método `call()`?
<details><summary>Respuesta</summary>

**El hilo del pool**, cuando le toca la tarea. Tú **nunca** lo llamas: solo haces `submit(tarea)`.

Si lo llamas tú (`new WriteCSV(1).call()`), se ejecuta en el hilo del `main`, sin pool ni concurrencia. Es el mismo error que llamar a `run()` en vez de `start()`.
</details>

### 9. ⭐ ¿Qué es un `Future`? Explícalo con una analogía.
<details><summary>Respuesta</summary>

Es el **ticket** (o resguardo) que te dan al hacer un pedido. Te lo dan **al momento**, aunque el pedido aún no esté listo. Más tarde, con `get()`, canjeas el ticket y recoges el resultado. Si aún no está listo, esperas.
</details>

### 10. ⭐ Si mi clase es `Callable<Path>`, ¿qué me devuelve `submit` y qué me devuelve `get()`?
<details><summary>Respuesta</summary>

- `submit` devuelve un `Future<Path>`: un ticket de un `Path`.
- `get()` devuelve un `Path`: **exactamente lo que pusiste en el `return` de `call()`**.
</details>

### 11. ⭐ ¿Por qué el `get()` va en un bucle aparte y no en el mismo bucle del `submit`?
<details><summary>Respuesta</summary>

Porque `get()` **espera** a que la tarea termine. Si lo pones en el bucle del `submit`, esperas a que acabe cada tarea **antes de repartir la siguiente**, y solo trabaja un hilo cada vez: pierdes la concurrencia.

Lo correcto es repartir todas las tareas (guardando los tickets en una lista) y **después** recogerlas.
</details>

### 12. ⭐ Mi `call()` lanza una excepción, pero en la consola no veo ningún error. ¿Por qué?
<details><summary>Respuesta</summary>

Porque el error **se queda guardado dentro del `Future`**. Solo lo ves cuando haces `get()`, y te llega envuelto en una **`ExecutionException`**. La causa real está en `e.getCause()`, o en el `Caused by:` de la consola.

Te pasó en `WriteCSV`: el error de `LocalDate` no aparecía hasta que pusiste el `get()`.
</details>

### 13. Mi tarea tiene que devolver un `File` y un `Path`. ¿Cómo lo hago, si `call()` solo devuelve una cosa?
<details><summary>Respuesta</summary>

Hay dos formas:
- Devolver solo el `Path` y sacar el `File` con `ruta.toFile()`.
- Meter los dos en una **caja**, un `record`: `record Resultado(File fichero, Path ruta) {}`, y usar `Callable<Resultado>`.
</details>

### 14. ⭐ En `WriteCSV`, ¿por qué hay que poner el número de tarea en el nombre del fichero?
<details><summary>Respuesta</summary>

Porque con 3 hilos a la vez, **varias tareas se ejecutan en el mismo segundo**, y el nombre `pacientes_yyyyMMdd_HHmmss.csv` sería idéntico. `Files.write` sobrescribiría el fichero, y en vez de 5 ficheros tendrías 1 o 2.

En tu ejecución, las 5 tareas cayeron en el mismo segundo.
</details>

---

## Bloque 4: Atómicos y `ReentrantLock`

### 15. ⭐ ¿Por qué `contador++` con varios hilos pierde cuentas?
<details><summary>Respuesta</summary>

Porque `++` son **tres pasos**: leer, sumar y escribir. Si dos hilos leen el mismo valor a la vez, los dos escriben el mismo resultado y **se pierde una suma**.

En `C03`, de 400 000 solo llegaron 307 769. `AtomicInteger.incrementAndGet()` hace los tres pasos de un golpe.
</details>

### 16. ¿Qué ofrece `ReentrantLock` que no tenga `synchronized`?
<details><summary>Respuesta</summary>

- `tryLock()`: intentarlo sin quedarse bloqueado.
- `tryLock(tiempo)`: esperar como máximo un tiempo.
- Modo **justo** (`new ReentrantLock(true)`).
- Varias `Condition`.

A cambio, **lo tienes que soltar tú**.
</details>

### 17. ⭐ ¿Por qué el `unlock()` va siempre en un `finally`?
<details><summary>Respuesta</summary>

Porque si salta una excepción antes del `unlock()`, el lock **se queda cogido para siempre** y los demás hilos esperan eternamente. El `finally` se ejecuta siempre, haya error o no.

`synchronized` no tiene este problema, porque se libera solo.
</details>

---

## Bloque 5: Interbloqueo

### 18. ⭐ ¿Qué es un interbloqueo? Pon un ejemplo.
<details><summary>Respuesta</summary>

Dos (o más) hilos que **se esperan mutuamente para siempre**. El camarero A tiene la cafetera y espera el molinillo; el B tiene el molinillo y espera la cafetera. El programa se congela **sin ningún error**.
</details>

### 19. ¿Cuál es la forma más sencilla de evitarlo?
<details><summary>Respuesta</summary>

Que **todos los hilos cojan los recursos en el mismo orden**: primero la cafetera y después el molinillo. Así no puede darse la espera circular, que es una de las 4 condiciones de Coffman.
</details>

---

## Bloque 6: `Semaphore` y `CountDownLatch`

### 20. ¿Para qué sirve un `Semaphore`?
<details><summary>Respuesta</summary>

Para que **como mucho N hilos** usen algo a la vez. Por ejemplo, 2 mesas para 5 clientes. Se coge un permiso con `acquire()` y se devuelve con `release()` (en un `finally`).
</details>

### 21. ¿Para qué sirve un `CountDownLatch`?
<details><summary>Respuesta</summary>

Para que uno o varios hilos **esperen a que ocurra algo N veces**. Por ejemplo, abrir la cafetería cuando lleguen los 3 camareros. Cada uno hace `countDown()` y el que espera hace `await()`.
</details>

### 22. ¿En qué se diferencia `CountDownLatch` de `join()`?
<details><summary>Respuesta</summary>

`join()` espera a que un hilo **termine**. `CountDownLatch` espera a que pase un **evento** N veces, aunque los hilos sigan trabajando después. Además, `CountDownLatch` es de **un solo uso**.
</details>

---

## Bloque 7: ¿Qué herramienta uso?

### 23. Tengo 100 tareas que calculan un precio y quiero sumarlos todos al final.
<details><summary>Respuesta</summary>

Un **pool** + **`Callable<Double>`**. Guardo los `Future` en una lista y luego los recorro con `get()` sumando los resultados. Es la parte 2 de `C02`.
</details>

### 24. Varios hilos cuentan cuántos clientes han entrado.
<details><summary>Respuesta</summary>

**`AtomicInteger`** con `incrementAndGet()`.
</details>

### 25. Una impresora compartida: si está ocupada, prefiero no esperar y avisar al usuario.
<details><summary>Respuesta</summary>

**`ReentrantLock`** con `tryLock()`: si devuelve `false`, aviso en lugar de bloquearme.
</details>
