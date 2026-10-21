# Tema 3: Locks de lectura y escritura, contado como una historia

> **Cómo usar este archivo:**
> - Léelo **de un tirón**, como un cuento. Aquí no hay sintaxis: solo **qué problema hay** y **cómo se arregla**.
> - Cada bloque tiene una **💬 frase clave**. Si te la sabes, sabes el bloque.
> - Ten abierto `visual.pdf` al lado (es el mismo PDF de la exposición): cada bloque te dice qué página mirar.
> - La sintaxis está en `2_TECNICO.md` (apartados 5 a 13), y para comprobarte tienes `3_PREGUNTAS.md`.
> - **Exposición:** `exposiciones/01_locks_lectura_escritura/` (guion y plan de estudio).

**La historia entera en tres líneas:**
- En los temas 1 y 2, el candado (`synchronized`, `ReentrantLock`) dejaba entrar **a uno cada vez**, para todo.
- Pero muchas veces casi todos solo **miran** y casi nadie **cambia** nada. Hacerles cola a todos es un desperdicio.
- Este tema va de candados más listos, que **distinguen entre leer y escribir**.

---

## 1. El problema: leer mientras alguien escribe
📄 *PDF, páginas 1 y 2* · 🧪 *Demos `L01` y `L02`*

> 💬 **"Leer a la vez no es peligroso. Lo peligroso es leer mientras alguien escribe."**

Hay una **ficha** (por ejemplo, de una vulnerabilidad) que consultan muchos y que de vez en cuando actualiza uno.

| Situación | ¿Problema? | Por qué |
|---|---|---|
| Dos **leen** a la vez | ❌ No | Mirar no cambia nada |
| Uno **lee** mientras otro **escribe** | ✅ Sí | Puede ver el dato **a medias** |
| Dos **escriben** a la vez | ✅ Sí | Se machacan los cambios |

**¿Qué es un dato "a medias"?** El precio del café está en **dos sitios**, la carta y la pizarra, y tienen que coincidir. El camarero cambia primero la carta y luego la pizarra. Un cliente que mira **justo en medio** ve la carta nueva y la pizarra vieja: **un dato incoherente**, que no ha existido ni antes ni después del cambio.
> Si el cambio se hiciera de un solo golpe, no habría hueco y nadie podría verlo a medias. Lo que hace un lock de escritura es **que un cambio de varios pasos parezca uno solo desde fuera**.

**¿Y con `synchronized`?** Se arregla, pero **entra uno cada vez**, también los que solo miran. Los lectores **hacen cola entre ellos** sin necesidad.
> En las demos: sin protección salen cientos de lecturas incoherentes (L01). Con `synchronized` sale todo bien, pero tarda **~2400 ms** (L02).

---

## 2. La biblioteca: dos tipos de pase
📄 *PDF, página 3*

> 💬 **"Muchos pueden consultar a la vez, pero para reorganizar las estanterías la sala tiene que estar vacía."**

- **Candado normal** = **un solo pase** para todo. Da igual que vayas a consultar o a reorganizar: entra uno y el resto, a la cola.
- **Lock de lectura y escritura** = **dos tipos de pase**:
  - **Pase de lector**: se da a todo el que lo pida. Muchos consultan **a la vez**.
  - **Pase de reorganizar**: es **único**. Para dártelo, la sala tiene que estar **vacía**, y mientras lo tienes **no entra nadie**.

Funciona **en las dos direcciones**:
1. El que va a reorganizar **espera a que salgan todos** los que están leyendo.
2. Mientras reorganiza, **no entra nadie**, ni a leer ni a reorganizar.

Así nadie ve una estantería a medio ordenar.

> ⚠ **Quién coge cada pase:** el **escritor** coge el `writeLock` y el **lector** coge el `readLock`. Si el escritor tiene el suyo, al lector **no le dan** el `readLock` y espera.

> En la demo L03: 4 lectores dentro a la vez, y tardan **~560 ms** en vez de 2400.

---

## 3. Que nadie espere para siempre: starvation y fairness
📄 *PDF, página 5* · 🧪 *Demo `L03`, parte 2*

> 💬 **"Si los lectores pudieran colarse siempre, el escritor no entraría nunca."**

**Starvation** (inanición) = alguien **espera indefinidamente** porque los demás siempre se le adelantan. Por ejemplo: llegan lectores sin parar, siempre hay alguno dentro y el escritor **nunca encuentra la sala vacía**.

**Cómo lo evita Java (`ReentrantReadWriteLock`):**
- **En modo normal** ("no justo", el de por defecto): un lector nuevo **se pone a esperar si el primero de la cola es un escritor**. No se cuela. Es una medida aproximada, no una garantía, pero funciona casi siempre.
- **En modo justo** (*fairness*): se entra **por orden de llegada**, siempre. Evita la starvation, pero **es más lento**.

> En la demo L03, parte 2: el Lector-3 llega cuando todavía hay lectores dentro, pero **espera detrás del escritor**, que ya estaba esperando.

---

## 4. Reentrante, y por qué no se puede "subir"
📄 *PDF, página 5* · 🧪 *Demo `L03`, parte 3*

> 💬 **"Reentrante = si ya tengo el pase, puedo volver a pasar. Pero con el pase de lector no me dan el de reorganizar: me esperaría a mí mismo."**

**Reentrante:** si ya tienes un pase y pasas otra vez por una puerta que pide **el mismo**, el guardia te deja, porque sabe que es tuyo. En código, pasa cuando un método con el lock llama a otro método que pide **el mismo** lock. El lock lleva la cuenta, y hay que soltarlo tantas veces como se cogió.

| Tengo… | Pido… | ¿Me dejan? |
|---|---|---|
| lectura | lectura | ✅ (reentrante) |
| escritura | escritura | ✅ (reentrante) |
| escritura | lectura | ✅ (**bajar** se puede) |
| **lectura** | **escritura** | ❌ **Me quedo bloqueado para siempre** |

**¿Por qué no se puede subir?** Estás **consultando** dentro de la sala y pides el pase de **reorganizar**. El guardia te dice: *"cuando la sala esté vacía"*. Pero **tú sigues dentro**, esperando, así que la sala nunca se vacía.
- **No salta ningún error:** el programa se congela, sin más.
- **Qué hacer:** soltar la lectura, pedir la escritura y **volver a comprobar** el dato, porque en el hueco otro ha podido cambiarlo. Es la misma trampa de "comprobar y luego actuar" del tema 1.

---

## 5. Leer "a ojo": la lectura optimista
📄 *PDF, páginas 6 y 7* · 🧪 *Demo `L04`*

> 💬 **"Leo sin pedir permiso y luego compruebo si alguien ha escrito. Si sí, repito, esta vez con permiso."**

La pizarra tiene un **número de versión** en la esquina, que sube cada vez que alguien la cambia:
1. **Apunto la versión**: "v7".
2. **Hago una foto** a la pizarra **sin avisar a nadie**.
3. **Vuelvo a mirar la versión.**
   - Sigue siendo v7 → nadie la tocó: mi foto vale. ✅
   - Ahora es v8 → la cambiaron mientras hacía la foto. Puede que esté **a medias**, así que la tiro y **repito con permiso** (pase de lector). 🔁

**Por qué compensa:** como no pido permiso, **el que escribe no tiene que esperarme**. Si casi nadie escribe, leer sale casi gratis.

**Lo que hay que tener claro:**
- La lectura optimista **no bloquea a los escritores**: pueden escribir mientras yo leo.
- Por eso **hay que comprobar siempre** la versión (`validate`). Sin comprobar, podría usar datos a medias.
- **Solo uso lo que he leído DESPUÉS** de comprobar.
- Si escriben mucho, la comprobación falla una y otra vez, y no compensa.
- Esto solo lo tiene **`StampedLock`**. Y `StampedLock` **no es reentrante**: si pides otra vez un pase que ya tienes, te bloqueas a ti mismo.

> En la demo L04: de 200 lecturas optimistas, 40 fallaron la comprobación, y 32 de ellas eran fotos a medias. Gracias a la comprobación, **se usaron 0 datos incoherentes**.

---

## 6. Las tres herramientas
📄 *PDF, página 8*

> 💬 **"Un pase único · dos tipos de pase · dos tipos de pase y además mirar sin pase."**

| Herramienta | En la biblioteca | Lo que la distingue |
|---|---|---|
| `ReentrantLock` (y `synchronized`) | Un pase para todo | Sencillo, para uso general |
| `ReentrantReadWriteLock` | Pase de lector compartido + pase de escritor único | Reentrante y con modo justo; no se puede subir de lectura a escritura |
| `StampedLock` | Lo mismo + leer a ojo y comprobar | Lectura optimista; **no** reentrante, sin modo justo; puede intentar subir (`tryConvertToWriteLock`) |

---

## 7. Cuándo NO compensa
📄 *PDF, páginas 9 y 10* · 🧪 *Demo `L05`, escenario B*

> 💬 **"Compensa con muchas lecturas que tardan algo y pocas escrituras. Si no, `synchronized` es más simple y a veces más rápido."**

- **Lecturas cortísimas** (leer un número): llevar la cuenta de cuántos lectores hay dentro cuesta **más** que la propia lectura.
  > En la demo L05, con lecturas instantáneas: `ReentrantReadWriteLock` **858 ms**, `synchronized` **582 ms**. ¡Peor!
- **Muchas escrituras:** los lectores casi nunca coinciden solos, y la lectura optimista falla todo el rato.
- **Pocos hilos:** no hay a quién dejar pasar a la vez.
- **Ya hay algo hecho:** `ConcurrentHashMap` y similares.
- **El dato no cambia nunca:** no hay nada que proteger.

**Y siempre:** el que coge un lock lo **suelta**, pase lo que pase (`finally`). Si no, el pase se queda cogido para siempre y el programa se congela sin dar ningún error.

---

## 8. Resumen

| Concepto | Frase clave |
|---|---|
| El problema | Leer a la vez no es peligroso; leer mientras alguien escribe, sí |
| Dato a medias | Mitad nuevo, mitad viejo: incoherente |
| `readLock` / `writeLock` | Pase de lector compartido / pase de escritor único |
| El escritor | Espera a que salgan todos; mientras escribe, no entra nadie |
| Starvation | Alguien espera para siempre |
| Fairness | Por orden de llegada: sin starvation, pero más lento |
| Reentrante | Si ya tengo el pase, puedo volver a pasar |
| Subir de lectura a escritura | No se puede: me esperaría a mí mismo |
| Lectura optimista | Leo sin permiso, compruebo la versión y, si cambió, repito con permiso |
| `StampedLock` | Tiene la lectura optimista; no es reentrante |
| Cuándo no compensa | Lecturas cortísimas, muchas escrituras, pocos hilos |

**¿Y ahora?**
1. Repasa el `visual.pdf` entero.
2. Haz `3_PREGUNTAS.md` tapando las respuestas.
3. Para el código, `2_TECNICO.md`.
