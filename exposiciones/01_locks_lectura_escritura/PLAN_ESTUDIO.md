# Plan de estudio: de hoy (martes 20) al viernes 23

> **Objetivo:** ser capaz de explicarlo **con tus palabras** en 5 minutos y aguantar preguntas. No hace falta memorizar: hace falta **entender** y **haberlo dicho en voz alta** varias veces.
> **Total:** unas 5-6 horas repartidas en 4 días.

---

## Martes 20 (1 h 30): entender la idea

1. **PDF `visuales/locks_visual.pdf`, páginas 1 a 3** (15 min). Solo mirar los dibujos. Pregúntate:
   - ¿Por qué leer a la vez no es peligroso?
   - ¿Qué es un dato "a medias"?
2. **`teoria/tema3_locks/1_HISTORIA.md`**, bloques 1-2 (antes "apuntes, parte 1") (20 min).
3. **Ejecuta las demos `L01` y `L02`** (15 min). Lee los comentarios de arriba y responde:
   - ¿Por qué salen lecturas incoherentes en L01?
   - ¿Por qué tarda tanto L02?
4. **Prueba de la abuela** (10 min): sin mirar nada, explica en voz alta (o grábate en el móvil) **la analogía de la biblioteca** y **qué es leer de forma optimista**. Si te atascas, vuelve a la página 3 o la 6 del PDF.
5. **`teoria/tema3_locks/2_TECNICO.md`, apartados 5-6** (30 min) y **ejecuta `L03`**. Fíjate en la línea de tiempo de la parte 2 y compárala con la página 5 del PDF: es la misma.

✅ **Hoy terminas sabiendo:** el problema, la analogía, y qué hacen `readLock()` y `writeLock()`.

## Miércoles 21 (1 h 30): la parte técnica

1. **Repaso relámpago** (5 min): explica de memoria la biblioteca.
2. **`2_TECNICO.md`, apartados 7-8** (20 min): fairness, starvation y la tabla `ReentrantLock` vs RW.
3. **`2_TECNICO.md`, apartados 9-10 + PDF, páginas 6-7** (30 min): `StampedLock`.
   - **Dibuja tú en un papel** el diagrama de flujo de la página 7, sin mirar. Si sabes dibujarlo, sabes explicarlo.
4. **Ejecuta `L04`** (15 min) y relaciona cada parte con una pregunta:
   - parte 1 → "¿bloquea a los escritores?"
   - parte 2 → "¿qué pasa si `validate` da `false`?"
   - parte 3 → `tryConvertToWriteLock`
   - parte 4 → "¿es reentrante?"
5. **`PREGUNTAS.md`, preguntas 1-10** (20 min): tapa las respuestas y contesta **en voz alta**. Marca las que fallas.

✅ **Hoy terminas sabiendo:** la receta de 5 pasos de la lectura optimista y responder a 7 de las 10 preguntas.

## Jueves 22 (2 h): tu demo y el ensayo

1. **Haz tu demo** `ejercicios/src/tema3_locks/` siguiendo `ENUNCIADO_CacheVulnerabilidades.md` (1 h).
   - Si se te atasca, **usa `L05_Comparativa` como modelo**, pero cámbialo a tu tema: tienes que poder explicar cada línea.
   - Si no te da tiempo, presentas `L05_Comparativa`, que es válida.
2. **Ejecuta `L05`** y compara tus tiempos con los de la página 9 del PDF (5 min).
3. **Ensayo con cronómetro** (30 min): lee `GUION_5min.md` una vez y después **exponlo 3 veces** con el PDF delante:
   - 1ª vez: mirando el guion, sin preocuparte por el tiempo.
   - 2ª vez: solo con las frases clave.
   - 3ª vez: sin guion y **cronometrado** (objetivo: entre 4:30 y 5:15).
4. **`PREGUNTAS.md`**: repite las que fallaste ayer y haz las extra, de la 11 a la 20 (25 min).

✅ **Hoy terminas sabiendo:** hacer la exposición entera en 5 minutos.

## Viernes 23 (20 min antes de clase)

1. Mira las **páginas 8 y 10 del PDF** (la comparativa y "¿cuál elijo?").
2. Di en voz alta las **7 frases clave** del guion.
3. Comprueba que **el PDF y la demo abren** en el ordenador de clase, o que tienes la captura.

---

## Técnicas que funcionan para esto

| Técnica | Cómo | Por qué |
|---|---|---|
| **Explicar en voz alta** | A ti mismo, a alguien de casa o grabándote | Si no sabes decirlo, no lo entiendes: así encuentras los huecos |
| **Dibujar de memoria** | Las páginas 5 y 7 del PDF en un papel | La profesora valora lo visual, y dibujar fija la idea |
| **Preguntas tapadas** | `PREGUNTAS.md` sin mirar la respuesta | Es exactamente lo que pasará en clase |
| **De alto a bajo** | Para cada concepto: analogía → método → demo | Es lo que pide el enunciado ("dos niveles") |
| **Modificar las demos** | Cambia números en `L05` (más escritores, lecturas sin `sleep`…) y predice el resultado **antes** de ejecutar | Así entiendes **por qué** sale lo que sale |

## Las 7 frases clave (si solo te quedas con esto)

1. Leer a la vez no es peligroso; lo peligroso es leer mientras alguien escribe.
2. Muchos pueden consultar a la vez, pero para reorganizar la sala tiene que estar vacía.
3. `readLock()` es compartido y `writeLock()` es exclusivo.
4. Con `readLock` no se puede pedir `writeLock`: te esperas a ti mismo.
5. La lectura optimista lee sin pedir permiso y luego valida; si `validate` da `false`, repite con `readLock()`.
6. `StampedLock` no es reentrante, y la lectura optimista no bloquea a los escritores.
7. Compensa con muchas lecturas lentas y pocas escrituras; si no, `synchronized` es más simple y a veces más rápido.
