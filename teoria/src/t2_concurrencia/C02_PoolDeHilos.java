package t2_concurrencia;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * =====================================================================
 *  TEORÍA 2.2 — POOL DE HILOS: ExecutorService, Callable y Future
 * =====================================================================
 *
 *  Hasta ahora: new Thread(...) por cada tarea. Problema: crear hilos es CARO
 *  y si llegan 1000 clientes no puedes crear 1000 camareros.
 *
 *  POOL DE HILOS = una plantilla FIJA de camareros que se van turnando las tareas.
 *  Tú solo entregas TAREAS; el pool decide qué hilo las ejecuta.
 *
 *      ExecutorService pool = Executors.newFixedThreadPool(3);  // 3 hilos reutilizables
 *      pool.submit(tarea);                                      // entregar trabajo
 *      pool.shutdown();                                         // no aceptar más tareas
 *      pool.awaitTermination(...)                               // esperar a que acaben (como join)
 *
 *  | Tipo de tarea   | Método     | Devuelve resultado | Puede lanzar excepción comprobada |
 *  |-----------------|------------|--------------------|-----------------------------------|
 *  | Runnable        | run()      | NO                 | NO                                |
 *  | Callable<T>     | call()     | SÍ (un T)          | SÍ                                |
 *
 *  FUTURE<T> = "resguardo" del resultado de un Callable que aún se está calculando.
 *      future.get()     -> espera a que termine y devuelve el resultado (bloquea)
 *      future.isDone()  -> ¿ya ha terminado?
 *
 *  ⚠ Si olvidas shutdown(), el programa NO termina: los hilos del pool siguen vivos esperando trabajo.
 */
public class C02_PoolDeHilos {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        // ==============================================================
        // 1) Runnable en un pool: 6 clientes, solo 3 camareros
        //    Fíjate en los nombres: los hilos se REUTILIZAN (pool-1-thread-1, -2, -3)
        // ==============================================================
        System.out.println("=== 1) 6 clientes, 3 camareros (Runnable) ===");
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 6; i++) {
            final int cliente = i;
            pool.submit(() -> {
                System.out.println("Cliente #" + cliente + " atendido por " + Thread.currentThread().getName());
                dormir(100);
            });
        }
        pool.shutdown();                                   // ya no entran más clientes
        pool.awaitTermination(5, TimeUnit.SECONDS);         // esperamos a que acaben
        System.out.println("¿Pool terminado? " + pool.isTerminated());

        // ==============================================================
        // 2) Callable + Future: tareas que DEVUELVEN un resultado
        //    Cada pedido calcula su precio; main recoge los resultados.
        // ==============================================================
        System.out.println("\n=== 2) Pedidos con resultado (Callable + Future) ===");
        ExecutorService cocina = Executors.newFixedThreadPool(2);
        List<Future<Double>> cuentas = new ArrayList<>();

        String[] pedidos = {"café", "tostada", "zumo", "bocadillo"};
        double[] precios = {1.50, 2.20, 2.80, 4.50};

        for (int i = 0; i < pedidos.length; i++) {
            final String pedido = pedidos[i];
            final double precio = precios[i];
            Callable<Double> tarea = () -> {               // Callable = lambda que DEVUELVE algo
                dormir(150);
                System.out.println("  preparado " + pedido + " en " + Thread.currentThread().getName());
                return precio;
            };
            cuentas.add(cocina.submit(tarea));             // submit devuelve un Future
        }

        double total = 0;
        for (Future<Double> f : cuentas) {
            total += f.get();                              // get() ESPERA a que esa tarea acabe
        }
        cocina.shutdown();
        System.out.printf("Total de la cuenta: %.2f €%n", total);
    }

    static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
