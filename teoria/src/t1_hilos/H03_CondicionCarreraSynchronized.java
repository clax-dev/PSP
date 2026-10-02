package t1_hilos;

/**
 * =====================================================================
 *  TEORÍA 3.3 — CONDICIÓN DE CARRERA Y synchronized
 *  Basado en: el bloque synchronized (LOCK) de Cafeteria.java
 * =====================================================================
 *
 *  RECURSO COMPARTIDO: dato al que acceden varios hilos (variable static, lista, consola...).
 *
 *  CONDICIÓN DE CARRERA: el resultado depende del orden en que se intercalan los hilos.
 *    contador++ NO es atómico, son 3 pasos:  leer -> sumar 1 -> escribir
 *    Si dos hilos leen el mismo valor a la vez, uno de los incrementos se PIERDE.
 *
 *  SECCIÓN CRÍTICA: trozo de código que solo debe ejecutar UN hilo a la vez.
 *
 *  synchronized (cerrojo) { ... }
 *    - Solo un hilo puede tener el cerrojo (monitor) de ese objeto a la vez.
 *    - Los demás se quedan BLOQUEADOS (estado BLOCKED) esperando.
 *    - ⚠ TODOS los hilos deben usar el MISMO objeto cerrojo.
 *      Por eso en Cafeteria es:  private static final Object LOCK = new Object();
 *      Si cada hilo tuviera su propio LOCK, no habría exclusión ninguna.
 *
 *  También se puede sincronizar un método entero:
 *      public synchronized void metodo()        -> cerrojo = this
 *      public static synchronized void metodo() -> cerrojo = la clase
 *
 *  ⚠ Sincroniza SOLO lo imprescindible: dentro del synchronized los hilos van de uno en uno
 *    (se pierde paralelismo).
 */
public class H03_CondicionCarreraSynchronized {

    static int contadorInseguro = 0;
    static int contadorSeguro = 0;
    private static final Object LOCK = new Object();   // cerrojo compartido

    public static void main(String[] args) throws InterruptedException {
        final int HILOS = 4;
        final int INCREMENTOS = 100_000;

        // ==============================================================
        // 1) SIN sincronizar: se pierden incrementos
        // ==============================================================
        Thread[] hilos = new Thread[HILOS];
        for (int i = 0; i < HILOS; i++) {
            hilos[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTOS; j++) {
                    contadorInseguro++;             // ❌ condición de carrera
                }
            });
            hilos[i].start();
        }
        for (Thread t : hilos) t.join();

        // ==============================================================
        // 2) CON synchronized: resultado siempre correcto
        // ==============================================================
        for (int i = 0; i < HILOS; i++) {
            hilos[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTOS; j++) {
                    synchronized (LOCK) {           // ✔ sección crítica
                        contadorSeguro++;
                    }
                }
            });
            hilos[i].start();
        }
        for (Thread t : hilos) t.join();

        System.out.println("Esperado:            " + (HILOS * INCREMENTOS));
        System.out.println("Sin synchronized:    " + contadorInseguro + "   <- casi siempre MENOR");
        System.out.println("Con synchronized:    " + contadorSeguro);

        // ==============================================================
        // 3) Sincronizar la SALIDA por consola (lo que hace Cafeteria)
        //    Sin el bloque, las líneas del diálogo de distintos camareros se mezclarían.
        // ==============================================================
        System.out.println("\n--- Diálogos sin mezclarse ---");
        for (int i = 1; i <= 3; i++) {
            final int id = i;
            hilos[i - 1] = new Thread(() -> {
                synchronized (LOCK) {                 // las 3 líneas salen JUNTAS
                    System.out.println("-Camarero " + id + ": ¿Qué desea?");
                    System.out.println("-Cliente: Un café");
                    System.out.println("-Camarero " + id + ": Aquí tiene.");
                }
            });
            hilos[i - 1].start();
        }
        for (int i = 0; i < 3; i++) hilos[i].join();

        // ==============================================================
        // 4) Método synchronized
        // ==============================================================
        Caja caja = new Caja();
        Thread c1 = new Thread(() -> { for (int i = 0; i < 1000; i++) caja.cobrar(1); });
        Thread c2 = new Thread(() -> { for (int i = 0; i < 1000; i++) caja.cobrar(1); });
        c1.start(); c2.start(); c1.join(); c2.join();
        System.out.println("\nTotal en caja (esperado 2000): " + caja.getTotal());
    }

    static class Caja {
        private int total = 0;

        public synchronized void cobrar(int euros) {   // cerrojo = this (la caja)
            total += euros;
        }

        public synchronized int getTotal() {
            return total;
        }
    }
}
