# Tema 2: Concurrencia, contado como una historia

> **Cómo usar este archivo:**
> - Léelo **de un tirón**, como un cuento. Aquí no hay sintaxis: solo **qué problema hay** y **cómo se arregla**.
> - Cada bloque tiene una **💬 frase clave**. Si te la sabes, sabes el bloque.
> - Ten abierto `visual.pdf` al lado: cada bloque te dice qué página mirar.
> - La sintaxis está en `2_TECNICO.md`, y para comprobarte tienes `3_PREGUNTAS.md`.

**La historia entera en tres líneas:**
- En el tema 1, los camareros de la cafetería solo tenían que **no pisarse** (`synchronized`).
- En este tema, además, tienen que **coordinarse**: esperarse, turnarse, avisarse, devolverse resultados y no quedarse bloqueados.
- Cada bloque es un problema nuevo de la cafetería y la herramienta que lo resuelve.

---

## 1. El cocinero y los camareros: esperar a que haya algo
📄 *PDF, página 2* · 🧪 *Demo `C01_ProductorConsumidor`*

> 💬 **"Si no hay nada que hacer, me duermo y suelto la barra; cuando cambie algo, me avisan."**

**El problema:** el cocinero deja platos en una **barra de 3 huecos** y los camareros los recogen.
- ¿Qué hace un camarero si la barra está **vacía**?
- ¿Y el cocinero si está **llena**?

Si se quedan mirando la barra todo el rato ("¿hay plato?, ¿hay plato?, ¿hay plato?…"), gastan CPU para nada.

**La solución:**
1. El que no puede hacer nada **se duerme** (`wait`). Al dormirse, **suelta la barra** para que los demás puedan usarla.
2. Cuando alguien cambia algo, por ejemplo deja un plato, **avisa a todos los que duermen** (`notifyAll`).
3. Los que se despiertan **vuelven a mirar** la barra antes de actuar, porque puede que otro se les haya adelantado. Por eso se comprueba con un bucle (**`while`**) y no con un `if`.

**En la demo:**
- *"[barra vacía] Camarero-2 espera..."*: no hay plato y se duerme.
- *"[barra llena] el cocinero espera..."*: hay 3 platos y el cocinero se duerme.

---

## 2. El pool de hilos: una plantilla fija de camareros
📄 *PDF, página 3* · 🧪 *Demo `C02_PoolDeHilos`, parte 1*

> 💬 **"No contrato un camarero por cliente: tengo una plantilla fija que se va turnando el trabajo."**

**El problema:** si por cada cliente creas un hilo nuevo (`new Thread`), con 1000 clientes tienes 1000 hilos. Crear hilos es **caro**, y demasiados a la vez saturan el ordenador.

**La solución: un pool.** Es como **contratar 3 camareros fijos**:
- Tú no les dices "tú atiendes a este". Solo **dejas las tareas en el mostrador** (`submit`), y el primer camarero libre coge la siguiente.
- Cuando un camarero termina, **no se va a casa**: coge otra tarea.
- Al final del día **cierras la cafetería** (`shutdown`). Si no, los camareros se quedan esperando más clientes **para siempre**, y el programa no termina.

**En la demo:** 6 clientes y 3 camareros. Solo aparecen `pool-1-thread-1`, `-2` y `-3`, **repetidos**: son siempre los mismos 3, reutilizados.

> 🔑 **Idea clave que te costó en `WriteCSV`:** la tarea (`WriteCSV`) **no es** el camarero. La tarea es **el encargo**; los camareros son los hilos del pool. Puedes tener 5 encargos y 3 camareros.

---

## 3. `Callable` y `Future`: el ticket del pedido
📄 *PDF, páginas 4 y 5* · 🧪 *Demo `C02_PoolDeHilos`, parte 2* · 📝 *Tu ejercicio `WriteCSV`*

> 💬 **"Pido, me dan un ticket, sigo a lo mío, y con el ticket recojo el resultado cuando lo necesito."**

**El problema:** hasta ahora las tareas (`Runnable`) **hacían cosas** pero **no devolvían nada**. ¿Y si quiero que la tarea me dé un resultado, por ejemplo el fichero que ha creado o lo que cuesta el pedido?

**La solución tiene dos piezas:**
- **`Callable`**: una tarea que **sí devuelve algo** al terminar (el `return` de su método `call()`).
- **`Future`**: el **ticket** que te dan al hacer el pedido. Es como la tintorería: dejas la ropa, te dan un resguardo y vuelves con él a recogerla.

**El viaje de un pedido, paso a paso:**
1. **Pides:** `submit(tarea)`. Te dan un **ticket** (`Future`) **al momento**, aunque el pedido aún no esté hecho. Lo guardas, por ejemplo en una lista.
2. **Un camarero lo prepara él solo:** el hilo del pool ejecuta `call()`. **Tú no llamas a `call()` nunca**, igual que en el tema 1 no llamabas a `run()`.
3. **El resultado se guarda dentro del ticket:** lo que pone el `return` de `call()`.
4. **Recoges:** `get()` con el ticket te da el resultado. Si aún no está listo, **esperas en el mostrador**.
5. **Cierras:** `shutdown()`.

**Lo que más se olvida:**
- **Los errores también se quedan dentro del ticket.** Si el camarero "quema el pedido" (salta una excepción en `call()`), no viene a decírtelo: **te enteras al hacer `get()`**. Por eso en `WriteCSV` no veías el error de `LocalDate` hasta que pusiste el `get()`.
- **Primero pides todo y luego recoges todo.** Si pides un café y te quedas esperando en el mostrador antes de pedir el siguiente, solo trabaja un camarero cada vez. Hay que hacer **un bucle para pedir** (guardando los tickets) y **otro para recoger**.

**En la demo:** 4 pedidos (tostada, café, zumo y bocadillo) preparados por 2 camareros. Al final se recogen los 4 tickets y se suma la cuenta: *"Total de la cuenta: 11,00 €"*.

---

## 4. Contadores seguros y cerrojos con más control
📄 *PDF, página 6* · 🧪 *Demo `C03_AtomicosYLocks`*

> 💬 **"Para contar, un contador atómico. Para un cerrojo con paciencia limitada, `ReentrantLock`."**

**Problema 1: el contador que pierde cuentas.** 4 camareros apuntan cada café en una libreta compartida. "Sumar 1" en realidad son **tres pasos**:
1. leer el número;
2. sumarle 1;
3. escribirlo.

Si dos camareros leen a la vez el 5, los dos escriben 6. **Se ha perdido un café.**

**Solución: un contador atómico** (`AtomicInteger`). Hace los tres pasos **de un golpe**, sin que nadie se meta en medio.
> En la demo: se esperaban 400 000 y el `int` normal se queda en **307 769**; el atómico llega a **400 000** clavado.

**Problema 2: el cerrojo sin paciencia.** Con `synchronized`, si la cafetera está ocupada **esperas lo que haga falta**, aunque sea para siempre. ¿Y si prefieres decir *"si en 100 ms no está libre, hago otra cosa"*?

**Solución: `ReentrantLock`.** Es un cerrojo "manual":
- lo coges y lo sueltas tú;
- tiene `tryLock` (*"inténtalo, pero no te quedes atascado"*).

**Lo que nunca se olvida:** como lo sueltas tú, **hay que soltarlo siempre**, aunque haya un error. Por eso va en un `finally`.
> En la demo: *"Camarero-2: ocupada tras 100 ms, hago otra cosa en vez de bloquearme"*.

---

## 5. Interbloqueo: dos camareros esperándose para siempre
📄 *PDF, página 7* · 🧪 *Demo `C04_Interbloqueo`*

> 💬 **"Si todos cogen las cosas en el mismo orden, nadie se queda esperando al otro para siempre."**

**El problema:** para hacer un café hacen falta **la cafetera y el molinillo**.
- El camarero A coge la cafetera y espera el molinillo.
- El camarero B coge el molinillo y espera la cafetera.

**Ninguno suelta lo que tiene y los dos esperan para siempre.** El programa se congela **sin ningún error**: eso es un **interbloqueo** (*deadlock*).

**Por qué pasa:** se tienen que dar **4 condiciones a la vez**:
1. Cada cosa la usa uno solo.
2. Tengo una cosa y espero otra sin soltar la primera.
3. Nadie me la puede quitar.
4. Se esperan en círculo: A a B y B a A.

**La solución:** basta con **romper una** de las cuatro. La más fácil es la 4: **todos cogen primero la cafetera y después el molinillo**. Así no puede formarse el círculo.
> En la demo: con orden contrario sale *"⚠ INTERBLOQUEO… Estado A=BLOCKED, B=BLOCKED"*; con el mismo orden, *"✔ Los dos han preparado su café"*.

---

## 6. Semáforo y cuenta atrás: limitar y esperar a todos
📄 *PDF, página 8* · 🧪 *Demo `C05_SemaforoYCountDownLatch`*

> 💬 **"`Semaphore` = solo caben N. `CountDownLatch` = no empiezo hasta que lleguen todos."**

**`Semaphore`: la cafetería tiene 2 mesas.**
- Entran 5 clientes, pero **solo se pueden sentar 2 a la vez**.
- Al entrar coges una mesa (`acquire`). Si no quedan, esperas.
- Al irte la dejas libre (`release`), y la coge el siguiente.
> En la demo: *"mesas libres: 1… mesas libres: 0"*, y nunca hay más de 2 clientes sentados.

**`CountDownLatch`: no se abre hasta que lleguen los 3 camareros.**
- Es una **cuenta atrás**: empieza en 3, y cada camarero que llega resta 1 (`countDown`).
- El encargado espera (`await`) hasta que llega a **0**, y entonces abre.
- Es de **un solo uso**: cuando llega a 0, no se puede reiniciar.
> En la demo: *"Camarero 1 ha llegado… 2… 3 → ✔ Están todos: ¡abrimos la cafetería!"*

**¿En qué se diferencia de `join()`?** `join()` espera a que un hilo **termine**. La cuenta atrás espera a que pase **algo** (que lleguen), aunque los hilos sigan trabajando después.

---

## 7. Resumen: qué herramienta para qué problema
📄 *PDF, página 9*

| Mi problema es… | Herramienta | Frase clave |
|---|---|---|
| Esperar a que haya algo (o sitio) | `wait` / `notifyAll` | Me duermo, suelto y me avisan |
| Muchas tareas y no quiero crear mil hilos | Pool (`ExecutorService`) | Plantilla fija que se turna |
| Que la tarea me **devuelva** algo | `Callable` + `Future` | Ticket del pedido |
| Un contador compartido | `AtomicInteger` | Sumar de un golpe |
| Un cerrojo con `tryLock` o tiempo máximo | `ReentrantLock` | Cerrojo con paciencia limitada |
| Que no se bloqueen para siempre | Mismo orden de recursos | Todos en el mismo orden |
| Solo N a la vez | `Semaphore` | Solo caben N |
| Esperar a que lleguen todos | `CountDownLatch` | Cuenta atrás |

**¿Y ahora?**
1. Repasa el `visual.pdf` entero.
2. Haz `3_PREGUNTAS.md` tapando las respuestas.
3. Cuando necesites escribir código, abre `2_TECNICO.md`.
