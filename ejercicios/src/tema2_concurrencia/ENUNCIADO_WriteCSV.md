# Ejercicio: WriteCSV con `Callable` (PSP, tema 2: concurrencia)

## Enunciado (tal como lo ha dado la profesora)

> Una clase `WriteCSV` que implementa `Callable`, que escriba `pacientes.csv`. Su método `call` devuelve un `File` y un `Path`, y el programa da información del fichero.

**Enunciado completo (subido después):**
> - Pool de **3 hilos**
> - **5 tareas** de generación de CSV (5 × `WriteCSV`)
> - `WriteCSV` genera el fichero **`pacientes_YYYYMMdd_HHmmss.csv`**
> - `WriteCSV` hace un `sout` con el **nombre del hilo actual** (`Thread.currentThread().getName()`)

### Lo que cambia respecto a lo que ya tenías

| Antes | Ahora |
|---|---|
| Un nombre fijo, `pacientes.csv` | Un nombre **con fecha y hora**: `pacientes_20261016_131502.csv` (formato `yyyyMMdd_HHmmss`, como el log de BioDAM y del laboratorio) |
| Una tarea, llamando a `call()` directamente | **5 tareas** mandadas a un **pool de 3 hilos** (`Executors.newFixedThreadPool(3)` + `submit`), como en la demo `C02` |
| — | Dentro de `call()`, imprimir qué hilo la está ejecutando |
| Un resultado | **5 resultados** (5 `Future`): el `main` los recoge todos con `get()` y muestra la información de cada fichero |

### ⚠️ La trampa del ejercicio: el nombre del fichero

Con 3 hilos trabajando a la vez, **varias tareas se ejecutan en el mismo segundo**, así que generarían **el mismo nombre** (`..._HHmmss.csv`). Y como `Files.write` sin opciones **sobrescribe**, unas tareas machacarían el fichero de otras: en vez de 5 ficheros, te saldrían 1 o 2.

Formas de evitarlo (pregunta a tu profesora cuál prefiere):
- Añadir **un número de tarea** al nombre, pasándoselo a cada `WriteCSV` por el **constructor** (`pacientes_..._1.csv`, `..._2.csv`…). Es la más segura.
- Añadir los **milisegundos** al patrón (`SSS`). Reduce el problema, pero no lo elimina del todo.
- Añadir el **nombre del hilo** al fichero. Ojo: con 3 hilos y 5 tareas, **algunos hilos repiten**, así que también podría chocar.

## Lo que pide, separado en piezas

| Pieza | Qué significa |
|---|---|
| **Clase `WriteCSV` que implementa `Callable`** | Una **tarea** que se puede mandar a un hilo y que **devuelve un resultado** cuando termina (a diferencia de `Runnable`, que no devuelve nada) |
| **Escribe `pacientes.csv`** | Dentro de su método `call()` crea un fichero de texto con pacientes, una línea por paciente, con los campos separados por `;` (como el CSV de BioDAM: `ID;EDAD;DIAGNOSTICO;ESTADO`) |
| **`call()` devuelve un `File` y un `Path`** | Lo que la tarea entrega al terminar: la referencia al fichero que ha creado |
| **Información del fichero** | Quien recibe el resultado muestra datos del fichero: ruta, nombre, tamaño, número de líneas… |
| **(Implícito)** Alguien tiene que **lanzar** la tarea | Un `main` (por ejemplo, en una clase `Main` o `LanzadorWriteCSV`) que la ejecuta y recoge el resultado |

## Cosas que tienes que decidir (o preguntar a la profesora)

1. **Un método solo puede devolver UNA cosa.** "Devuelve un `File` y un `Path`" se puede interpretar de tres formas:
   - `Callable<Path>`: devuelve el `Path`, y el `File` se saca de él con `ruta.toFile()`.
   - `Callable<File>`: devuelve el `File`, y el `Path` se saca con `fichero.toPath()`.
   - **Devolver los dos a la vez**: una "caja" con los dos dentro. En Java moderno, un `record` con dos campos, por ejemplo `record ResultadoCSV(File fichero, Path ruta) {}`, y entonces `Callable<ResultadoCSV>`.

   Pregúntale cuál quiere. Si no lo sabes, la tercera cumple el enunciado al pie de la letra.
2. **¿Cómo se lanza la tarea?** Con un pool de hilos (`ExecutorService` + `submit` + `Future.get()`), que es lo del tema 2, o directamente llamando a `call()` desde el `main`. Lo normal en PSP es el pool.
3. **¿Qué pacientes y qué columnas?** Si el enunciado no lo dice, invéntate 4 o 5, con una cabecera.
4. **¿Dónde se guarda?** Con una ruta relativa, se crea en el directorio de trabajo del proyecto (`C:\DAM\Segundo\PSP\`). Puedes meterlo en una carpeta, por ejemplo `salida/pacientes.csv` (créala antes con `Files.createDirectories`).
5. **¿Qué "información del fichero"?** Como mínimo: nombre, ruta absoluta, tamaño en bytes, número de líneas y si existe. También puedes mostrar la fecha de modificación y si se puede leer o escribir.

## Checklist

- [ ] Clase `WriteCSV` que implementa `Callable<...>` (con el tipo que hayas elegido en el punto 1)
- [ ] `call()` imprime el nombre del hilo actual (`Thread.currentThread().getName()`)
- [ ] `call()` genera el nombre `pacientes_yyyyMMdd_HHmmss.csv` (`LocalDateTime` + `DateTimeFormatter`), **sin que choquen** entre tareas (ver la trampa ⬆)
- [ ] `call()` escribe el CSV con cabecera + pacientes dentro de la carpeta
- [ ] `call()` devuelve el `File` y el `Path` (según el punto 1)
- [ ] `main`: pool de **3** hilos, **5** tareas con `submit`, los 5 `Future` guardados en una lista
- [ ] `main`: recoge los 5 resultados con `get()` y muestra la información de cada fichero
- [ ] `main`: `shutdown()` del pool al final
- [ ] Excepciones: `call()` puede lanzar `Exception` (`IOException` incluida); `get()` lanza `InterruptedException` y `ExecutionException`
- [ ] Comprobación: en la carpeta hay **5 ficheros** distintos, y en la consola aparecen **solo 3 nombres de hilo** distintos (`pool-1-thread-1`, `-2`, `-3`), repetidos

## Documentación que vas a usar

| Qué necesitas | Dónde está |
|---|---|
| `Callable`, `ExecutorService`, `submit`, `Future`, `get`, `shutdown` | **PSP**: `teoria/tema2_concurrencia/2_TECNICO.md` (apartados 3 y 4: plantilla completa), y la demo `teoria/src/t2_concurrencia/C02_PoolDeHilos.java` (tiene un `Callable` que devuelve un resultado) |
| Escribir un fichero de texto (`Files.write`, `Files.createDirectories`, opciones) | **AD**: `C:\DAM\Segundo\AC\teoria\apuntes\05_guia_librerias.md`, apartado 7 |
| `File` frente a `Path`, cómo pasar de uno a otro y cómo sacar la información de un fichero | **AD**: misma guía, apartado **7.4** |
| Devolver dos cosas a la vez (`record`) | **PSP**: `teoria/tema2_concurrencia/2_TECNICO.md`, apartado 3 |
