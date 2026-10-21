# Tema 1: Hilos, contado como una historia

> **Cómo usar este archivo:**
> - Léelo **de un tirón**, como un cuento. Aquí no hay sintaxis: solo **qué es cada cosa** y **qué problema resuelve**.
> - Cada bloque tiene una **💬 frase clave**. Si te la sabes, sabes el bloque.
> - Ten abierto `visual.pdf` al lado: cada bloque te dice qué página mirar.
> - La sintaxis está en `2_TECNICO.md`, y para comprobarte tienes `3_PREGUNTAS.md`.
> - **Ejercicio del tema:** `ejercicios/src/tema1_hilos/Cafeteria.java`.

**La historia entera en tres líneas:**
- Hasta ahora tus programas eran **un solo camarero** haciendo todo, una cosa detrás de otra.
- En este tema contratamos **varios camareros que trabajan a la vez** en la misma cafetería.
- Eso es más rápido, pero aparece un problema nuevo: **se pisan** cuando tocan lo mismo.

---

## 1. Proceso e hilo: la cafetería y sus camareros
📄 *PDF, página 2*

> 💬 **"El proceso es la cafetería; los hilos son los camareros que trabajan dentro y comparten todo lo que hay en ella."**

- **Proceso** = un programa en marcha. Es **la cafetería**: tiene su propio local, su caja y su barra (su **memoria**). Una cafetería no puede tocar la caja de otra.
- **Hilo** = una línea de trabajo dentro del proceso. Es **un camarero**. Todos los camareros de la misma cafetería **comparten** la caja, la barra y la libreta: comparten la memoria.
- Todo programa empieza con **un camarero**, el **`main`**. Luego puede contratar más.
- **Concurrencia** = varios camareros avanzan "a la vez".

**El detalle más importante del tema: el orden no está garantizado.** Quién va primero lo decide el **sistema operativo**, no tú. Si ejecutas dos veces el mismo programa, puede salir en otro orden.
> En la demo `H01`, *"Camarero 1 atiende mesa 1"* y *"Camarero 2 atiende mesa 1"* salen cada vez en un orden distinto.

---

## 2. Contratar un camarero: el encargo y el trabajador
📄 *PDF, página 3* · 🧪 *Demo `H01_CrearHilos`*

> 💬 **"`Runnable` es el encargo; `Thread` es el camarero que lo hace; `start()` es decirle «¡empieza!»."**

Para tener un camarero trabajando hacen falta dos cosas:
- **El encargo** (`Runnable`): *qué* hay que hacer, escrito en su método `run()`. Por ejemplo, "atiende las mesas 1, 2 y 3".
- **El trabajador** (`Thread`): *quién* lo hace. Le das el encargo al crearlo.

Y luego le dices **"¡empieza!"** con `start()`.

**La trampa más famosa: `start()` frente a `run()`.**
- `start()` → el camarero **nuevo** empieza a trabajar, y tú (el `main`) sigues a lo tuyo. ✅ Son dos trabajando.
- `run()` → **lo haces tú mismo**, el `main`, y hasta que no acabas no sigues. ❌ No hay ningún camarero nuevo: es como leer el encargo en voz alta en lugar de dárselo a alguien.
> En la demo: con `run()` sale *"Me ejecuta: main"*; con `start()`, *"Me ejecuta: HiloNuevo"*.

**Otras dos cosas que conviene saber:**
- A un camarero solo se le puede decir **"¡empieza!" una vez**. Si lo repites, error (`IllegalThreadStateException`).
- Hay otras formas de crear hilos, como heredar de `Thread` o usar una lambda, pero la recomendada es **el encargo aparte** (`implements Runnable`), como tu clase `Camarero`.

---

## 3. La vida de un camarero
📄 *PDF, página 4*

> 💬 **"Contratado → trabajando ↔ esperando → se va a casa. Y una vez en casa, no vuelve."**

| Estado | En la cafetería |
|---|---|
| `NEW` | Contratado, pero aún no le han dicho "¡empieza!" |
| `RUNNABLE` | Trabajando, o listo para trabajar en cuanto le toque |
| `BLOCKED` / `WAITING` / `TIMED_WAITING` | Esperando: a que se libere algo, a que otro termine o durmiendo un rato |
| `TERMINATED` | Ha terminado su encargo y se ha ido |

> En la demo `H01`: *"Estado de t1 antes de start(): NEW"* y *"al terminar: TERMINATED"*.

---

## 4. Esperar, dormir y pedir que pare
📄 *PDF, página 5* · 🧪 *Demo `H02_JoinSleepInterrupt`*

> 💬 **"`join` = espero a que acabes. `sleep` = me echo una siesta. `interrupt` = te pido por favor que pares."**

**`join()`: esperar a que alguien termine.**
El encargado (`main`) quiere hacer la caja al final del día, pero **no puede** hasta que los camareros terminen. Si no espera, cuenta el dinero **antes** de que acaben, y la cuenta sale mal.
> En la demo: sin `join`, *"main: no he esperado, el hilo sigue vivo? true"*. Con `join`, *"main: todos los camareros han terminado ✔"*.

**`sleep()`: echarse una siesta.** El camarero que ejecuta esa línea se duerme unos milisegundos. Sirve para simular que algo tarda.

**`interrupt()`: pedir que pare.** **No lo echa a la fuerza.** Es como dejarle **una nota**: "por favor, termina". El camarero tiene que **mirar si tiene notas** (`isInterrupted()`) y salir limpiamente.
> En la demo: *"me han interrumpido, salgo limpiamente"*.

**El esquema de toda la vida** (tu `abrirCafeteria`):
1. preparar lo compartido;
2. crear los camareros;
3. `start()` a todos;
4. `join()` a todos;
5. mostrar el resultado.

---

## 5. Se pisan: la condición de carrera
📄 *PDF, página 6* · 🧪 *Demo `H03_CondicionCarreraSynchronized`*

> 💬 **"Si dos camareros tocan lo mismo a la vez, se pisan. `synchronized` es un candado: dentro, de uno en uno."**

**El problema:** 4 camareros apuntan cada venta en **la misma caja**. "Sumar 1" son **tres pasos**:
1. mirar cuánto hay;
2. sumarle 1;
3. apuntar el resultado.

Si dos miran a la vez y ven 5, los dos apuntan 6. **Se ha perdido una venta.** A esto se le llama **condición de carrera**: el resultado depende de quién llega antes.
> En la demo: se esperaban 400 000 y **sin protección salen 125 564**.

**Las palabras que lo describen:**
- **Recurso compartido** = lo que tocan varios: la caja, la barra, la consola…
- **Sección crítica** = el trozo de código que toca ese recurso y que solo debe hacer **uno a la vez**.

**La solución: un candado** (`synchronized`). Antes de tocar la caja hay que coger el candado. Si lo tiene otro, **esperas**. Al salir, se suelta **solo**.
> En la demo: **con `synchronized` salen 400 000** clavados.

**Las dos reglas del candado:**
1. **Tiene que ser EL MISMO para todos.** Si cada camarero tiene su propio candado, no protege nada, porque cada uno abre el suyo. Por eso se declara `static final`.
2. **Solo lo imprescindible dentro.** Dentro del candado van de uno en uno; si metes demasiado, pierdes la ventaja de tener varios camareros.

**En tu `Cafeteria`:** el candado sirve para que las tres frases del diálogo de cada camarero (*"¿Qué desea?" → "Un café" → "Aquí tiene"*) salgan **juntas**, sin mezclarse con las de otro.

---

## 6. Colecciones seguras y la trampa de "comprobar y luego actuar"
📄 *PDF, página 7* · 🧪 *Demo `H04_ColeccionesConcurrentes`*

> 💬 **"Una colección segura protege cada operación, pero no dos seguidas. Pregunta y coge en un solo paso."**

**Problema 1: las listas normales se rompen.** Si varios camareros añaden a una `ArrayList` a la vez, se pierden elementos.
> En la demo: se esperaban 2000 y la `ArrayList` se queda en **1984**.

Java tiene versiones **seguras para hilos**, por ejemplo `ConcurrentLinkedQueue` para la cola de clientes.

**Problema 2: la trampa en la que caíste en `Cafeteria`.** Aunque la cola sea segura, **dos operaciones seguidas no lo son**:
1. El camarero 1 pregunta "¿queda alguien en la cola?" (`isEmpty`) → *sí, queda 1*.
2. El camarero 2 pregunta lo mismo → *sí, queda 1*.
3. El camarero 1 coge al cliente (`poll`) → lo tiene.
4. El camarero 2 coge al cliente → **¡ya no hay nadie!** Le sale `null`.

Entre **comprobar** y **actuar** hay un hueco, y otro se cuela. **La solución:** no preguntes; **intenta coger directamente**. `poll()` ya te dice si había alguien: si devuelve `null`, la cola estaba vacía. Es **un solo paso**, sin hueco.

> Esta misma trampa vuelve en el tema 3: entre soltar el `readLock` y coger el `writeLock` hay un hueco, y por eso hay que volver a comprobar.

---

## 7. Resumen

| Concepto | Frase clave |
|---|---|
| Proceso / hilo | La cafetería y sus camareros, que comparten todo |
| Orden | Lo decide el sistema operativo: **no está garantizado** |
| `Runnable` / `Thread` | El encargo y el camarero |
| `start()` frente a `run()` | `start` = otro trabaja; `run` = lo haces tú |
| `join()` | Espero a que acabes |
| `sleep()` | Siesta |
| `interrupt()` | Una nota pidiendo que pare (no lo echa) |
| Condición de carrera | Dos tocan lo mismo y se pisan |
| `synchronized` | Candado, **el mismo para todos**, y solo lo imprescindible |
| Colección concurrente | Segura en cada operación, pero no en dos seguidas |
| Comprobar y luego actuar | Pregunta y coge en un solo paso: `poll()` |

**¿Y ahora?**
1. Repasa el `visual.pdf` entero.
2. Haz `3_PREGUNTAS.md` tapando las respuestas.
3. Cuando necesites escribir código, abre `2_TECNICO.md`.
