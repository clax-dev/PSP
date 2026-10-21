# Guion de la exposición (5 minutos)

> **No te lo aprendas de memoria.** Apréndete **la idea de cada bloque** y la **frase clave**, y el resto dilo con tus palabras. Es justo lo que evalúan.
> Apóyate en el PDF `visuales/locks_visual.pdf`: cada bloque indica qué página enseñar.

| # | Bloque | Tiempo | Página del PDF |
|---|---|---|---|
| 1 | El problema | 0:00 – 0:40 | 1–2 |
| 2 | Analogía (nivel alto) | 0:40 – 1:30 | 3 |
| 3 | `ReentrantReadWriteLock` (nivel bajo) | 1:30 – 2:30 | 4–5 |
| 4 | Comparativa de las tres | 2:30 – 3:00 | 8 |
| 5 | Lectura optimista (`StampedLock`) | 3:00 – 4:00 | 6–7 |
| 6 | Demo | 4:00 – 4:40 | 9 (o la consola) |
| 7 | Conclusión y casos de uso | 4:40 – 5:00 | 10 |

---

## 1. El problema (40 s)
> **Frase clave:** *"Leer a la vez no es peligroso; lo peligroso es leer mientras alguien escribe."*

- Tenemos un dato que comparten muchos hilos, por ejemplo una **ficha de vulnerabilidad**: muchos la consultan y un analista la actualiza de vez en cuando.
- Si alguien la lee justo cuando la están cambiando, puede ver **una mitad vieja y otra nueva**.
- Con `synchronized` se arregla, pero **entra uno cada vez**, y los lectores hacen cola **entre ellos**, aunque no se molesten. Desperdiciamos rendimiento.

## 2. Analogía de la biblioteca (50 s)
> **Frase clave:** *"Muchos pueden consultar a la vez, pero para reorganizar las estanterías la sala tiene que estar vacía."*

- Biblioteca = el dato. Lectores = los que consultan. Bibliotecario que reorganiza = el escritor.
- Con un lock normal **hay un solo pase**.
- Con un lock de lectura/escritura **hay dos tipos de pase**: el de lector se reparte a muchos y el de reorganizar es único.
- Mientras reorganizas no entra nadie. Así nadie ve la estantería a medio ordenar.

## 3. `ReentrantReadWriteLock` (1 min)
> **Frase clave:** *"Un mismo objeto con dos llaves: `readLock()` compartida y `writeLock()` exclusiva."*

- `ReadWriteLock` es la **interfaz** y `ReentrantReadWriteLock` la **implementación**.
- Se usan igual que `ReentrantLock`: `lock()`, `try` y `unlock()` en un `finally`.
- **Página 4 (la tabla):** lector con lector, sí; escritor con cualquiera, no.
- **Página 5 (la línea de tiempo):** el escritor espera a que salgan **todos** los lectores, y los lectores que llegan después **se ponen detrás de él**. Si no, el escritor podría no entrar nunca: eso es la **starvation**.
- **Fairness:**
  - por defecto el lock es "no justo": va más rápido, pero no respeta el orden;
  - con `true` respeta el orden de llegada y evita la starvation, a cambio de ir más lento.
- **Trampa:** con el `readLock` cogido **no puedes pedir** el `writeLock`, porque te esperarías a ti mismo. Hay que soltar el de lectura y pedir el de escritura.

## 4. Comparativa (30 s)
> **Frase clave:** *"`ReentrantLock` es un pase único, `ReentrantReadWriteLock` son dos pases, y `StampedLock` añade mirar sin pase."*

- Enseña la tabla de la página 8. Destaca tres filas: **lectores a la vez**, **reentrante** y **lectura sin bloquear**.

## 5. Lectura optimista (1 min)
> **Frase clave:** *"Leo sin pedir permiso y luego compruebo si alguien ha escrito. Si sí, repito con permiso."*

- **Nivel alto:** la foto a la pizarra con un número de versión (página 6).
- **Nivel bajo** (página 7):
  1. `long stamp = tryOptimisticRead()`
  2. copiar los datos a variables locales
  3. `validate(stamp)`: si da `true`, uso los datos; si da `false`, repito con `readLock()`.
- **Lo importante:**
  - **no bloquea a los escritores**;
  - por eso **hay que validar**: sin validar puedes usar datos a medias;
  - `StampedLock` **no es reentrante**;
  - `tryConvertToWriteLock` sí permite subir de lectura a escritura.

## 6. Demo (40 s)
Ejecuta `L05_Comparativa` (o tu demo de `ejercicios/tema3_locks`) y señala:
- Sin sincronizar, **sale rápido pero con lecturas incoherentes**.
- Con `synchronized` sale **correcto pero lento (~2400 ms)**.
- Con RW y con Stamped sale **correcto y rápido (~560 ms)**.
- Bonus (escenario B): con lecturas cortísimas, **RW va peor que `synchronized`**.

> ⚠️ Si falla el proyector o el ordenador, ten **una captura** de la salida en el PDF (página 9).

## 7. Conclusión (20 s)
> **Frase clave:** *"Compensan cuando hay muchas lecturas que tardan algo y pocas escrituras. Si no, `synchronized` es más simple y a veces más rápido."*

- **Casos de uso:** cachés, configuración, catálogos, fichas que se consultan mucho.
- **No compensan:**
  - lecturas cortísimas;
  - muchas escrituras;
  - pocos hilos;
  - cuando ya hay una colección concurrente que lo resuelve (`ConcurrentHashMap`).

---

## Si te cortan con una pregunta técnica
Respira y sigue la estructura **"idea → API → ejemplo"**:
1. Una frase de nivel alto, con la analogía.
2. El nombre exacto del método.
3. "En mi demo se ve en…".

Las respuestas están preparadas en `PREGUNTAS.md`.
