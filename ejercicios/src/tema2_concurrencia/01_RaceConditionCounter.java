package tema2_concurrencia;

/**
 * Two threads update the same integer without synchronization.
 * The expected value is 2_000, but the actual value may be lower.
 */
class RaceConditionCounter01 {
    private static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        Thread firstWorker = new Thread(() -> {incrementCounter(); IO.println("First worker");}, "first-worker");
        Thread secondWorker = new Thread(() -> {incrementCounter(); IO.println("Second worker");}, "second-worker");

        firstWorker.start();
        secondWorker.start();

        firstWorker.join();
        secondWorker.join();

        System.out.println("Expected value: 2000");
        System.out.println("Actual value:   " + counter);
    }

    private static void incrementCounter() {
        for (int i = 0; i < 1_000; i++) {
            counter++;
            System.out.println("["+Thread.currentThread().getName()+"] El valor actual es: " + counter);

        }
    }
}
