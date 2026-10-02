package t1_hilos;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * =====================================================================
 *  TEORÍA 3.4 — COLECCIONES SEGURAS PARA HILOS + "comprobar y actuar"
 *  Basado en: la cola de clientes ConcurrentLinkedQueue de Cafeteria.java
 * =====================================================================
 *
 *  ArrayList, LinkedList y HashMap NO son seguras si varios hilos las modifican a la vez
 *  (se pierden datos o lanzan excepciones).
 *
 *  | Normal                 | Segura para hilos (java.util.concurrent)               |
 *  |------------------------|--------------------------------------------------------|
 *  | LinkedList (Queue)     | ConcurrentLinkedQueue                                  |
 *  | HashMap                | ConcurrentHashMap                                      |
 *  | ArrayList              | CopyOnWriteArrayList / Collections.synchronizedList()  |
 *
 *  ⚠ CUIDADO — "COMPROBAR Y LUEGO ACTUAR" (check-then-act):
 *    Aunque CADA operación de la colección sea segura, DOS operaciones seguidas NO lo son.
 *
 *        while (!cola.isEmpty()) {        // 1. compruebo: hay 1 cliente
 *            String c = cola.poll();      // 2. ...pero otro hilo se lo llevó entre medias
 *                                         //    -> c == null  ❌
 *
 *    SOLUCIÓN: hacer UNA sola operación atómica y comprobar su resultado:
 *
 *        String c;
 *        while ((c = cola.poll()) != null) { ... }   ✔
 */
public class H04_ColeccionesConcurrentes {

    public static void main(String[] args) throws InterruptedException {

        // ==============================================================
        // 1) ArrayList compartida SIN protección -> resultados erróneos
        // ==============================================================
        List<Integer> insegura = new ArrayList<>();
        List<Integer> segura = new CopyOnWriteArrayList<>();
        Runnable rellenar = () -> {
            for (int i = 0; i < 1000; i++) {
                try {
                    insegura.add(i);
                } catch (Exception e) {
                    // ArrayList puede incluso lanzar ArrayIndexOutOfBoundsException
                }
                segura.add(i);
            }
        };
        Thread a = new Thread(rellenar), b = new Thread(rellenar);
        a.start(); b.start(); a.join(); b.join();
        System.out.println("ArrayList (esperado 2000):            " + insegura.size());
        System.out.println("CopyOnWriteArrayList (esperado 2000): " + segura.size());

        // ==============================================================
        // 2) Patrón Cafeteria CORRECTO: varios consumidores de una cola
        // ==============================================================
        System.out.println("\n--- Cafetería con poll() atómico ---");
        Queue<String> colaClientes = new ConcurrentLinkedQueue<>();
        for (int i = 1; i <= 10; i++) colaClientes.add("Cliente #" + i);

        ConcurrentHashMap<Integer, Integer> atendidosPor = new ConcurrentHashMap<>();

        Thread[] camareros = new Thread[3];
        for (int i = 0; i < camareros.length; i++) {
            final int id = i + 1;
            camareros[i] = new Thread(() -> {
                String cliente;
                // ✔ poll() saca y devuelve en UN paso; si está vacía devuelve null
                while ((cliente = colaClientes.poll()) != null
                        && !Thread.currentThread().isInterrupted()) {
                    System.out.println("Camarero " + id + " atiende a " + cliente);
                    atendidosPor.merge(id, 1, Integer::sum);   // merge es atómico en ConcurrentHashMap
                }
            });
            camareros[i].start();
        }
        for (Thread t : camareros) t.join();

        System.out.println("Clientes atendidos por camarero: " + atendidosPor);
        System.out.println("Quedan en cola: " + colaClientes.size());
    }
}
