package t3_locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * =====================================================================
 *  TEORÍA 3.3 — SOLUCIÓN 2: ReentrantReadWriteLock (dos llaves)
 * =====================================================================
 *
 *  ReadWriteLock es una INTERFAZ con dos cerrojos que trabajan en pareja:
 *
 *      ReadWriteLock rw = new ReentrantReadWriteLock();   // la implementación
 *      Lock lectura   = rw.readLock();    // COMPARTIDO: muchos lectores a la vez
 *      Lock escritura = rw.writeLock();   // EXCLUSIVO:  un escritor y NADIE más
 *
 *  | Quién está dentro    | ¿Puede entrar un lector? | ¿Puede entrar un escritor? |
 *  |----------------------|--------------------------|----------------------------|
 *  | Nadie                | Sí                       | Sí                         |
 *  | Uno o más lectores   | Sí                       | NO (espera a que salgan)   |
 *  | Un escritor          | NO                       | NO                         |
 *
 *  Se usan igual que ReentrantLock: lock() + try + unlock() en finally.
 *
 *  FAIRNESS (justicia):
 *    new ReentrantReadWriteLock()      -> no justo (por defecto): más rendimiento,
 *                                         sin orden garantizado de llegada.
 *    new ReentrantReadWriteLock(true)  -> justo: entran por orden de llegada.
 *                                         Evita STARVATION (que alguien espere para siempre),
 *                                         pero es más lento.
 *
 *  REENTRANTE: el mismo hilo puede coger el MISMO lock varias veces (y debe soltarlo
 *  las mismas veces). Pero OJO: NO se puede SUBIR de lectura a escritura (parte 3).
 *
 *  Este archivo tiene 3 partes:
 *    1) Varios lectores dentro A LA VEZ (y el tiempo baja frente a L02).
 *    2) Un escritor llega mientras hay lectores: espera. Y los nuevos lectores, detrás de él.
 *    3) La trampa: tener el readLock y pedir el writeLock = el hilo se bloquea para siempre.
 */
public class L03_ReadWriteLock {

    static int precioCarta = 100;
    static int precioPizarra = 100;

    static final ReadWriteLock RW = new ReentrantReadWriteLock();   // prueba con (true)
    static final Lock LECTURA = RW.readLock();
    static final Lock ESCRITURA = RW.writeLock();

    static final long T0 = System.currentTimeMillis();

    static void log(String msg) {
        System.out.printf("[%4d ms] %-10s %s%n", System.currentTimeMillis() - T0,
                Thread.currentThread().getName(), msg);
    }

    public static void main(String[] args) throws InterruptedException {
        parte1_lectoresALaVez();
        parte2_escritorEspera();
        parte3_noSePuedeSubir();
    }

    // ==================================================================
    // 1) Muchos lectores dentro a la vez
    // ==================================================================
    static void parte1_lectoresALaVez() throws InterruptedException {
        System.out.println("=== 1) Lectores simultáneos ===");
        AtomicInteger dentro = new AtomicInteger();
        AtomicInteger maximo = new AtomicInteger();

        Runnable cliente = () -> {
            for (int j = 0; j < 50; j++) {
                LECTURA.lock();
                try {
                    maximo.accumulateAndGet(dentro.incrementAndGet(), Math::max);
                    int carta = precioCarta;
                    L01_SinSincronizacion.dormir(10);        // la misma lectura lenta que en L02
                    int pizarra = precioPizarra;
                    if (carta != pizarra) System.out.println("¡Incoherente!");
                    dentro.decrementAndGet();
                } finally {
                    LECTURA.unlock();
                }
            }
        };

        Thread[] clientes = new Thread[4];
        long inicio = System.currentTimeMillis();
        for (int i = 0; i < clientes.length; i++) {
            clientes[i] = new Thread(cliente, "Cliente-" + (i + 1));
            clientes[i].start();
        }
        for (Thread c : clientes) c.join();

        System.out.println("Máximo de lectores dentro a la vez: " + maximo.get());
        System.out.println("Tiempo: " + (System.currentTimeMillis() - inicio)
                + " ms   (con synchronized, L02, eran ~2000 ms)\n");
    }

    // ==================================================================
    // 2) Un escritor llega mientras hay lectores
    // ==================================================================
    static void parte2_escritorEspera() throws InterruptedException {
        System.out.println("=== 2) El escritor espera a que salgan los lectores ===");

        Runnable lectorLento = () -> {
            log("quiere leer");
            LECTURA.lock();
            try {
                log("LEYENDO");
                L01_SinSincronizacion.dormir(300);
            } finally {
                log("deja de leer");
                LECTURA.unlock();
            }
        };

        Runnable escritor = () -> {
            log("quiere ESCRIBIR -> tiene que esperar a que no quede ningún lector");
            ESCRITURA.lock();
            try {
                log("ESCRIBIENDO (solo)");
                precioCarta = 120;
                L01_SinSincronizacion.dormir(100);
                precioPizarra = 120;
            } finally {
                log("termina de escribir");
                ESCRITURA.unlock();
            }
        };

        Thread l1 = new Thread(lectorLento, "Lector-1");
        Thread l2 = new Thread(lectorLento, "Lector-2");
        Thread w  = new Thread(escritor, "Escritor");
        Thread l3 = new Thread(lectorLento, "Lector-3");   // llega DESPUÉS que el escritor

        l1.start(); l2.start();
        L01_SinSincronizacion.dormir(50);
        w.start();
        L01_SinSincronizacion.dormir(50);
        l3.start();   // aunque hay lectores dentro, NO se cuela: el escritor ya estaba esperando
        l1.join(); l2.join(); w.join(); l3.join();
        System.out.println();
    }

    // ==================================================================
    // 3) No se puede subir de readLock a writeLock
    // ==================================================================
    static void parte3_noSePuedeSubir() throws InterruptedException {
        System.out.println("=== 3) Trampa: de lectura a escritura NO se puede subir ===");

        LECTURA.lock();
        try {
            System.out.println("Tengo el readLock. Ahora pido el writeLock...");
            // Con ESCRITURA.lock() este hilo se quedaría bloqueado PARA SIEMPRE:
            // el writeLock espera a que no haya lectores... y el lector soy yo.
            // Uso tryLock con tiempo máximo para que la demo no se cuelgue:
            boolean conseguido = ESCRITURA.tryLock(1, TimeUnit.SECONDS);
            System.out.println("¿Conseguido? " + conseguido + "   <- me esperaría a mí mismo");
        } finally {
            LECTURA.unlock();
        }

        // Lo correcto: soltar la lectura y pedir la escritura.
        // OJO: entre medias otro hilo puede haber cambiado el dato -> vuelve a comprobarlo.
        ESCRITURA.lock();
        try {
            System.out.println("Solté la lectura y pedí la escritura: conseguido ✔");
            // BAJAR sí se puede (downgrade): con el writeLock cogido, pido el readLock
            LECTURA.lock();
            System.out.println("Con el writeLock cogido, pido el readLock: conseguido ✔ (bajar SÍ se puede)");
        } finally {
            ESCRITURA.unlock();       // ahora solo me queda la lectura
        }
        LECTURA.unlock();
    }
}
