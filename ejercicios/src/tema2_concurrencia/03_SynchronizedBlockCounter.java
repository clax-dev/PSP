package tema2_concurrencia;

import java.util.concurrent.CountDownLatch;

/**
 * Only the critical section is protected by a private shared monitor.
 */
class SynchronizedBlockCounter03 {
    private static int counter = 0;
    // Objeto lock
    private static final Object LOCK = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread firstWorker = new Thread(() -> runIncrements(), "first-worker");
        Thread secondWorker = new Thread(() -> runIncrements(), "second-worker");

        firstWorker.start();
        secondWorker.start();

        firstWorker.join();
        secondWorker.join();

        System.out.println("Expected value: 200");
        System.out.println("Actual value:   " + counter);
    }

    private static void runIncrements() {
        System.out.println(Thread.currentThread().getName() + " Antes del LOCK");
        //synchronized (cou)
        synchronized (LOCK) {
            for (int i = 0; i < 100; i++) {
                System.out.println(Thread.currentThread().getName() + "valor actual -> " + counter);
                if(counter%100 == 0) {
                    IO.println(Thread.currentThread().getName() + ": " + counter);
                }
                counter++;
            }
        }
        System.out.println("Despues del LOCK");
    }
}

