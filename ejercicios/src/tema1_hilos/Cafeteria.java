package tema1_hilos;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static tema1_hilos.Cafeteria.Camarero.*;
import static tema1_hilos.Cafeteria.StoreStats.contadorProductos;

public class Cafeteria {

    class Camarero implements Runnable {
        public static int NUMERO_CLIENTES = 10; // Número total de clientes
        public static int NUMERO_CAMAREROS = 5; // Número total de camareros
        private int ID_CAMARERO;
        private static final Object LOCK = new Object(); // share variable lock for all the code to synchronize the print statements
        private static final Queue<String> colaClientes = new ConcurrentLinkedQueue<>(); // Cola segura para hilos que almacena los nombres de los clientes

        public Camarero(int ID_CAMARERO) {
            this.ID_CAMARERO = ID_CAMARERO;
        }

        @Override
        public void run() {
            // mientras haya clientes y el hilo no esté interrumpido, el camarero atenderá a los clientes
            while (!colaClientes.isEmpty() && !Thread.currentThread().isInterrupted()) {
                String cliente = colaClientes.poll(); // cogemos y sacamos un cliente de la cola
                int carta_cliente = (int) (Math.random() * 2) + 1; // numero aleatorio para el tipo de carta del cliente (1 o 2)

                // sincronizamos el bloque para imprimir de manera ordenada y evitar que varios hilos impriman al mismo tiempo
                synchronized (LOCK) {
                    if (carta_cliente == 1) {
                        int al1 = (int) (Math.random() * 2) + 1; // número aleatorio para el tipo de ropa del cliente (1 o 2)
                        System.out.println("-Camarero: Pantalón o camisa?");
                        if (al1 == 1) {
                            System.out.println("-Cliente (#" + cliente + "): Pantalón");
                            System.out.println("-Camarero (ID: " + ID_CAMARERO + ") : Aquí tiene su pantalón.");
                        } else {
                            System.out.println("-Cliente (#" + cliente + "): Camisa");
                            System.out.println("-Camarero (ID: " + ID_CAMARERO + ") : Aquí tiene su camisa.");
                        }
                    } else if (carta_cliente == 2) {
                        int al2 = (int) (Math.random() * 2) + 1;
                        System.out.println("-Camarero: ¿Qué consumible desea?");
                        if (al2 == 1) {
                            System.out.println("-Cliente (#" + cliente + "): Bebida");
                            System.out.println("-Camarero (ID: " + ID_CAMARERO + ") : Aquí tiene su bebida.");
                            contadorProductos();
                        } else {
                            System.out.println("-Cliente (#" + cliente + "): Comida");
                            System.out.println("-Camarero (ID: " + ID_CAMARERO + ") : Aquí tiene su comida.");
                            contadorProductos();
                        }
                    }
                }
            }

        }
    }

    public static void terminarPrograma() {
        if (Camarero.colaClientes.size() <= 0) {
            System.out.println("Todos los clientes han sido atendidos.");
            System.out.println("Cerrando cafeteria...");
            // termina el programa
            System.exit(0);
        }
    }

    public void abrirCafeteria() {
        // instancia de los hilos.
        Thread[] camareros = new Thread[NUMERO_CAMAREROS];
        for (int i = 0; i < NUMERO_CAMAREROS; i++) {
            camareros[i] = new Thread(new Camarero(i + 1));
            System.out.println("El camarero " + (i + 1) + " ha llegado a la cafetería");
        }

        // metemos clientes en la cola de clientes
        for (int i = 1; i <= NUMERO_CLIENTES; i++) {
            String cliente = "Cliente #" + i;
            Camarero.colaClientes.add(cliente);
        } // ahora ya podrá entrar en el bucle while

        // iniciamos los hilos (les damos la vida y cuando terminan la acción del hilo, mueren)
        for (int i = 0; i < NUMERO_CAMAREROS; i++) {
            camareros[i].start();
        }

        try {
            // esperamos a que los hilos terminen su ejecución antes de continuar
            for (int i = 0; i < NUMERO_CAMAREROS; i++) {
                camareros[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    static class StoreStats {
        static int contador = 0;
        private static final Object LOCK = new Object();
        // hilos implementando runnable

        public static void contadorProductos() {
            synchronized (LOCK) {
                contador++;
            }
            if (!Camarero.colaClientes.isEmpty()) {
                System.out.println("clientes servidos -> " + contador);
            } else {
                System.out.println("Total served customers: " + StoreStats.contador);
            }
        }
    }

    public static void main(String[] args) {
        Cafeteria cafeteria = new Cafeteria();
        cafeteria.abrirCafeteria();

        terminarPrograma();
    }

}