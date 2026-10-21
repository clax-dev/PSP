# Tema 1: Hilos, preguntas para comprobarte

> **Cómo usar este archivo:**
> - Lee la pregunta, **contesta en voz alta** (o en un papel) y solo después abre la respuesta.
> - Marca con ✅ las que te salen y repite las demás otro día.
> - Las de ⭐ son las que más se preguntan, o en las que más te has equivocado tú.

## Progreso
- [ ] Bloque 1: proceso, hilo y concurrencia (1-3)
- [ ] Bloque 2: crear y arrancar (4-7)
- [ ] Bloque 3: join, sleep e interrupt (8-11)
- [ ] Bloque 4: condición de carrera y synchronized (12-17)
- [ ] Bloque 5: colecciones y "comprobar y luego actuar" (18-20)

---

## Bloque 1: Proceso, hilo y concurrencia

### 1. ⭐ ¿Qué diferencia hay entre un proceso y un hilo?
<details><summary>Respuesta</summary>

Un **proceso** es un programa en marcha, con **su propia memoria**. Un **hilo** es una línea de ejecución **dentro** de un proceso, y los hilos de un mismo proceso **comparten memoria**.

Analogía: el proceso es la cafetería y los hilos son los camareros, que comparten la caja y la barra.
</details>

### 2. ⭐ Ejecuto dos veces el mismo programa con hilos y sale en distinto orden. ¿Está mal?
<details><summary>Respuesta</summary>

No. **El orden de ejecución no está garantizado**: lo decide el sistema operativo. Un programa con hilos tiene que funcionar bien **en cualquier orden**.
</details>

### 3. ¿Con cuántos hilos empieza todo programa Java?
<details><summary>Respuesta</summary>

Con **uno**, el hilo **`main`**.
</details>

---

## Bloque 2: Crear y arrancar

### 4. ⭐ ¿Qué diferencia hay entre `t.start()` y `t.run()`?
<details><summary>Respuesta</summary>

- `start()` **crea un hilo nuevo**, que ejecuta `run()` mientras el `main` sigue a lo suyo.
- `run()` es una llamada normal a un método: lo ejecuta **el hilo actual**, sin concurrencia.

En `H01`: con `run()` sale *"Me ejecuta: main"*.
</details>

### 5. ¿Qué pasa si llamo a `start()` dos veces sobre el mismo hilo?
<details><summary>Respuesta</summary>

`IllegalThreadStateException`. Un hilo solo se puede arrancar **una vez**. Para repetir el trabajo, se crea un `Thread` nuevo.
</details>

### 6. ¿Por qué es mejor `implements Runnable` que `extends Thread`?
<details><summary>Respuesta</summary>

Porque en Java solo se puede **heredar de una clase**. Si heredas de `Thread`, ya no puedes heredar de otra. Además, separa **el encargo** (`Runnable`) de **quién lo hace** (`Thread`), que es lo que luego usa el pool del tema 2.
</details>

### 7. ¿En qué estado está un hilo justo después de `new Thread(...)`? ¿Y cuando acaba `run()`?
<details><summary>Respuesta</summary>

`NEW` y `TERMINATED`.
</details>

---

## Bloque 3: `join`, `sleep` e `interrupt`

### 8. ⭐ ¿Para qué sirve `join()`?
<details><summary>Respuesta</summary>

Para que el hilo que lo llama **espere a que otro termine**. Sin `join`, el `main` mostraría los resultados **antes** de que los hilos acaben, y saldrían mal.
</details>

### 9. ⭐ Tengo 3 hilos. ¿Por qué no hago `start()` y `join()` dentro del mismo bucle?
<details><summary>Respuesta</summary>

Porque `join()` espera a que ese hilo acabe **antes** de arrancar el siguiente, así que van **de uno en uno** y se pierde la concurrencia. Primero se arrancan todos y después se esperan todos.
</details>

### 10. ¿`interrupt()` mata al hilo?
<details><summary>Respuesta</summary>

**No.** Solo le **pide** que pare: le deja una "nota". El hilo tiene que comprobarlo con `isInterrupted()`, o capturar `InterruptedException` si estaba dormido, y salir él solo.
</details>

### 11. ¿Por qué se pone `Thread.currentThread().interrupt()` dentro del `catch (InterruptedException e)`?
<details><summary>Respuesta</summary>

Porque al saltar la excepción la "nota" de interrupción **se borra**. Volviéndola a poner, el resto del código todavía puede enterarse de que le pidieron parar.
</details>

---

## Bloque 4: Condición de carrera y `synchronized`

### 12. ⭐ ¿Qué es una condición de carrera?
<details><summary>Respuesta</summary>

Cuando **el resultado depende del orden** en que se ejecutan los hilos, porque varios tocan el mismo dato a la vez. Por ejemplo, dos camareros suman a la caja a la vez y se pierde una venta.
</details>

### 13. ⭐ ¿Por qué `contador++` no es seguro con varios hilos?
<details><summary>Respuesta</summary>

Porque son **tres pasos** (leer, sumar y escribir). Si dos hilos leen el mismo valor a la vez, escriben el mismo resultado y se pierde un incremento. En `H03`, de 400 000 salieron 125 564.
</details>

### 14. ¿Qué es una sección crítica?
<details><summary>Respuesta</summary>

El trozo de código que toca un recurso compartido y que **solo debe ejecutar un hilo a la vez**.
</details>

### 15. ⭐ ¿Por qué el candado de `synchronized` se declara `static final`?
<details><summary>Respuesta</summary>

Para que sea **el mismo objeto para todos los hilos**. Si cada hilo tuviera su propio candado, cada uno abriría el suyo y **no habría exclusión**.
</details>

### 16. Pongo `public synchronized void cobrar()` en la clase `Caja`, pero cada camarero crea su propia `new Caja()`. ¿Está protegido?
<details><summary>Respuesta</summary>

**No.** Un método `synchronized` usa como candado **ese objeto** (`this`). Si cada camarero tiene su propia caja, hay un candado por camarero y no se protegen entre sí. Hay que **compartir la misma caja**, pasándola por el constructor.
</details>

### 17. ¿Por qué no meto todo el `run()` dentro del `synchronized` y así me olvido de problemas?
<details><summary>Respuesta</summary>

Porque dentro del candado los hilos van **de uno en uno**. Si metes todo, es como tener un solo camarero: pierdes la concurrencia. Solo se sincroniza **lo imprescindible**.
</details>

---

## Bloque 5: Colecciones y "comprobar y luego actuar"

### 18. ¿Qué colección uso para una cola de clientes compartida por varios hilos?
<details><summary>Respuesta</summary>

`ConcurrentLinkedQueue`.
</details>

### 19. ⭐ ¿Qué falla en `while (!cola.isEmpty()) { String c = cola.poll(); ... }` con varios hilos?
<details><summary>Respuesta</summary>

Es **"comprobar y luego actuar"**: entre `isEmpty()` y `poll()` hay un hueco. Otro hilo puede llevarse el último cliente, y `poll()` devuelve **`null`**. Aunque cada operación de la cola es segura, **dos seguidas no lo son**.

Se arregla con una sola operación: `while ((c = cola.poll()) != null) { ... }`. Te pasó en `Cafeteria`.
</details>

### 20. Después de hacer `join()` a todos los hilos, ¿hace falta `System.exit(0)`?
<details><summary>Respuesta</summary>

No. Todos han terminado, y el programa acaba solo cuando termina el `main`.
</details>
