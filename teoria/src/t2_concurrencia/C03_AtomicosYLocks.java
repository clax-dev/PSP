package t2_concurrencia;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * =====================================================================
 *  TEORÍA 2.3 — ALTERNATIVAS A synchronized: variables atómicas y ReentrantLock
 *  (En el tema 1, H03, viste el contador que perdía incrementos.)
 * =====================================================================
 *
 *  1) VARIABLES ATÓMICAS (java.util.concurrent.atomic)
 *     AtomicInteger, AtomicLong, AtomicBoolean...
 *     Sus operaciones (incrementAndGet, addAndGet...) son ATÓMICAS: se hacen "de un golpe",
 *     sin que otro hilo pueda meterse en medio. No necesitan synchronized.
 *     -> Ideales para CONTADORES compartidos.
 *
 *  2) ReentrantLock (java.util.concurrent.locks)
 *     Un cerrojo "manual": hace lo mismo que synchronized pero con más control.
 *
 *         lock.lock();
 *         try {
 *             // sección crítica
 *         } finally {
 *             lock.unlock();      // ⚠ SIEMPRE en finally: si hay una excepción, se libera igual
 *         }
 *
 *     Ventajas frente a synchronized:
 *       - tryLock(): intenta coger el cerrojo SIN quedarse bloqueado para siempre
 *       - tryLock(tiempo, unidad): espera como máximo ese tiempo (útil contra interbloqueos, ver C04)
 *       - new ReentrantLock(true): "justo" (fair) -> los hilos entran por orden de llegada
 *
 *  ¿CUÁL USO?
 *    - Un contador o un valor simple           -> Atomic...
 *    - Un bloque de código normal              -> synchronized (lo más sencillo)
 *    - Necesito tryLock / tiempo máximo / fair -> ReentrantLock
 */
public class C03_AtomicosYLocks {

    static int contadorNormal = 0;
    static AtomicInteger contadorAtomico = new AtomicInteger(0);

    static int cafesServidos = 0;
    static final ReentrantLock CAFETERA = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        final int HILOS = 4, VUELTAS = 100_000;

        // ==============================================================
        // 1) int normal vs AtomicInteger
        // ==============================================================
        Thread[] hilos = new Thread[HILOS];
        for (int i = 0; i < HILOS; i++) {
            hilos[i] = new Thread(() -> {
                for (int j = 0; j < VUELTAS; j++) {
                    contadorNormal++;                     // ❌ no atómico
                    contadorAtomico.incrementAndGet();    // ✔ atómico
                }
            });
            hilos[i].start();
        }
        for (Thread t : hilos) t.join();
        System.out.println("Esperado:      " + HILOS * VUELTAS);
        System.out.println("int normal:    " + contadorNormal + "   <- pierde incrementos");
        System.out.println("AtomicInteger: " + contadorAtomico.get());

        // ==============================================================
        // 2) ReentrantLock con lock / unlock en finally
        // ==============================================================
        System.out.println("\n--- ReentrantLock ---");
        Runnable servirCafes = () -> {
            for (int i = 0; i < 1000; i++) {
                CAFETERA.lock();
                try {
                    cafesServidos++;                      // sección crítica
                } finally {
                    CAFETERA.unlock();                    // SIEMPRE se libera
                }
            }
        };
        Thread a = new Thread(servirCafes), b = new Thread(servirCafes);
        a.start(); b.start(); a.join(); b.join();
        System.out.println("Cafés servidos (esperado 2000): " + cafesServidos);

        // ==============================================================
        // 3) tryLock: si la cafetera está ocupada, no me quedo esperando para siempre
        // ==============================================================
        System.out.println("\n--- tryLock ---");
        Thread ocupaCafetera = new Thread(() -> {
            CAFETERA.lock();
            try {
                System.out.println("Camarero-1 usa la cafetera durante 500 ms");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                CAFETERA.unlock();
            }
        });
        ocupaCafetera.start();
        Thread.sleep(50);                                 // aseguramos que Camarero-1 la coge antes

        if (CAFETERA.tryLock(100, TimeUnit.MILLISECONDS)) {
            try {
                System.out.println("Camarero-2 consigue la cafetera");
            } finally {
                CAFETERA.unlock();
            }
        } else {
            System.out.println("Camarero-2: ocupada tras 100 ms, hago otra cosa en vez de bloquearme");
        }
        ocupaCafetera.join();
    }
}
