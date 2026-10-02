package t1_hilos;

/**
 * =====================================================================
 *  TEORÍA 3.2 — CONTROLAR HILOS: join, sleep, interrupt
 *  Basado en: abrirCafeteria() y el while de Camarero.run() en Cafeteria.java
 * =====================================================================
 *
 *  | Método                  | Qué hace                                                 |
 *  |-------------------------|----------------------------------------------------------|
 *  | t.start()               | arranca el hilo t                                        |
 *  | t.join()                | el hilo que lo llama ESPERA a que t termine              |
 *  | t.join(ms)              | espera como máximo ms milisegundos                       |
 *  | Thread.sleep(ms)        | DUERME el hilo ACTUAL ms milisegundos                    |
 *  | t.interrupt()           | PIDE a t que pare (no lo mata: t debe comprobarlo)       |
 *  | isInterrupted()         | ¿me han pedido que pare?                                 |
 *  | Thread.currentThread()  | el hilo que está ejecutando esta línea                   |
 *  | t.isAlive()             | ¿sigue vivo?                                             |
 *
 *  join() y sleep() lanzan InterruptedException (comprobada) -> try/catch o throws.
 */
public class H02_JoinSleepInterrupt {

    public static void main(String[] args) throws InterruptedException {

        // ==============================================================
        // 1) SIN join: main sigue a lo suyo y puede terminar antes
        // ==============================================================
        System.out.println("=== 1) Sin join ===");
        Thread lento = new Thread(() -> {
            dormir(300);
            System.out.println("   (hilo lento) termino");
        });
        lento.start();
        System.out.println("main: no he esperado, el hilo sigue vivo? " + lento.isAlive());
        lento.join(); // (lo esperamos aquí solo para no mezclar con el siguiente apartado)

        // ==============================================================
        // 2) CON join: main espera a TODOS (patrón de abrirCafeteria)
        // ==============================================================
        System.out.println("\n=== 2) Con join ===");
        Thread[] camareros = new Thread[3];
        for (int i = 0; i < camareros.length; i++) {
            final int id = i + 1;
            camareros[i] = new Thread(() -> {
                dormir((long) (Math.random() * 300));
                System.out.println("   Camarero " + id + " ha terminado");
            });
            camareros[i].start();
        }
        try {
            for (Thread t : camareros) {
                t.join();                // main se bloquea hasta que t acabe
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("main: todos los camareros han terminado ✔");

        // ==============================================================
        // 3) INTERRUPT: pedir a un hilo que pare
        //    El hilo debe comprobar isInterrupted() (como el while de Camarero)
        // ==============================================================
        System.out.println("\n=== 3) interrupt ===");
        Thread trabajador = new Thread(() -> {
            int vuelta = 0;
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("   trabajando... vuelta " + (++vuelta));
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ⚠ Al lanzarse la excepción, el "flag" de interrupción se BORRA.
                    //   Lo volvemos a marcar para que el while lo vea y salga.
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("   me han interrumpido, salgo limpiamente");
        });
        trabajador.start();
        Thread.sleep(350);
        trabajador.interrupt();          // petición de parada
        trabajador.join();
        System.out.println("main: fin");
    }

    // Helper para no repetir try/catch en cada sleep
    static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
