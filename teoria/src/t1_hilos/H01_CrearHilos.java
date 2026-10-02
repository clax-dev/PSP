package t1_hilos;

/**
 * =====================================================================
 *  TEORÍA 3.1 — QUÉ ES UN HILO Y CÓMO SE CREA
 *  Basado en: ejercicios/tema1_hilos/Cafeteria.java
 * =====================================================================
 *
 *  PROCESO: programa en ejecución, con su propia memoria.
 *  HILO (thread): una línea de ejecución DENTRO de un proceso.
 *     - Los hilos de un mismo proceso COMPARTEN memoria (variables static, objetos...).
 *     - Esto es útil (comunicarse es fácil) y peligroso (condiciones de carrera, ver H03).
 *  CONCURRENCIA: varias tareas avanzan "a la vez".
 *     ⚠ El ORDEN de ejecución lo decide el planificador del S.O.: NO está garantizado.
 *
 *  El programa ya tiene un hilo: "main". Desde él creamos otros.
 *
 *  TRES FORMAS DE CREAR UN HILO:
 *    1. implements Runnable   (RECOMENDADA — la de Cafeteria)
 *    2. extends Thread        (peor: Java solo permite heredar de UNA clase)
 *    3. lambda                (Runnable es una interfaz funcional -> une tema 1 y tema 3)
 *
 *  CICLO DE VIDA:
 *     NEW --start()--> RUNNABLE <--> BLOCKED / WAITING / TIMED_WAITING --fin run()--> TERMINATED
 *
 *  ▶ Ejecuta varias veces: el orden de los mensajes cambia.
 */
public class H01_CrearHilos {

    // ---------------------------------------------------------------
    // FORMA 1: implementar Runnable (como Camarero en Cafeteria)
    // ---------------------------------------------------------------
    static class Camarero implements Runnable {
        private final int id;

        Camarero(int id) { this.id = id; }

        @Override
        public void run() {          // el "trabajo" que hará el hilo
            for (int i = 1; i <= 3; i++) {
                System.out.println("Camarero " + id + " atiende mesa " + i
                        + "  [" + Thread.currentThread().getName() + "]");
            }
        }
    }

    // ---------------------------------------------------------------
    // FORMA 2: heredar de Thread
    // ---------------------------------------------------------------
    static class Cocinero extends Thread {
        Cocinero(String nombre) { super(nombre); }   // nombre del hilo

        @Override
        public void run() {
            System.out.println(getName() + " está cocinando");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Hilo actual: " + Thread.currentThread().getName());

        // FORMA 1
        Thread t1 = new Thread(new Camarero(1), "Hilo-Camarero-1");
        Thread t2 = new Thread(new Camarero(2), "Hilo-Camarero-2");

        // FORMA 2
        Thread t3 = new Cocinero("Hilo-Cocinero");

        // FORMA 3: lambda (Runnable = () -> nada)
        Thread t4 = new Thread(() -> System.out.println("Soy un hilo lambda  ["
                + Thread.currentThread().getName() + "]"), "Hilo-Lambda");

        System.out.println("Estado de t1 antes de start(): " + t1.getState());   // NEW

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        // Esperamos a que acaben (ver H02) para que el estado final sea TERMINATED
        t1.join(); t2.join(); t3.join(); t4.join();
        System.out.println("Estado de t1 al terminar: " + t1.getState());       // TERMINATED

        // ===============================================================
        // start()  vs  run()
        // ===============================================================
        System.out.println("\n--- start() vs run() ---");
        Runnable tarea = () -> System.out.println("Me ejecuta: " + Thread.currentThread().getName());

        new Thread(tarea, "HiloNuevo").start();   // ✔ crea un hilo NUEVO -> "HiloNuevo"
        new Thread(tarea, "HiloNuevo2").run();    // ❌ NO crea hilo: lo ejecuta "main"

        // Un hilo solo puede arrancarse UNA vez
        try {
            t1.start();
        } catch (IllegalThreadStateException e) {
            System.out.println("start() dos veces -> IllegalThreadStateException");
        }

        // ===============================================================
        // Crear N hilos en un array (como abrirCafeteria)
        // ===============================================================
        System.out.println("\n--- Array de hilos ---");
        int N = 3;
        Thread[] camareros = new Thread[N];
        for (int i = 0; i < N; i++) {
            final int id = i + 1;                    // efectivamente final para la lambda
            camareros[i] = new Thread(() -> System.out.println("Camarero " + id + " ha llegado"));
        }
        for (Thread t : camareros) t.start();
        for (Thread t : camareros) t.join();
        System.out.println("main termina");
    }
}
