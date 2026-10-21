# Errores frecuentes (sacados de tus ejercicios de PSP)

> Repásalo antes de entregar un ejercicio: son errores que **ya has cometido**, así que es fácil que vuelvan.
> Actualizado el 21 de octubre.

## Tema 1: `Cafeteria.java`
| # | Error | Por qué está mal | Arreglo |
|---|---|---|---|
| 1 | `while (!cola.isEmpty())` + `cola.poll()` | "Comprobar y luego actuar": otro hilo se lleva el último cliente y `poll()` da `null` | `while ((c = cola.poll()) != null)` |
| 2 | `System.exit(0)` al final | Sobra: tras los `join()`, todos han terminado | Quitarlo |
| 3 | Mostrar el total dentro del contador, comprobando `if (!cola.isEmpty())` | No sabes si eres el último: otro puede estar todavía atendiendo | Mostrar el total en el `main`, **después** de los `join()` |

## Tema 2: `WriteCSV.java`
| # | Error | Por qué está mal | Arreglo |
|---|---|---|---|
| 4 | `LocalDate` con el patrón `HHmmss` | `LocalDate` no tiene hora → `UnsupportedTemporalTypeException: HourOfDay` | `LocalDateTime` |
| 5 | `YYYY` en el patrón | `YYYY` es el "año de la semana" y falla a finales de diciembre | `yyyy` |
| 6 | `createDirectories(fichero)` | Crea una **carpeta** llamada `pacientes.csv` | `createDirectories(carpeta)` y `write(fichero, …)` |
| 7 | `carpeta.resolve("…")` sin guardar el resultado | `resolve` **devuelve** un `Path` nuevo; no cambia `carpeta` | `Path fichero = carpeta.resolve("…")` |
| 8 | Llamar tú a `call()` | Se ejecuta en el `main`, sin pool | `pool.submit(tarea)` |
| 9 | Devolver la carpeta en vez del fichero | Las 5 tareas devolverían lo mismo | `return fichero;` |
| 10 | El nombre del fichero sin el número de tarea | Varias tareas caen en el mismo segundo y se machacan | `"pacientes_" + fecha + "_" + numero + ".csv"` |
| 11 | `for (i = 1; i < 5; i++) { …; i++; }` | Sube `i` dos veces y llega solo a 4: salen las tareas 1 y 3 | `for (int i = 1; i <= 5; i++)` sin el `i++` extra |
| 12 | `get()` dentro del bucle del `submit` | Espera a cada tarea antes de repartir la siguiente: 1 hilo cada vez | Bucle para repartir + bucle para recoger |
| 13 | `mostrarRuta(Path)` | `Path` es un **tipo**; en la llamada va **la variable** | `mostrarRuta(futures)` |
| 14 | `import static sun.swing.SwingUtilities2.submit;` | Import basura que mete el autocompletado | Borrarlo |
| 15 | `List` sin tipo | Aviso `unchecked` y casts | `List<Future<Path>>` |

## Generales
| # | Error | Arreglo |
|---|---|---|
| 16 | Llamar a `run()` en vez de `start()` | `start()` |
| 17 | `start()` y `join()` en el mismo bucle | Bucles separados |
| 18 | Un candado por hilo (o un objeto `synchronized` distinto por hilo) | Un candado `static final` o un objeto compartido |
| 19 | `unlock()` / `release()` fuera de `finally` | Siempre en `finally` |
| 20 | Olvidar `pool.shutdown()` | Al final del `main` |
