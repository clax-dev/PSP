# PSP: Programación de Servicios y Procesos (DAM 2º)

Proyecto de IntelliJ con **dos módulos**: ejercicios y teoría.

```
PSP/
├── ejercicios/                      ← TUS EJERCICIOS
│   └── src/
│       ├── tema1_hilos/         Cafeteria.java
│       ├── tema2_concurrencia/  RaceConditionCounter, SynchronizedBlockCounter, WriteCSV (ver ENUNCIADO_WriteCSV.md)
│       └── tema3_locks/         tu demo de la exposición (ver ENUNCIADO_CacheVulnerabilidades.md)
│
├── teoria/                          ← TEORÍA: una carpeta por tema, siempre con los mismos 4 archivos
│   ├── tema1_hilos/                 1_HISTORIA.md   el tema contado como una historia (empieza aquí)
│   ├── tema2_concurrencia/          2_TECNICO.md    sintaxis y plantillas, para consultar
│   ├── tema3_locks/                 3_PREGUNTAS.md  autoevaluación con la respuesta tapada
│   │                                visual.pdf      diagramas (visual.html = su fuente)
│   ├── CHULETA.md                   ★ las frases clave de todos los temas
│   ├── ERRORES_FRECUENTES.md        ★ los fallos de tus ejercicios
│   ├── _backup_apuntes_2026-10-21/  los apuntes antiguos, por si acaso
│   └── src/                         ejemplos EJECUTABLES con la teoría en comentarios
│       ├── t1_hilos/        H01 crear hilos · H02 join/sleep/interrupt
│       │                    H03 condición de carrera + synchronized · H04 colecciones concurrentes
│       ├── t2_concurrencia/ C01 productor/consumidor (wait/notify) · C02 pool de hilos (Executor, Callable, Future)
│       │                    C03 atómicos y ReentrantLock · C04 interbloqueo · C05 Semaphore y CountDownLatch
│       └── t3_locks/        L01 sin sincronizar · L02 synchronized · L03 ReentrantReadWriteLock
│                            L04 StampedLock (lectura optimista) · L05 comparativa de las 4
│
├── exposiciones/                    ← EXPOSICIONES Y ACTIVIDADES DE INVESTIGACIÓN
│   └── 01_locks_lectura_escritura/  GUION_5min · PREGUNTAS · PLAN_ESTUDIO · visuales/locks_visual.pdf
│
└── _backup_original/                copia de la estructura anterior (excluida en IntelliJ)
```

> **Las lambdas, las colecciones y los streams están en el proyecto AC** (`C:\DAM\Segundo\AC`).

## Cómo trabajar

1. **Abrir:** en IntelliJ, `File → Open…` y elige la carpeta `C:\DAM\Segundo\PSP`.
2. **Estudiar un tema:** en `teoria/temaX_*/`, lee `1_HISTORIA.md` con `visual.pdf` al lado, ejecuta las demos de `teoria/src/tX_*` (▶ verde junto al `main`) y comprueba con `3_PREGUNTAS.md`. `2_TECNICO.md` es para consultar cuando programes.
3. **Hacer el ejercicio:** abre `ejercicios/src/temaX_*` y busca los `TODO` (**View → Tool Windows → TODO**).
4. **Repasar:** `teoria/CHULETA.md` y `teoria/ERRORES_FRECUENTES.md`.

| Tema | Teoría | Teoría (código) | Ejercicio |
|---|---|---|---|
| 1. Hilos | `tema1_hilos/` | `t1_hilos/` | `tema1_hilos/Cafeteria` |
| 2. Concurrencia | `tema2_concurrencia/` | `t2_concurrencia/` | `tema2_concurrencia/` |
| 3. Locks de lectura y escritura | `tema3_locks/` | `t3_locks/` | `tema3_locks/` + exposición en `exposiciones/01_locks_lectura_escritura/` |

**Para un tema nuevo:** crea `ejercicios/src/temaN_<nombre>/`, `teoria/src/tN_<nombre>/` y `teoria/temaN_<nombre>/` (con 1_HISTORIA, 2_TECNICO, 3_PREGUNTAS y visual), y añade una fila a la tabla.

**JDK:** openjdk-25 · **Codificación:** UTF-8
