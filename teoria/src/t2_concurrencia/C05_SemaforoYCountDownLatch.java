package t2_concurrencia;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * =====================================================================
 *  TEORÍA 2.5 — HERRAMIENTAS DE SINCRONIZACIÓN: Semaphore y CountDownLatch
 * =====================================================================
 *
 *  SEMAPHORE (semáforo) = un contador de PERMISOS.
 *     Sirve para limitar cuántos hilos usan un recurso A LA VEZ (no solo uno, como synchronized).
 *         Semaphore mesas = new Semaphore(2);   // hay 2 mesas libres
 *         mesas.acquire();   // coger un permiso (si no quedan, espera)
 *         ...
 *         mesas.release();   // devolverlo (⚠ en finally)
 *     Con 1 permiso se comporta como un cerrojo ("semáforo binario").
 *
 *  COUNTDOWNLATCH = una cuenta atrás.
 *     Uno o varios hilos ESPERAN hasta que otros hilos hagan llegar la cuenta a 0.
 *         CountDownLatch listos = new CountDownLatch(3);
 *         listos.countDown();   // "yo ya estoy" (resta 1)
 *         listos.await();       // espera hasta que la cuenta llegue a 0
 *     ⚠ Solo se usa UNA vez: no se puede reiniciar.
 *
 *  Diferencia con join(): join espera a que un hilo TERMINE;
 *  el latch espera a que ocurra un EVENTO (por ejemplo, "todos han llegado"), aunque los hilos sigan vivos.
 */
public class C05_SemaforoYCountDownLatch {

    public static void main(String[] args) throws InterruptedException {

        // ==============================================================
        // 1) CountDownLatch: la cafetería no abre hasta que llegan los 3 camareros
        // ==============================================================
        System.out.println("=== 1) CountDownLatch: abrir cuando estén todos ===");
        CountDownLatch camarerosListos = new CountDownLatch(3);
        for (int i = 1; i <= 3; i++) {
            final int id = i;
            new Thread(() -> {
                dormir((long) (Math.random() * 300));
                System.out.println("Camarero " + id + " ha llegado");
                camarerosListos.countDown();               // resta 1
            }).start();
        }
        camarerosListos.await();                            // main espera a que llegue a 0
        System.out.println("✔ Están todos: ¡abrimos la cafetería!");

        // ==============================================================
        // 2) Semaphore: 5 clientes, pero solo hay 2 mesas
        //    Nunca habrá más de 2 clientes sentados a la vez.
        // ==============================================================
        System.out.println("\n=== 2) Semaphore: 5 clientes, 2 mesas ===");
        Semaphore mesas = new Semaphore(2);
        Thread[] clientes = new Thread[5];
        for (int i = 0; i < clientes.length; i++) {
            final int id = i + 1;
            clientes[i] = new Thread(() -> {
                try {
                    mesas.acquire();                        // coger mesa (espera si no hay)
                    try {
                        System.out.println("Cliente " + id + " se sienta   (mesas libres: "
                                + mesas.availablePermits() + ")");
                        dormir(200);                        // está tomando algo
                        System.out.println("Cliente " + id + " se va");
                    } finally {
                        mesas.release();                    // deja la mesa libre
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            clientes[i].start();
        }
        for (Thread t : clientes) t.join();
        System.out.println("✔ Todos los clientes han sido atendidos");
    }

    static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
