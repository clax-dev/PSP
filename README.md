# PSP: Programación de Servicios y Procesos (DAM 2º)

Proyecto de IntelliJ con **dos módulos**: ejercicios y teoría.

```
PSP/
├── ejercicios/                      ← TUS EJERCICIOS
│   └── src/
│       └── tema1_hilos/         Cafeteria.java
│
├── teoria/                          ← TEORÍA (basada en los ejercicios)
│   ├── apuntes/                     apuntes en Markdown
│   │   ├── 00_chuleta.md            ★ resumen para repasar rápido
│   │   ├── 01_hilos.md
│   │   └── 02_errores_frecuentes.md ★ fallos encontrados en tus ejercicios
│   └── src/                         ejemplos EJECUTABLES con la teoría en comentarios
│       └── t1_hilos/        H01 crear hilos · H02 join/sleep/interrupt
│                            H03 condición de carrera + synchronized · H04 colecciones concurrentes
│
└── _backup_original/                copia de la estructura anterior (excluida en IntelliJ)
```

> **Las lambdas, las colecciones y los streams están en el proyecto AC** (`C:\DAM\Segundo\AC`).

## Cómo trabajar

1. **Abrir:** en IntelliJ, `File → Open…` y elige la carpeta `C:\DAM\Segundo\PSP`.
2. **Estudiar un tema:** lee `teoria/apuntes/0X_*.md` (el botón de vista previa está arriba a la derecha) y ejecuta los ejemplos de `teoria/src/tX_*` con el ▶ verde que aparece junto al `main`.
3. **Hacer el ejercicio:** abre `ejercicios/src/temaX_*` y busca los `TODO` (**View → Tool Windows → TODO**).
4. **Repasar:** `00_chuleta.md` y `02_errores_frecuentes.md`.

| Tema | Teoría (apuntes) | Teoría (código) | Ejercicio |
|---|---|---|---|
| 1. Hilos | `01_hilos.md` | `t1_hilos/` | `tema1_hilos/Cafeteria` |

**Para un tema nuevo:** crea `ejercicios/src/tema2_<nombre>/`, `teoria/src/t2_<nombre>/` y `teoria/apuntes/0X_<nombre>.md`, y añade una fila a la tabla.

**JDK:** openjdk-25 · **Codificación:** UTF-8
