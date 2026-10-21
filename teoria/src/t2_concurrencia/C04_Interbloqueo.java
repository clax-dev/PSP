package t2_concurrencia;

/**
 * =====================================================================
 *  TEORÍA 2.4 — INTERBLOQUEO (DEADLOCK)
 * =====================================================================
 *
 *  INTERBLOQUEO: dos (o más) hilos se quedan esperando PARA SIEMPRE,
 *  cada uno por un recurso que tiene el otro.
 *
 *     Camarero-A tiene la CAFETERA y espera el MOLINILLO
 *     Camarero-B tiene el MOLINILLO y espera la CAFETERA
 *     -> ninguno suelta lo suyo -> el programa se congela (sin error, sin mensaje)
 *
 *  CONDICIONES DE COFFMAN (tienen que darse LAS 4 a la vez):
 *    1. Exclusión mutua      -> el recurso solo lo puede usar uno (synchronized)
 *    2. Retención y espera   -> tengo uno y espero otro sin soltar el primero
 *    3. No expropiación      -> nadie me puede quitar el recurso a la fuerza
 *    4. Espera circular      -> A espera a B y B espera a A
 *
 *  CÓMO EVITARLO (romper una de las 4 condiciones):
 *    ✔ Mismo ORDEN para todos: todos cogen primero la cafetera y luego el molinillo
 *      (rompe la espera circular). Es la solución más sencilla.
 *    ✔ tryLock con tiempo máximo (ver C03): si no consigo el segundo, suelto el primero.
 *    ✔ Coger los recursos del tirón con un único cerrojo.
 *
 *  ▶ Esta demo PROVOCA un interbloqueo a propósito, lo detecta y después muestra la solución.
 *    Los hilos bloqueados son "daemon", así que no impiden que el programa termine.
 */
public class C04_Interbloqueo {

    static final Object CAFETERA = new Object();
    static final Object MOLINILLO = new Object();

    public static void main(String[] args) throws InterruptedException {

        // ==============================================================
        // 1) PROVOCAR el interbloqueo: orden contrario
        // ==============================================================
        System.out.println("=== 1) Orden contrario -> interbloqueo ===");
        Thread a = new Thread(() -> usar("Camarero-A", CAFETERA, "la cafetera", MOLINILLO, "el molinillo"));
        Thread b = new Thread(() -> usar("Camarero-B", MOLINILLO, "el molinillo", CAFETERA, "la cafetera"));
        a.setDaemon(true);
        b.setDaemon(true);
        a.start(); b.start();

        a.join(1000);   // esperamos como mucho 1 s
        b.join(1000);
        if (a.isAlive() && b.isAlive()) {
            System.out.println("⚠ INTERBLOQUEO: tras 1 s siguen esperando. Estado A=" + a.getState()
                    + ", B=" + b.getState());
        }

        // ==============================================================
        // 2) SOLUCIÓN: los dos cogen los recursos en el MISMO orden
        //    (usamos otros dos objetos porque los de arriba siguen bloqueados)
        // ==============================================================
        System.out.println("\n=== 2) Mismo orden -> sin interbloqueo ===");
        Object cafetera2 = new Object(), molinillo2 = new Object();
        Thread c = new Thread(() -> usar("Camarero-C", cafetera2, "la cafetera", molinillo2, "el molinillo"));
        Thread d = new Thread(() -> usar("Camarero-D", cafetera2, "la cafetera", molinillo2, "el molinillo"));
        c.start(); d.start();
        c.join(); d.join();
        System.out.println("✔ Los dos han preparado su café.");
    }

    // Coge el primer recurso, espera un poco y luego intenta el segundo.
    static void usar(String quien, Object r1, String n1, Object r2, String n2) {
        synchronized (r1) {
            System.out.println(quien + " coge " + n1);
            dormir(100);                      // da tiempo al otro a coger su primer recurso
            System.out.println(quien + " espera " + n2 + "...");
            synchronized (r2) {
                System.out.println(quien + " coge " + n2 + " y prepara el café");
            }
        }
    }

    static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
