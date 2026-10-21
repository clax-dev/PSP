package t3_locks;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * =====================================================================
 *  TEORÍA 3.1 — EL PROBLEMA: leer mientras otro escribe (SIN sincronización)
 * =====================================================================
 *
 *  Seguimos en la cafetería. El precio del café aparece en DOS sitios:
 *  la CARTA de la mesa y la PIZARRA de la barra. Siempre tienen que coincidir.
 *
 *    - Muchos CLIENTES (lectores) miran el precio todo el rato.
 *    - Un CAMARERO (escritor) cambia el precio de vez en cuando:
 *          1º cambia la carta  ->  2º cambia la pizarra
 *
 *  Si un cliente mira JUSTO entre el paso 1 y el 2, ve la carta con el precio
 *  nuevo y la pizarra con el viejo: una LECTURA INCOHERENTE (dato "a medias").
 *
 *  Nadie protege nada, así que esto pasa muchas veces. Ejecuta y mira el contador.
 *
 *  Ojo: el problema NO es que dos clientes lean a la vez (eso es inofensivo).
 *  El problema es que alguien LEA mientras otro ESCRIBE.
 */
public class L01_SinSincronizacion {

    static int precioCarta = 100;      // en céntimos
    static int precioPizarra = 100;

    public static void main(String[] args) throws InterruptedException {
        AtomicInteger incoherentes = new AtomicInteger();   // solo para contar (es seguro)
        AtomicInteger lecturas = new AtomicInteger();

        Thread camarero = new Thread(() -> {
            for (int nuevo = 101; nuevo <= 200; nuevo++) {
                precioCarta = nuevo;                 // paso 1
                dormir(1);                           // (cambiar la pizarra le lleva un momento)
                precioPizarra = nuevo;               // paso 2
                dormir(5);                           // descansa hasta el siguiente cambio
            }
        }, "Camarero");

        Runnable cliente = () -> {
            while (camarero.isAlive()) {
                int carta = precioCarta;
                int pizarra = precioPizarra;
                lecturas.incrementAndGet();
                if (carta != pizarra) incoherentes.incrementAndGet();
                dormir(1);
            }
        };

        Thread[] clientes = new Thread[4];
        for (int i = 0; i < clientes.length; i++) {
            clientes[i] = new Thread(cliente, "Cliente-" + (i + 1));
        }

        camarero.start();
        for (Thread c : clientes) c.start();
        camarero.join();
        for (Thread c : clientes) c.join();

        System.out.println("Lecturas totales:     " + lecturas.get());
        System.out.println("Lecturas incoherentes: " + incoherentes.get() + "   <- carta y pizarra NO coincidían");
        System.out.println("\nSolución: proteger el acceso. Siguiente demo: L02 (synchronized).");
    }

    static void dormir(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
