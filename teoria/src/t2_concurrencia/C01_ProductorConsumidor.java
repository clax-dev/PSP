package t2_concurrencia;

import java.util.LinkedList;
import java.util.Queue;

/**
 * =====================================================================
 *  TEORÍA 2.1 — PRODUCTOR / CONSUMIDOR con wait() y notifyAll()
 *  Continuación de la Cafetería (tema 1): ahora hay un COCINERO que prepara platos
 *  y CAMAREROS que los recogen de una BARRA con sitio limitado.
 * =====================================================================
 *
 *  PROBLEMA: dos tipos de hilos comparten un "almacén" (buffer) limitado.
 *    - El PRODUCTOR (cocinero) no puede dejar platos si la barra está LLENA.
 *    - El CONSUMIDOR (camarero) no puede coger platos si la barra está VACÍA.
 *    Con synchronized evitamos que se pisen, pero ¿cómo ESPERAN sin gastar CPU?
 *
 *  wait()       -> "me duermo y SUELTO el cerrojo hasta que alguien me avise"
 *  notifyAll()  -> "despierto a todos los que esperan en este cerrojo"
 *  notify()     -> despierta a UNO cualquiera (más arriesgado; mejor notifyAll)
 *
 *  REGLAS DE ORO
 *    1. wait/notify SOLO dentro de synchronized sobre el MISMO objeto
 *       (si no -> IllegalMonitorStateException).
 *    2. La condición se comprueba con WHILE, nunca con IF:
 *       al despertar, otro hilo pudo adelantarse y la condición puede volver a ser falsa.
 *    3. wait() lanza InterruptedException -> try/catch o throws.
 *
 *  ▶ Ejecuta varias veces: el orden cambia, pero NUNCA hay más de 3 platos en la barra
 *    ni se coge un plato que no existe.
 */
public class C01_ProductorConsumidor {

    // El recurso compartido: una barra con capacidad limitada.
    static class Barra {
        private final Queue<String> platos = new LinkedList<>();
        private final int capacidad;

        Barra(int capacidad) { this.capacidad = capacidad; }

        // PRODUCTOR
        public synchronized void dejar(String plato) throws InterruptedException {
            while (platos.size() == capacidad) {          // WHILE, no if
                System.out.println("   [barra llena] el cocinero espera...");
                wait();                                   // suelta el cerrojo y duerme
            }
            platos.add(plato);
            System.out.println("Cocinero deja " + plato + "   -> en barra: " + platos.size());
            notifyAll();                                  // avisa: "ya hay platos"
        }

        // CONSUMIDOR
        public synchronized String recoger(String camarero) throws InterruptedException {
            while (platos.isEmpty()) {                    // WHILE, no if
                System.out.println("   [barra vacía] " + camarero + " espera...");
                wait();
            }
            String plato = platos.poll();
            System.out.println(camarero + " recoge " + plato + "   -> en barra: " + platos.size());
            notifyAll();                                  // avisa: "ya hay sitio"
            return plato;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Barra barra = new Barra(3);
        final int PLATOS = 8;

        Thread cocinero = new Thread(() -> {
            try {
                for (int i = 1; i <= PLATOS; i++) {
                    barra.dejar("plato#" + i);
                    Thread.sleep(20);                     // cocina rápido -> la barra se llena
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Dos camareros que recogen 4 platos cada uno (4 + 4 = 8)
        Runnable camarero = () -> {
            String nombre = Thread.currentThread().getName();
            try {
                for (int i = 0; i < PLATOS / 2; i++) {
                    barra.recoger(nombre);
                    Thread.sleep(120);                    // tarda en servir
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        Thread c1 = new Thread(camarero, "Camarero-1");
        Thread c2 = new Thread(camarero, "Camarero-2");

        cocinero.start(); c1.start(); c2.start();
        cocinero.join(); c1.join(); c2.join();
        System.out.println("Servicio terminado: los " + PLATOS + " platos se han servido.");
    }
}
