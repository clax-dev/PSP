package t3_locks;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * =====================================================================
 *  TEORÍA 3.5 — COMPARATIVA de las 4 formas (la que pide la profesora)
 * =====================================================================
 *
 *  La MISMA carta con 4 protecciones distintas, y el MISMO trabajo:
 *
 *    1. Sin sincronización      -> rápido, pero con lecturas incoherentes ✘
 *    2. synchronized            -> correcto, pero los lectores hacen cola
 *    3. ReentrantReadWriteLock  -> correcto y los lectores entran juntos
 *    4. StampedLock optimista   -> correcto, los lectores ni siquiera cogen lock
 *
 *  ESCENARIO A: muchas lecturas LENTAS y pocas escrituras (para lo que se inventaron).
 *  ESCENARIO B: las MISMAS proporciones (8 lectores, 1 escritor) pero lecturas INSTANTÁNEAS.
 *               Aquí ReadWriteLock suele ir PEOR que synchronized: gestionar el contador de
 *               lectores cuesta más que la propia lectura (cuando NO compensa).
 *
 *  ⚠ Los tiempos cambian en cada ejecución y en cada ordenador. Fíjate en la PROPORCIÓN.
 */
public class L05_Comparativa {

    /** Lo que tienen en común las 4 versiones. */
    interface Carta {
        boolean leerEsCoherente(int msTrabajo);   // true si carta == pizarra
        void escribir(int nuevo, int msTrabajo);
    }

    // ---------------------------------------------------------------- 1
    static class CartaSinSync implements Carta {
        int carta = 100, pizarra = 100;
        public boolean leerEsCoherente(int ms) {
            int c = carta; trabajar(ms); int p = pizarra;
            return c == p;
        }
        public void escribir(int nuevo, int ms) {
            carta = nuevo; trabajar(ms); pizarra = nuevo;
        }
    }

    // ---------------------------------------------------------------- 2
    static class CartaSynchronized implements Carta {
        int carta = 100, pizarra = 100;
        public synchronized boolean leerEsCoherente(int ms) {
            int c = carta; trabajar(ms); int p = pizarra;
            return c == p;
        }
        public synchronized void escribir(int nuevo, int ms) {
            carta = nuevo; trabajar(ms); pizarra = nuevo;
        }
    }

    // ---------------------------------------------------------------- 3
    static class CartaReadWrite implements Carta {
        int carta = 100, pizarra = 100;
        final ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
        public boolean leerEsCoherente(int ms) {
            rw.readLock().lock();
            try {
                int c = carta; trabajar(ms); int p = pizarra;
                return c == p;
            } finally {
                rw.readLock().unlock();
            }
        }
        public void escribir(int nuevo, int ms) {
            rw.writeLock().lock();
            try {
                carta = nuevo; trabajar(ms); pizarra = nuevo;
            } finally {
                rw.writeLock().unlock();
            }
        }
    }

    // ---------------------------------------------------------------- 4
    static class CartaStamped implements Carta {
        int carta = 100, pizarra = 100;
        final StampedLock sl = new StampedLock();
        final AtomicInteger fallos = new AtomicInteger();
        public boolean leerEsCoherente(int ms) {
            long s = sl.tryOptimisticRead();
            int c = carta; trabajar(ms); int p = pizarra;
            if (!sl.validate(s)) {
                fallos.incrementAndGet();
                s = sl.readLock();
                try {
                    c = carta; trabajar(ms); p = pizarra;
                } finally {
                    sl.unlockRead(s);
                }
            }
            return c == p;
        }
        public void escribir(int nuevo, int ms) {
            long s = sl.writeLock();
            try {
                carta = nuevo; trabajar(ms); pizarra = nuevo;
            } finally {
                sl.unlockWrite(s);
            }
        }
    }

    // ==================================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("ESCENARIO A: 4 lectores × 50 lecturas de 10 ms · 1 escritor × 5 escrituras");
        System.out.println("-----------------------------------------------------------------------------");
        cabecera();
        for (Carta c : nuevas()) medir(c, 4, 50, 10, 1, 5, 1, 50);

        System.out.println("\nESCENARIO B: 8 lectores × 500.000 lecturas INSTANTÁNEAS · 1 escritor × 500.000");
        System.out.println("-----------------------------------------------------------------------------");
        cabecera();
        for (int calentamiento = 0; calentamiento < 2; calentamiento++) {   // la JVM "calienta"
            for (Carta c : nuevas()) medirSilencioso(c);
        }
        for (Carta c : nuevas()) medir(c, 8, 500_000, 0, 1, 500_000, 0, 0);
    }

    static Carta[] nuevas() {
        return new Carta[]{new CartaSinSync(), new CartaSynchronized(), new CartaReadWrite(), new CartaStamped()};
    }

    static void cabecera() {
        System.out.printf("%-24s %10s %14s %16s%n", "Versión", "Tiempo", "Incoherentes", "Fallos optimistas");
    }

    static void medirSilencioso(Carta carta) throws InterruptedException {
        ejecutar(carta, 8, 500_000, 0, 1, 500_000, 0, 0, new AtomicInteger());
    }

    static void medir(Carta carta, int lectores, int lecturas, int msLectura,
                      int escritores, int escrituras, int msEscritura, int pausaEscritor)
            throws InterruptedException {
        AtomicInteger incoherentes = new AtomicInteger();
        long ms = ejecutar(carta, lectores, lecturas, msLectura,
                escritores, escrituras, msEscritura, pausaEscritor, incoherentes);
        String fallos = carta instanceof CartaStamped st ? String.valueOf(st.fallos.get()) : "-";
        System.out.printf("%-24s %7d ms %14d %16s%n",
                carta.getClass().getSimpleName(), ms, incoherentes.get(), fallos);
    }

    static long ejecutar(Carta carta, int lectores, int lecturas, int msLectura,
                         int escritores, int escrituras, int msEscritura, int pausaEscritor,
                         AtomicInteger incoherentes) throws InterruptedException {
        Thread[] hilos = new Thread[lectores + escritores];
        for (int i = 0; i < lectores; i++) {
            hilos[i] = new Thread(() -> {
                for (int j = 0; j < lecturas; j++) {
                    if (!carta.leerEsCoherente(msLectura)) incoherentes.incrementAndGet();
                }
            });
        }
        for (int i = 0; i < escritores; i++) {
            hilos[lectores + i] = new Thread(() -> {
                for (int j = 0; j < escrituras; j++) {
                    carta.escribir(j, msEscritura);
                    trabajar(pausaEscritor);
                }
            });
        }
        long inicio = System.nanoTime();
        for (Thread h : hilos) h.start();
        for (Thread h : hilos) h.join();
        return (System.nanoTime() - inicio) / 1_000_000;
    }

    static void trabajar(int ms) {
        if (ms > 0) L01_SinSincronizacion.dormir(ms);
    }
}
