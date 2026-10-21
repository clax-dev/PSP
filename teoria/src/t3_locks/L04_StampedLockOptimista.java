package t3_locks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;

/**
 * =====================================================================
 *  TEORÍA 3.4 — SOLUCIÓN 3: StampedLock y la LECTURA OPTIMISTA
 * =====================================================================
 *
 *  StampedLock tiene TRES modos, y cada vez que coges uno te da un STAMP
 *  (un número, un "sello"/"ticket" de tipo long):
 *
 *      long s = sl.writeLock();          ... sl.unlockWrite(s);   // exclusivo
 *      long s = sl.readLock();           ... sl.unlockRead(s);    // compartido (como readLock de RW)
 *      long s = sl.tryOptimisticRead();  ... sl.validate(s)       // ¡NO bloquea nada!
 *
 *  LECTURA OPTIMISTA = "leo sin pedir permiso y DESPUÉS compruebo si alguien escribió".
 *    1. s = tryOptimisticRead()     -> apunto la "versión" actual (no cojo ningún lock)
 *    2. copio los datos a variables locales
 *    3. validate(s)                 -> ¿ha habido alguna escritura desde que apunté?
 *         true  -> lo que copié es bueno, lo uso.
 *         false -> lo que copié puede ser BASURA: lo tiro y vuelvo a leer con readLock().
 *
 *  Analogía: haces una foto a la pizarra sin pedirle nada al camarero. Antes de usarla,
 *  miras el "número de versión" de la pizarra: si no ha cambiado, tu foto vale.
 *
 *  CLAVES (preguntas de examen):
 *    - La lectura optimista NO bloquea a los escritores. El escritor entra cuando quiere.
 *    - Por eso HAY que validar. Sin validate() podrías usar datos a medias.
 *    - Solo se usan los datos DESPUÉS de validar (nunca dentro de un if antes).
 *    - StampedLock NO es reentrante: si ya tienes el writeLock y lo pides otra vez, te bloqueas.
 *    - No tiene Condition (no hay await/signal) y no hay modo "justo".
 *    - tryConvertToWriteLock(s): intenta SUBIR a escritura (lo que RW no permitía).
 *      Devuelve el nuevo stamp, o 0 si no ha podido.
 *    - Un stamp 0 significa "no conseguido".
 *
 *  Este archivo tiene 4 partes:
 *    1) Paso a paso, en un solo hilo: validate() true y validate() false.
 *    2) Con hilos: cuántas veces falla la lectura optimista y por qué hay que validar.
 *    3) tryConvertToWriteLock: subir de lectura a escritura.
 *    4) No es reentrante.
 */
public class L04_StampedLockOptimista {

    static int precioCarta = 100;
    static int precioPizarra = 100;
    static final StampedLock SL = new StampedLock();

    // ---------------- La receta de la lectura optimista ----------------
    static final AtomicInteger FALLOS_OPTIMISTAS = new AtomicInteger();
    static final AtomicInteger BASURA_DETECTADA = new AtomicInteger();

    static int[] leer() {
        long stamp = SL.tryOptimisticRead();        // 1. apunto la versión (no bloqueo a nadie)
        int carta = precioCarta;                     // 2. copio a variables locales
        L01_SinSincronizacion.dormir(10);
        int pizarra = precioPizarra;

        if (!SL.validate(stamp)) {                   // 3. ¿alguien escribió mientras?
            if (carta != pizarra) BASURA_DETECTADA.incrementAndGet();   // (solo para la demo)
            FALLOS_OPTIMISTAS.incrementAndGet();
            stamp = SL.readLock();                   // 4. plan B: lectura "pesimista"
            try {
                carta = precioCarta;
                pizarra = precioPizarra;
            } finally {
                SL.unlockRead(stamp);
            }
        }
        return new int[]{carta, pizarra};            // 5. ahora sí, uso los datos
    }

    static void escribir(int nuevo) {
        long stamp = SL.writeLock();
        try {
            precioCarta = nuevo;
            L01_SinSincronizacion.dormir(1);
            precioPizarra = nuevo;
        } finally {
            SL.unlockWrite(stamp);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        parte1_pasoAPaso();
        parte2_conHilos();
        parte3_convertir();
        parte4_noReentrante();
    }

    // ==================================================================
    // 1) Paso a paso en un solo hilo
    // ==================================================================
    static void parte1_pasoAPaso() {
        System.out.println("=== 1) Paso a paso ===");

        long s1 = SL.tryOptimisticRead();
        System.out.println("Stamp optimista: " + s1);
        int carta = precioCarta;
        System.out.println("Leo la carta: " + carta + "   (nadie escribe)");
        System.out.println("validate(" + s1 + ") = " + SL.validate(s1) + "   -> el dato vale ✔");

        long s2 = SL.tryOptimisticRead();
        System.out.println("\nStamp optimista: " + s2);
        carta = precioCarta;
        System.out.println("Leo la carta: " + carta);
        System.out.println("...y mientras, alguien escribe (fíjate: el escritor NO ha tenido que esperarme):");
        escribir(130);
        System.out.println("validate(" + s2 + ") = " + SL.validate(s2)
                + "   -> mi " + carta + " está desfasado (ahora vale " + precioCarta + "): repito con readLock()\n");
    }

    // ==================================================================
    // 2) Con hilos
    // ==================================================================
    static void parte2_conHilos() throws InterruptedException {
        System.out.println("=== 2) Muchos lectores optimistas + un escritor ===");
        AtomicInteger incoherentesUsadas = new AtomicInteger();
        AtomicInteger lecturas = new AtomicInteger();

        Thread camarero = new Thread(() -> {
            for (int nuevo = 131; nuevo <= 140; nuevo++) {
                escribir(nuevo);
                L01_SinSincronizacion.dormir(50);
            }
        });
        Thread[] clientes = new Thread[4];
        for (int i = 0; i < clientes.length; i++) {
            clientes[i] = new Thread(() -> {
                for (int j = 0; j < 50; j++) {
                    int[] p = leer();
                    lecturas.incrementAndGet();
                    if (p[0] != p[1]) incoherentesUsadas.incrementAndGet();
                }
            });
        }

        long inicio = System.currentTimeMillis();
        camarero.start();
        for (Thread c : clientes) c.start();
        camarero.join();
        for (Thread c : clientes) c.join();

        System.out.println("Lecturas:                         " + lecturas.get());
        System.out.println("validate() == false (plan B):     " + FALLOS_OPTIMISTAS.get());
        System.out.println("  ...de esas, copias A MEDIAS:    " + BASURA_DETECTADA.get()
                + "   <- por esto hay que validar");
        System.out.println("Datos incoherentes USADOS:        " + incoherentesUsadas.get() + "   ✔");
        System.out.println("Tiempo: " + (System.currentTimeMillis() - inicio) + " ms\n");
    }

    // ==================================================================
    // 3) tryConvertToWriteLock: "leo y, si hace falta, escribo"
    // ==================================================================
    static void parte3_convertir() {
        System.out.println("=== 3) tryConvertToWriteLock ===");
        // Regla del negocio: si el café cuesta menos de 150, se sube a 150.
        long stamp = SL.readLock();
        try {
            while (precioCarta < 150) {
                long ws = SL.tryConvertToWriteLock(stamp);
                if (ws != 0L) {                      // ✔ conversión conseguida
                    stamp = ws;
                    System.out.println("Convertido a escritura (stamp " + ws + "), subo el precio");
                    precioCarta = 150;
                    precioPizarra = 150;
                } else {                             // ✘ no se pudo (había otros lectores)
                    SL.unlockRead(stamp);
                    stamp = SL.writeLock();          // lo pido "a la antigua" y vuelvo a comprobar
                }
            }
        } finally {
            SL.unlock(stamp);                        // unlock() vale para cualquier modo
        }
        System.out.println("Precio final: " + precioCarta + "\n");
    }

    // ==================================================================
    // 4) No es reentrante
    // ==================================================================
    static void parte4_noReentrante() {
        System.out.println("=== 4) StampedLock NO es reentrante ===");
        long s = SL.writeLock();
        try {
            // Con SL.writeLock() aquí, el hilo se bloquearía para siempre (se espera a sí mismo).
            long otra = SL.tryWriteLock();           // la versión "sin esperar"
            System.out.println("Ya tengo el writeLock y lo pido otra vez -> stamp = " + otra
                    + "   (0 = no conseguido)");
        } finally {
            SL.unlockWrite(s);
        }
    }
}
