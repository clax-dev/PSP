package tema2_concurrencia;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;   // ✅ LocalDate solo tiene fecha; LocalDateTime tiene fecha Y hora
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class WriteCSV implements Callable<Path> {
    int NUMERO_TAREA;
    List<String> pacientes = List.of(
            "ID;NOMBRE;EDAD;DIAGNOSTICO;ESTADO",
            "101;Ana García;62;Diabetes;ACTIVO",
            "102;Luis Pérez;45;Hipertensión;ACTIVO",
            "103;Marta Ruiz;38;Asma;INACTIVO",
            "104;Pablo Sánchez;70;Diabetes;INACTIVO",
            "105;Sara López;29;Migraña;ACTIVO",
            "106;Álvaro Gómez;55;Hipertensión;ACTIVO"
    );

    public WriteCSV(int NUMERO_TAREA) {
        this.NUMERO_TAREA = NUMERO_TAREA;
    }

    @Override
    public Path call() throws Exception {
        LocalDateTime fecha = LocalDateTime.now();   // ✅ con hora, para poder usar HHmmss
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        System.out.println("Current thread name = " + Thread.currentThread().getName());
        System.out.println("Current task number = " + NUMERO_TAREA);

        Path carpeta = Paths.get("prueba_writeCSV");
        // ✅ el número de tarea en el nombre: así 2 tareas en el mismo segundo no se machacan el fichero
        Path fichero = carpeta.resolve("pacientes_" + fecha.format(dtf) + "_" + NUMERO_TAREA + ".csv");
        Files.createDirectories(carpeta);
        Files.write(fichero, pacientes);
        return fichero;
    }

    // ✅ List<Future<Path>> en vez de List a secas (quita el aviso "unchecked")
    public static List<Future<Path>> entregar(List<Future<Path>> futures, ExecutorService executor) {
        for (int i = 1; i <= 5; i++) {               // ✅ <= 5 para que llegue a la tarea 5
            futures.add(executor.submit(new WriteCSV(i)));
            // ✅ quitado el i++ que había aquí: el for ya suma 1 en cada vuelta
        }
        return futures;
    }

    // ✅ recibe la LISTA de Futures (no un Path): es lo que necesita para recorrerla
    // ✅ throws Exception: get() puede lanzar InterruptedException y ExecutionException
    public static void mostrarRuta(List<Future<Path>> futures) throws Exception {
        for (Future<Path> future : futures) {
            Path ruta = future.get();
            System.out.println("Nombre:   " + ruta.getFileName());                     // ✅ información
            System.out.println("Ruta:     " + ruta.toAbsolutePath());                  //    del fichero
            System.out.println("Tamaño:   " + Files.size(ruta) + " bytes");
            System.out.println("Líneas:   " + Files.readAllLines(ruta).size());
            System.out.println("---");
        }
    }

    static void main() throws Exception {
        List<Future<Path>> futures = new ArrayList<>();
        ExecutorService exec = Executors.newFixedThreadPool(3);
        entregar(futures, exec);
        mostrarRuta(futures);                         // ✅ se le pasa la lista, y faltaba el ;

        exec.shutdown();
    }
}
