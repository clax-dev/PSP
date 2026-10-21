# Tema 3: Locks de lectura y escritura, preguntas para comprobarte

> **Cómo usar este archivo:**
> - Lee la pregunta, **contesta en voz alta** y solo después abre la respuesta.
> - Cada respuesta tiene una parte **corta** (la primera frase que dirías) y una **ampliación**.
> - Las 10 primeras son las del enunciado de la exposición. Es el mismo contenido que `exposiciones/01_locks_lectura_escritura/PREGUNTAS.md`.

## Progreso
- [ ] Las 10 del enunciado (1-10)
- [ ] Extra (11-20)

---

## Las 10 del enunciado

### 1. ¿Por qué no utilizar siempre `synchronized`?
<details><summary>Respuesta</summary>

**Corta:** porque `synchronized` deja entrar a **uno cada vez**, también a los lectores, que no se molestan entre ellos. Si hay muchas lecturas lentas, se desperdicia mucho tiempo.

**Ampliación:**
- En mi demo, 4 lectores con `synchronized` tardan ~2400 ms y con `ReentrantReadWriteLock` ~560 ms, porque leen a la vez.
- Además, `synchronized` **no tiene** `tryLock`, tiempo máximo de espera ni fairness.
- **Pero** `synchronized` es más sencillo, se libera solo y, con secciones cortas, puede ser **más rápido**. Por eso no hay que usar siempre RW.
</details>

### 2. ¿Qué ocurre si un escritor intenta acceder mientras hay lectores?
<details><summary>Respuesta</summary>

**Corta:** el escritor **espera** (se bloquea en `writeLock().lock()`) hasta que sale **el último** lector.

**Ampliación:**
- Mientras espera, los lectores **nuevos** que lleguen normalmente **no se cuelan**: esperan detrás de él.
  - En el modo no justo es una medida aproximada (un lector espera si el primero de la cola es un escritor).
  - En el modo justo, porque se respeta el orden de llegada.
- Así se evita que el escritor espere para siempre.
- Se ve en L03, parte 2.
</details>

### 3. ¿Puede producirse starvation?
<details><summary>Respuesta</summary>

**Corta:** sí. Si llegan lectores sin parar, siempre hay alguno dentro y **el escritor nunca entra**. También puede pasarle a un lector si hay muchos escritores.

**Ampliación:**
- `ReentrantReadWriteLock` en modo **justo** (`new ReentrantReadWriteLock(true)`) evita la starvation, porque deja pasar por orden de llegada; a cambio, va más lento.
- En modo no justo, Java la **reduce**, pero no la garantiza.
- `StampedLock` **no tiene** modo justo.
</details>

### 4. ¿Qué ocurre si `validate()` devuelve `false`?
<details><summary>Respuesta</summary>

**Corta:** que alguien ha **escrito** desde que cogí el stamp. Los datos que he copiado **pueden estar a medias**, así que los **descarto** y vuelvo a leer, normalmente con `readLock()`.

**Ampliación:**
- También se puede volver a intentar la lectura optimista en un bucle. Pero si hay muchas escrituras, se pasa al `readLock`, para no estar repitiendo siempre.
- En L04, de 200 lecturas, 40 fallaron la validación, y 32 de ellas eran datos a medias.
</details>

### 5. ¿La lectura optimista bloquea a los escritores?
<details><summary>Respuesta</summary>

**Corta:** **no**. No coge ningún lock: solo apunta una versión. El escritor entra cuando quiere, y el lector se entera después, con `validate`.

**Ampliación:**
- Esa es justo su ventaja frente a `readLock()`, que sí impide escribir mientras se lee.
- En L04, parte 1, el escritor escribe en mitad de la lectura optimista sin esperar.
</details>

### 6. ¿Por qué hay que liberar siempre los locks?
<details><summary>Respuesta</summary>

**Corta:** porque, a diferencia de `synchronized`, un `Lock` **no se libera solo**. Si no haces `unlock()`, se queda cogido para siempre y los demás hilos esperan eternamente. El programa se congela **sin dar ningún error**.

**Ampliación:**
- Por eso se escribe `lock()` y después `try { … } finally { unlock(); }`.
- Si el `writeLock` se queda cogido, nadie lee ni escribe. Si se queda un `readLock`, nunca vuelve a entrar un escritor.
- En `StampedLock` hay que soltarlo **con su stamp**.
</details>

### 7. ¿Es `StampedLock` reentrante?
<details><summary>Respuesta</summary>

**Corta:** **no**. Si un hilo que ya tiene el `writeLock` vuelve a pedirlo, **se bloquea a sí mismo**.

**Ampliación:**
- En L04, parte 4, `tryWriteLock()` devuelve 0 (no conseguido).
- Por eso no hay que llamar, desde una sección protegida, a otro método que pida el mismo lock.
- Tampoco tiene `Condition` ni fairness.
</details>

### 8. ¿Qué diferencia hay entre una lectura optimista y `readLock()`?
<details><summary>Respuesta</summary>

**Corta:** `readLock()` **coge un lock**: bloquea a los escritores y lo que lees siempre vale. La optimista **no coge nada**: no bloquea a nadie, pero **tienes que validar** y, si falla, repetir.

**Ampliación:** tabla de `2_TECNICO.md`, apartado 10.

| | `readLock()` | Optimista |
|---|---|---|
| ¿Bloquea a los escritores? | Sí | No |
| ¿Hay que soltarlo? | Sí | No |
| ¿Hay que validar? | No | Sí |
| Coste si nadie escribe | Bajo | Mínimo |
</details>

### 9. ¿Qué ocurriría si el dato se modifica mientras se realiza una lectura optimista?
<details><summary>Respuesta</summary>

**Corta:** la lectura **no se entera en ese momento**, y puede copiar una mezcla de datos viejos y nuevos. Pero al hacer `validate(stamp)` sale `false`, y por eso **se descarta y se repite**.

**Ampliación:**
- El peligro es **usar los datos antes de validar**: por ejemplo, dividir entre un valor que vale 0 por estar a medias, o acceder a un índice de array.
- Primero se copia, luego se valida y luego se usa.
</details>

### 10. ¿Cuándo podría empeorar el rendimiento utilizando un `ReadWriteLock`?
<details><summary>Respuesta</summary>

**Corta:** cuando las lecturas son **muy cortas**, cuando hay **muchas escrituras** o cuando hay **pocos hilos**. Gestionar el lock de lectura cuesta más que lo que se gana.

**Ampliación:**
- En mi demo L05, escenario B, con lecturas instantáneas RW tarda ~860 ms y `synchronized` ~580 ms.
- La razón: cada `readLock().lock()` **modifica el contador compartido** de lectores, y 8 hilos peleándose por ese contador es más caro que leer un `int`.
- El modo justo empeora aún más el rendimiento.
</details>

---

## Preguntas extra (por si la profesora aprieta)

### 11. ¿Puedo pasar de `readLock` a `writeLock` sin soltarlo?
<details><summary>Respuesta</summary>

**Corta:** en `ReentrantReadWriteLock`, **no**. El hilo se queda bloqueado para siempre, porque el `writeLock` espera a que no queden lectores... y el lector es él.

**Ampliación:**
- Hay que soltar la lectura, pedir la escritura y **volver a comprobar** el dato.
- Bajar (de escritura a lectura) sí se puede.
- En `StampedLock` existe `tryConvertToWriteLock(stamp)`, que devuelve 0 si no puede.
</details>

### 12. ¿Qué es exactamente un stamp?
<details><summary>Respuesta</summary>

Un **`long`** que devuelve `StampedLock` al coger un modo. Funciona como un **ticket**:
- sirve para soltar el lock (`unlockRead(stamp)`);
- en la lectura optimista sirve para comprobar, con `validate(stamp)`, si la versión ha cambiado.

Un 0 significa "no conseguido".
</details>

### 13. ¿Qué significa "reentrante"?
<details><summary>Respuesta</summary>

Que **el mismo hilo** puede coger otra vez un lock que ya tiene, sin bloquearse. Lleva la cuenta y hay que hacer tantos `unlock()` como `lock()`.

`ReentrantLock` y `ReentrantReadWriteLock` son reentrantes; `StampedLock`, no.
</details>

### 14. ¿Qué es la fairness y por qué no se activa siempre?
<details><summary>Respuesta</summary>

Es una política de **orden de llegada**: el que más tiempo lleva esperando entra antes. Evita la starvation.

No se activa siempre porque es **más lenta**: obliga a dormir y despertar hilos más veces en lugar de dejar entrar al que llega justo cuando se libera el lock.
</details>

### 15. ¿Por qué hay que copiar los datos a variables locales en la lectura optimista?
<details><summary>Respuesta</summary>

Porque `validate` comprueba lo que has leído **antes** de llamarlo. Si después de validar vuelves a leer el campo, esa lectura nueva **ya no está protegida** y podría haber cambiado.
</details>

### 16. ¿`ReadWriteLock` es una clase?
<details><summary>Respuesta</summary>

No: es una **interfaz** con dos métodos, `readLock()` y `writeLock()`. La clase que la implementa es `ReentrantReadWriteLock`.

`StampedLock` **no** implementa esa interfaz. Ofrece vistas con `asReadLock()` y `asWriteLock()`.
</details>

### 17. ¿Qué alternativas hay a estos locks?
<details><summary>Respuesta</summary>

- `ConcurrentHashMap` para mapas.
- `CopyOnWriteArrayList` para listas que casi nunca cambian.
- `AtomicInteger` y `AtomicReference` para un solo valor.
- Objetos **inmutables** (`record`): si no cambian, no hay que protegerlos.
- `volatile` para un único dato simple que solo necesita que los cambios se vean.
</details>

### 18. ¿Qué pasa si hago `unlock()` de un lock que no tengo?
<details><summary>Respuesta</summary>

Salta `IllegalMonitorStateException`. En `StampedLock`, pasa lo mismo si suelto con un stamp que no corresponde.
</details>

### 19. ¿Puede un hilo con `writeLock` leer?
<details><summary>Respuesta</summary>

Sí. Puede leer directamente, porque está solo, y también puede pedir el `readLock` (bajar, o *downgrade*). Lo que no se puede es lo contrario.
</details>

### 20. En tu demo, ¿por qué el que no sincroniza va tan rápido?
<details><summary>Respuesta</summary>

Porque nadie espera a nadie. Pero da **resultados incorrectos**: lecturas incoherentes.

> *"Rápido y mal no sirve: el objetivo es correcto y, dentro de lo correcto, rápido."*
</details>
