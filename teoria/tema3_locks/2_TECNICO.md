# Tema 3: Locks de lectura y escritura, la parte técnica (para consultar)

> **Este archivo no se lee de un tirón: se consulta.** Cuando estés programando o prepares una pregunta técnica, busca aquí.
> Los apartados empiezan en el **5** a propósito: así coinciden con los números que usa el plan de estudio de la exposición. Los apartados 1-4 (el nivel alto) están contados en `1_HISTORIA.md`.
> **Código:** `teoria/src/t3_locks/` (L01 a L05), con la carta y la pizarra de la cafetería.

**Índice:**
- 5. Lock tradicional frente a lock de lectura y escritura
- 6. `ReadWriteLock` y `ReentrantReadWriteLock` (+ ⭐ plantilla "leer y, solo si hace falta, escribir")
- 7. Fairness y starvation
- 8. `ReentrantLock` frente a `ReentrantReadWriteLock`
- 9. `StampedLock` (receta de la lectura optimista y `tryConvertToWriteLock`)
- 10. Funcionamiento y limitaciones de la lectura optimista
- 11. Cuándo NO compensa
- 12. Por qué hay que liberar SIEMPRE los locks
- 13. Resumen final y "¿cuál elijo?"

---

## 5. Lock tradicional frente a lock de lectura y escritura

| | Lock tradicional (`synchronized`, `ReentrantLock`) | Lock de lectura/escritura |
|---|---|---|
| Cerrojos | **Uno** | **Dos** que trabajan juntos (lectura + escritura) |
| Lector + lector | Esperan (uno cada vez) | **A la vez** ✅ |
| Lector + escritor | Uno cada vez | Uno cada vez |
| Escritor + escritor | Uno cada vez | Uno cada vez |
| Tipo de acceso | Siempre **exclusivo** | Lectura **compartida**, escritura **exclusiva** |

> **Demos L01 y L02.** Sin protección, salen cientos de lecturas incoherentes. Con `synchronized` salen 0, pero 4 lectores tardan **~2400 ms**, porque van en fila.

## 6. `ReadWriteLock` y `ReentrantReadWriteLock`

- **`ReadWriteLock`** es una **interfaz** (`java.util.concurrent.locks`). Solo define dos métodos:
  - `readLock()`: devuelve el `Lock` de **lectura** (compartido).
  - `writeLock()`: devuelve el `Lock` de **escritura** (exclusivo).
- **`ReentrantReadWriteLock`** es **la implementación** que usamos.

```java
ReadWriteLock rw = new ReentrantReadWriteLock();   // o (true) para el modo justo
Lock lectura   = rw.readLock();
Lock escritura = rw.writeLock();

// LEER
lectura.lock();
try {
    return precio;              // muchos hilos pueden estar aquí a la vez
} finally {
    lectura.unlock();
}

// ESCRIBIR
escritura.lock();
try {
    precio = nuevo;             // aquí solo hay UN hilo, y ningún lector
} finally {
    escritura.unlock();
}
```

Los dos son `Lock` normales, así que tienen lo mismo que `ReentrantLock`: `lock()`, `unlock()`, `tryLock()`, `tryLock(tiempo, unidad)` y `lockInterruptibly()`.

### Tabla de quién puede entrar

| Quién está dentro | ¿Entra un lector? | ¿Entra un escritor? |
|---|---|---|
| Nadie | ✅ | ✅ |
| 1 o más lectores | ✅ | ❌ espera a que salgan **todos** |
| Un escritor | ❌ | ❌ |

### Lecturas simultáneas

Mientras **nadie tenga el `writeLock`**, todos los hilos que piden `readLock().lock()` entran **sin esperar**.

> **Demo L03, parte 1:** 4 lectores, con un **máximo de 4 dentro a la vez**. Tardan **~560 ms**, frente a los ~2400 ms de `synchronized`.

### Exclusión de lectores durante una escritura

Pasan dos cosas:
- El escritor que pide `writeLock().lock()` **espera** a que salga **el último lector**.
- Mientras el escritor está dentro, **ningún lector entra**.

> **Demo L03, parte 2 (salida real):**
> ```
> [ 576 ms] Lector-1   LEYENDO
> [ 577 ms] Lector-2   LEYENDO
> [ 612 ms] Escritor   quiere ESCRIBIR -> tiene que esperar a que no quede ningún lector
> [ 663 ms] Lector-3   quiere leer                ← llega con lectores dentro... y NO se cuela
> [ 878 ms] Lector-1   deja de leer
> [ 878 ms] Lector-2   deja de leer
> [ 879 ms] Escritor   ESCRIBIENDO (solo)
> [ 980 ms] Escritor   termina de escribir
> [ 980 ms] Lector-3   LEYENDO                    ← entra cuando el escritor termina
> ```
> El Lector-3 llega cuando todavía hay lectores dentro, pero **se pone detrás del escritor que ya esperaba**. Si se colara, podría llegar otro lector, y otro, y el escritor no entraría nunca (**starvation**, ver el apartado 7).

### Reentrante: qué se puede y qué no

"Reentrante" significa que **el mismo hilo** puede volver a coger un lock que ya tiene. Tendrá que soltarlo las mismas veces que lo cogió.

| Tengo… | Pido… | Resultado |
|---|---|---|
| `readLock` | `readLock` | ✅ Reentrante |
| `writeLock` | `writeLock` | ✅ Reentrante |
| `writeLock` | `readLock` | ✅ **Bajar** (*downgrade*): se permite |
| `readLock` | `writeLock` | ❌ **Subir** (*upgrade*): **el hilo se bloquea para siempre** |

**¿Por qué no se puede subir?**
- El `writeLock` espera a que **no quede ningún lector**... y el lector eres **tú**. Te esperas a ti mismo.
- Además, si dos lectores intentaran subir a la vez, cada uno esperaría a que saliera el otro: un **interbloqueo** seguro.

**Qué se hace en su lugar:**
1. Soltar el `readLock`.
2. Pedir el `writeLock`.
3. **Volver a comprobar** el dato, porque entre el paso 1 y el 2 otro hilo ha podido cambiarlo.

> **Demo L03, parte 3:** con el `readLock` cogido, `writeLock().tryLock(1 s)` devuelve **`false`**.

### ⭐ Plantilla: leer y, solo si hace falta, escribir
Solo se suelta la lectura **si de verdad hay que escribir**. Si lo que sigue es leer, te quedas dentro y aprovechas la reentrancia.

```java
public String consultarFicha() {
    boolean hayQueCorregir;

    lectura.lock();
    try {
        hayQueCorregir = gravedad < 5;
        if (!hayQueCorregir) {
            return resumen();          // resumen() pide lectura.lock() → ✅ reentrante
        }
    } finally {
        lectura.unlock();              // se ejecuta también tras el return
    }

    escritura.lock();                  // solo llego aquí si hay que ESCRIBIR
    try {
        if (gravedad < 5) {            // ⚠ vuelvo a comprobar: en el hueco pudo cambiar
            gravedad = 9;
        }
        return resumen();              // con la escritura cogida pido lectura → ✅ bajar se puede
    } finally {
        escritura.unlock();
    }
}

private String resumen() {
    lectura.lock();
    try {
        return "CVE-1234 · gravedad " + gravedad;
    } finally {
        lectura.unlock();
    }
}
```

| Camino | Locks | ¿Reentrancia? |
|---|---|---|
| No hay que corregir | lectura → `resumen()` pide lectura | ✅ lectura → lectura |
| Hay que corregir | lectura → **suelto** → escritura → `resumen()` pide lectura | ✅ escritura → lectura (bajar) |


## 7. Fairness y starvation

**Starvation** (inanición) es cuando un hilo **espera indefinidamente** porque los demás siempre se le adelantan.

> **Ejemplo típico:** llegan lectores sin parar. Siempre hay alguno dentro, así que **el escritor nunca encuentra la sala vacía** y no escribe jamás.

**Fairness** (justicia) es la política que decide **quién entra cuando se libera el lock**:

| Constructor | Modo | Cómo se comporta |
|---|---|---|
| `new ReentrantReadWriteLock()` | **No justo** (por defecto) | No respeta el orden de llegada: alguien recién llegado puede colarse. Da **más rendimiento**. Para no castigar a los escritores, un lector nuevo **espera si el primero de la cola es un escritor**, pero es una medida aproximada, no una garantía |
| `new ReentrantReadWriteLock(true)` | **Justo** | Entran **más o menos por orden de llegada**. Evita la starvation, pero es **más lento** (más esperas y cambios de hilo) |

> ⚠️ El modo justo **reduce** la starvation, pero cuesta rendimiento. Úsalo solo si en tu caso hay muchos lectores sin parar y escribir es importante.

## 8. `ReentrantLock` frente a `ReentrantReadWriteLock`

| | `ReentrantLock` | `ReentrantReadWriteLock` |
|---|---|---|
| Qué es | **Un** lock exclusivo | **Una pareja**: lectura (compartido) + escritura (exclusivo) |
| ¿Lectores a la vez? | ❌ | ✅ |
| ¿Reentrante? | ✅ | ✅ (los dos locks) |
| Fairness (`true`) | ✅ | ✅ |
| `tryLock`, `lockInterruptibly` | ✅ | ✅ |
| `Condition` (`newCondition()`) | ✅ | Solo el **writeLock** (el readLock lanza `UnsupportedOperationException`) |
| Coste de cada `lock()` | Menor | **Mayor**: lleva la cuenta de cuántos lectores hay y si hay un escritor |
| Cuándo | Lecturas y escrituras parecidas, o secciones muy cortas | **Muchas lecturas** que tardan algo y **pocas escrituras** |

## 9. `StampedLock`

`StampedLock` llegó en Java 8 y está en `java.util.concurrent.locks`.

**No implementa `Lock` ni `ReadWriteLock`.** Cada vez que coges un modo te devuelve un **stamp**: un `long` que funciona como un ticket. Para soltar el lock **tienes que devolver ese ticket**.

| Modo | Coger | Soltar | Bloquea |
|---|---|---|---|
| Escritura | `long s = sl.writeLock();` | `sl.unlockWrite(s);` | A todos |
| Lectura (pesimista) | `long s = sl.readLock();` | `sl.unlockRead(s);` | Solo a los escritores |
| **Lectura optimista** | `long s = sl.tryOptimisticRead();` | (no hay nada que soltar) | **A nadie** |

Detalles del stamp:
- `sl.unlock(s)` suelta el modo que sea.
- Un stamp **`0`** significa **"no conseguido"**: por ejemplo, `tryOptimisticRead()` devuelve 0 si alguien tiene ahora mismo el `writeLock`.

### `tryOptimisticRead()` y `validate(long stamp)`

La receta tiene siempre estos 5 pasos:

```java
long stamp = sl.tryOptimisticRead();   // 1. apunto la "versión" (NO cojo ningún lock)
int carta   = precioCarta;             // 2. copio los datos a variables LOCALES
int pizarra = precioPizarra;

if (!sl.validate(stamp)) {             // 3. ¿ha habido alguna escritura desde el paso 1?
    stamp = sl.readLock();             // 4. sí → lo copiado puede ser basura: plan B, leer con lock
    try {
        carta   = precioCarta;
        pizarra = precioPizarra;
    } finally {
        sl.unlockRead(stamp);
    }
}
usar(carta, pizarra);                  // 5. SOLO AHORA uso los datos
```

`validate(stamp)` devuelve:
- **`true`**: no ha habido **ninguna escritura** desde que se obtuvo el stamp, así que lo copiado es **coherente**.
- **`false`**: **alguien ha cogido el `writeLock`** entretanto, o el stamp era 0. Lo copiado **no vale**.

> **Demo L04, parte 2 (salida real):**
> - De 200 lecturas optimistas, **40** fallaron la validación.
> - De esas 40, **32 eran copias a medias** (la carta y la pizarra no coincidían).
> - Gracias a `validate`, se usaron **0** datos incoherentes.
>
> **Por eso es obligatorio validar.**

### `tryConvertToWriteLock(long stamp)`: el "subir" que RW no permitía

`tryConvertToWriteLock` intenta convertir un stamp de lectura (o uno optimista) en un stamp de **escritura** **sin soltar nada**.
- Devuelve el **nuevo stamp** si lo consigue: por ejemplo, si eras el único lector.
- Devuelve **`0`** si no lo consigue. Entonces sueltas el lock, pides `writeLock()` y vuelves a comprobar.

```java
long stamp = sl.readLock();
try {
    while (precio < 150) {
        long ws = sl.tryConvertToWriteLock(stamp);
        if (ws != 0L) { stamp = ws; precio = 150; }              // conseguido
        else { sl.unlockRead(stamp); stamp = sl.writeLock(); }   // plan B
    }
} finally {
    sl.unlock(stamp);
}
```

### Lo que `StampedLock` NO tiene (preguntas trampa)

| | `StampedLock` |
|---|---|
| ¿Reentrante? | ❌ **No.** Si tienes el `writeLock` y lo pides otra vez, **te bloqueas**. Demo L04, parte 4: `tryWriteLock()` devuelve 0 |
| ¿Fairness? | ❌ No tiene modo justo; no garantiza preferencia por lectores ni por escritores |
| ¿`Condition` (await/signal)? | ❌ No |
| ¿Saber qué hilo lo tiene? | ❌ No tiene "dueño": solo importa el stamp |

## 10. Funcionamiento y limitaciones de la lectura optimista

**Cómo funciona por dentro:** el `StampedLock` guarda un **contador de versión**.
- Cada vez que alguien **coge el `writeLock`**, el contador cambia.
- `tryOptimisticRead()` te da el valor actual del contador.
- `validate(s)` mira si el contador sigue igual.

Lo que la lectura optimista **no hace** es igual de importante:
- **No bloquea a los escritores.** Un escritor puede entrar en mitad de tu lectura optimista; tú te enteras después, con `validate`. *(Demo L04, parte 1: el escritor escribe sin esperar y luego `validate` devuelve `false`.)*
- No se apunta en ningún sitio, así que **no hay que soltarla** (no existe `unlockOptimistic`).

| Limitación | Por qué importa |
|---|---|
| **Hay que validar siempre** | Sin `validate`, puedes usar datos a medias |
| **No uses los datos antes de validar** | Dividir entre un valor que aún es "basura" o usar un índice de array sin validar puede **lanzar excepciones**. Primero copia, luego valida y luego usa |
| **Copia a variables locales** | Si vuelves a leer el campo después de validar, ya no está protegido |
| **Solo sirve para leer** | Para escribir hace falta `writeLock` (o `tryConvertToWriteLock`) |
| **Si hay muchas escrituras, falla mucho** | Cada fallo es leer **dos veces**; en ese caso, mejor `readLock` directamente |
| **Código más difícil** | Más fácil equivocarse que con `synchronized` |
| **StampedLock no es reentrante** | Si un método protegido llama a otro que pide el mismo lock, el hilo se bloquea |

### Lectura optimista frente a `readLock()`

| | `readLock()` (pesimista) | `tryOptimisticRead()` |
|---|---|---|
| ¿Coge un lock? | Sí | **No** |
| ¿Bloquea a los escritores? | **Sí**, mientras lees | **No** |
| ¿Hay que soltarlo? | Sí, `unlockRead(s)` | No |
| ¿Hay que validar? | No: lo leído siempre vale | **Sí**, con `validate(s)` |
| ¿Si escriben mientras lees? | No puede pasar | `validate` devuelve `false` → repetir |
| Coste | Bajo-medio | **Mínimo** si nadie escribe |

## 11. Cuándo NO compensa usar estos mecanismos

| Situación | Por qué no compensa | Mejor usar |
|---|---|---|
| **La sección crítica es cortísima** (leer un `int`) | Gestionar el contador de lectores cuesta **más** que la propia lectura | `synchronized`, `volatile` o un `Atomic…` |
| **Hay casi tantas escrituras como lecturas** | Los lectores casi nunca coinciden solos, y el optimista falla todo el rato | `synchronized` / `ReentrantLock` |
| **Pocos hilos** o **poca competencia** | No hay a quién dejar entrar a la vez | `synchronized` |
| **Necesitas reentrancia o `Condition`** | `StampedLock` no los tiene | `ReentrantReadWriteLock` o `ReentrantLock` |
| **Ya existe una estructura concurrente** | Ya está resuelto, y mejor | `ConcurrentHashMap`, `CopyOnWriteArrayList`… |
| **El dato no cambia nunca** | No hay nada que proteger | Objetos **inmutables** (`record`, `final`) |

> **Demo L05, escenario B (salida real):** 8 lectores, pero con lecturas **instantáneas**:
> ```
> CartaSynchronized            582 ms
> CartaReadWrite               858 ms    ← ¡PEOR que synchronized!
> CartaStamped                  16 ms
> ```
> Con 8 lectores parecía el caso ideal para RW, pero cada `lock()` de lectura **modifica el contador compartido de lectores**, y 8 hilos peleándose por ese contador cuestan más que leer.

## 12. Por qué hay que liberar SIEMPRE los locks

- `synchronized` se libera **solo** al salir del bloque, incluso con una excepción.
- `Lock` y `StampedLock` **no**: si se lanza una excepción antes de `unlock()`, el lock **se queda cogido para siempre**.
  - Con un `writeLock`, **nadie** vuelve a leer ni a escribir: el programa se congela sin dar ningún error.
  - Con un `readLock`, **ningún escritor** vuelve a entrar.

Por eso:
```java
lock.lock();          // FUERA del try: si lock() fallara, no hay nada que soltar
try {
    ...
} finally {
    lock.unlock();    // SIEMPRE se ejecuta
}
```

Errores típicos al soltar:
- Soltar un lock que no tienes lanza `IllegalMonitorStateException`.
- En `StampedLock`, soltar con un stamp **equivocado** también.
- Si coges un lock reentrante **dos veces**, tienes que soltarlo **dos veces**.

## 13. Resumen final: la tabla de las tres

| | `ReentrantLock` | `ReentrantReadWriteLock` | `StampedLock` |
|---|---|---|---|
| Lectores a la vez | ❌ | ✅ | ✅ |
| Lectura sin bloquear | ❌ | ❌ | ✅ (optimista) |
| ¿Reentrante? | ✅ | ✅ | ❌ |
| Fairness | ✅ opcional | ✅ opcional | ❌ |
| `Condition` | ✅ | Solo escritura | ❌ |
| Subir de lectura a escritura | — | ❌ (el hilo se bloquea) | ✅ `tryConvertToWriteLock` |
| Cómo se suelta | `unlock()` | `unlock()` en cada lock | `unlockRead/Write(stamp)` |
| Dificultad | Fácil | Media | **Alta** |
| Ideal para | Casos generales | Muchas lecturas lentas, pocas escrituras | Muchísimas lecturas cortas, escrituras raras, máximo rendimiento |

### ¿Cuál elijo?

```
¿El dato se lee mucho más de lo que se escribe?
├── No  → synchronized / ReentrantLock
└── Sí  → ¿Leer tarda algo (no es un simple int)?
          ├── No  → synchronized, Atomic o volatile (RW no compensa)
          └── Sí  → ¿Necesitas reentrancia, Condition o fairness?
                    ├── Sí → ReentrantReadWriteLock
                    └── No → ¿Necesitas el máximo rendimiento y el código no te asusta?
                             ├── Sí → StampedLock (lectura optimista)
                             └── No → ReentrantReadWriteLock
```
