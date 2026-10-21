# Errores frecuentes (sacados de tus ejercicios de PSP)

Revisado por última vez el 2 de octubre.

| # | Archivo | Problema | Pista |
|---|---|---|---|
| 1 | `tema1_hilos/Cafeteria.java` | `while (!cola.isEmpty())` seguido de `cola.poll()` es un caso de "comprobar y luego actuar": `cliente` puede valer `null`. | `teoria/src/t1_hilos/H04_ColeccionesConcurrentes.java` |
| 2 | Mismo archivo | `System.exit(0)` no hace falta: después de los `join()` todos los hilos ya han terminado. | — |
