package t3_locks;

/**
 * =====================================================================
 *  TEORÍA 3.2 — SOLUCIÓN 1: synchronized (un cerrojo para TODOS)
 * =====================================================================
 *
 *  synchronized (o ReentrantLock) es un cerrojo EXCLUSIVO: dentro solo cabe
 *  UN hilo, da igual que vaya a leer o a escribir.
 *
 *    ✔ Ya no hay lecturas incoherentes: el camarero cambia carta y pizarra
 *      sin que nadie mire en medio.
 *    ✘ Pero los clientes, que solo MIRAN, también hacen cola entre ellos.
 *      Dos clientes leyendo a la vez no se molestarían nada... y aun así esperan.
 *
 *  Analogía: una biblioteca con UN solo pase. Para consultar un libro
 *  o para reorganizar las estanterías hay que tener el pase. Uno cada vez.
 *
 *  Ejecuta y mira el tiempo: 4 clientes × 50 lecturas × 10 ms cada una
 *  = 2000 ms, porque van UNO DETRÁS DE OTRO. (En L03 bajará a ~500 ms.)
 */
public class L02_Synchronized {

    static int precioCarta = 100;
    static int precioPizarra = 100;
    static int incoherentes = 0;

    static final Object CERROJO = new Object();

    static void leer() {
        synchronized (CERROJO) {
            int carta = precioCarta;
            L01_SinSincronizacion.dormir(10);         // leer la carta lleva su tiempo
            int pizarra = precioPizarra;
            if (carta != pizarra) incoherentes++;
        }
    }

    static void escribir(int nuevo) {
        synchronized (CERROJO) {
            precioCarta = nuevo;
            L01_SinSincronizacion.dormir(1);
            precioPizarra = nuevo;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread camarero = new Thread(() -> {
            for (int nuevo = 101; nuevo <= 105; nuevo++) {
                escribir(nuevo);
                L01_SinSincronizacion.dormir(50);
            }
        }, "Camarero");

        Thread[] clientes = new Thread[4];
        for (int i = 0; i < clientes.length; i++) {
            clientes[i] = new Thread(() -> {
                for (int j = 0; j < 50; j++) leer();
            }, "Cliente-" + (i + 1));
        }

        long inicio = System.currentTimeMillis();
        camarero.start();
        for (Thread c : clientes) c.start();
        camarero.join();
        for (Thread c : clientes) c.join();
        long ms = System.currentTimeMillis() - inicio;

        System.out.println("Lecturas incoherentes: " + incoherentes + "   <- ✔ arreglado");
        System.out.println("Tiempo total:          " + ms + " ms   <- ✘ los lectores han hecho cola entre ellos");
        System.out.println("\nSiguiente demo: L03 (ReentrantReadWriteLock): que los lectores entren juntos.");
    }
}
