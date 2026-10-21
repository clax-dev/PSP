# Ejercicio: caché de vulnerabilidades (demo para la exposición)

> **Opcional pero recomendado** por la profesora. Lo haces tú, porque si te preguntan por el código **tienes que poder defenderlo**.
> **Modelo:** `teoria/src/t3_locks/L05_Comparativa.java` hace lo mismo con la carta y la pizarra de la cafetería.

## Qué tiene que hacer

Una **ficha de vulnerabilidad** con **dos datos que siempre tienen que ir juntos**, por ejemplo:
- `gravedad` (un `int` del 1 al 10)
- `versionParche` (un `int`)

**Regla:** cada vez que el analista actualiza la ficha, pone **los dos al mismo número**. Si un lector ve dos números distintos, ha leído **a medias**.

- **Lectores** (varios hilos): consultan la ficha muchas veces y cuentan cuántas lecturas eran incoherentes.
- **Analista** (un hilo): actualiza la ficha unas pocas veces.

Hay que hacerlo con **4 versiones** de la ficha y mostrar, para cada una, el **tiempo** y las **lecturas incoherentes**:
1. Sin sincronización
2. `synchronized`
3. `ReentrantReadWriteLock`
4. `StampedLock` con lectura optimista

## Piezas (en este orden)

| # | Pieza | Pista |
|---|---|---|
| 1 | Una **interfaz** `FichaVulnerabilidad` con `boolean leerEsCoherente()` y `void actualizar(int nuevo)` | Así las 4 versiones se usan igual desde el `main` |
| 2 | Clase `FichaSinSync` que implementa la interfaz | Lee un dato, "trabaja" un poco (`Thread.sleep`) y lee el otro. Escribe igual: uno, pausa, el otro |
| 3 | `FichaSynchronized` | Lo mismo, con los métodos `synchronized` |
| 4 | `FichaReadWrite` | Un `ReentrantReadWriteLock` como atributo; leer con `readLock()`, escribir con `writeLock()`, y `unlock` en `finally` |
| 5 | `FichaStamped` | Un `StampedLock`; leer con la **receta de 5 pasos** (`teoria/tema3_locks/2_TECNICO.md`, apartado 9) y contar las veces que `validate` da `false` |
| 6 | Un método `medir(FichaVulnerabilidad ficha)` | Crea N hilos lectores y 1 analista, los arranca, hace `join` a todos, y devuelve o imprime el tiempo (`System.currentTimeMillis()`) y las incoherencias (`AtomicInteger`) |
| 7 | `main` | Llama a `medir` con cada una de las 4 fichas e imprime una tabla |

## Checklist

- [ ] Las 4 clases implementan la misma interfaz
- [ ] En `FichaSinSync` salen lecturas incoherentes (si salen 0, haz más larga la pausa entre los dos datos)
- [ ] Las otras 3 dan **0 incoherentes**
- [ ] `synchronized` es claramente más lento que RW y que Stamped
- [ ] Todos los `unlock` están en un `finally`
- [ ] En `FichaStamped` los datos se **copian a variables locales** y solo se usan **después** de `validate`
- [ ] Sabes explicar **cada línea** en voz alta

## Extra (si te sobra tiempo)

- Añade un **escenario B** con lecturas instantáneas (sin `sleep`) y comprueba que RW va **peor** que `synchronized` (pregunta 10).
- Prueba `new ReentrantReadWriteLock(true)` y compara el tiempo.

## Documentación

| Qué | Dónde |
|---|---|
| Teoría | `teoria/tema3_locks/` (1_HISTORIA, 2_TECNICO, 3_PREGUNTAS) |
| Receta de la lectura optimista | `2_TECNICO.md`, apartado 9 · PDF, página 7 |
| Modelo completo | `teoria/src/t3_locks/L05_Comparativa.java` |
| Crear hilos y `join` | `teoria/tema1_hilos/2_TECNICO.md`, apartado 4 |
