import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class EsempiFuture {

    // Simula un operazione lenta
    static int getEtaUtente(String username) throws InterruptedException {
        System.out.println("[" + Thread.currentThread().getName() + "] Get età: " + username);
        Thread.sleep(1000);
        return new Random().nextInt(0, 100);
    }

    // Simula un operazione lenta
    static String getEmailUtente(String username) throws InterruptedException {
        System.out.println("[" + Thread.currentThread().getName() + "] Get email: " + username);
        Thread.sleep(800);
        return "imeil@gimeil.com";
    }

    // Simula un task che fallisce sempre
    static String taskCheFallisce() throws Exception {
        Thread.sleep(300);
        throw new RuntimeException("Errore durante l'esecuzione della task!");
    }

    // 1. Future e ExecutorService
    static void esempioFuture() throws Exception {
        System.out.println("\n=== 1. Future e ExecutorService ===");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Esempio con valore di ritorno
        Future<Integer> futureEta = executor.submit(() -> getEtaUtente("alice"));

        // Esempio con runnable
        Future<?> futureLog = executor.submit(() -> System.out
                .println("[" + Thread.currentThread().getName() + "] Sono la runnable di esempioFuture!"));

        // Aspetta finchè i futuri non sono completati
        System.out.println("Età di alice: " + futureEta.get());
        futureLog.get();
        System.out.println("futureLog.isDone(): " + futureLog.isDone());

        executor.shutdown();
    }

    // 2. Timeout e cancellazione
    static void esempioTimeoutECancellazione() throws Exception {
        System.out.println("\n=== 2. Timeout e cancellazione ===");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<Integer> futureLento = executor.submit(() -> {
            System.out.println(
                    "[" + Thread.currentThread().getName() + "] Inizio a contare i granelli di sabbia nel deserto...");
            Thread.sleep(5000);
            return 42;
        });

        try {
            // Aspetta al massimo 1 secondo
            int result = futureLento.get(1, TimeUnit.SECONDS);
            System.out.println("Risultato: " + result);
        } catch (TimeoutException e) {
            System.out.println("Ci hai messo troppo! Cancello la task...");
            futureLento.cancel(true); // interrupt
            System.out.println("isCancelled: " + futureLento.isCancelled());
        }

        executor.shutdownNow();
    }

    // 3. Futuri in parallelo
    static void esempioFuturiParalleli() throws Exception {
        System.out.println("\n=== 3. Futuri in parallelo ===");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<String> utenti = new ArrayList<>();
        utenti.add("Marco");
        utenti.add("Antonio");
        utenti.add("Enrico");
        utenti.add("Samuele");

        List<Future<Integer>> futuri = new ArrayList<>();

        for (String utente : utenti) {
            Future<Integer> f = executor.submit(() -> getEtaUtente(utente));
            futuri.add(f);
        }

        // Get dei risultati
        for (int i = 0; i < utenti.size(); i++) {
            System.out.printf("%s -> eta %d\n", utenti.get(i), futuri.get(i).get());
        }

        executor.shutdown();
    }

    // 4. Concatenamento CompletableFuture
    static void esempioConcatenamento() throws Exception {
        System.out.println("\n=== 4. Concatenamento CompletableFuture ===");

        CompletableFuture<String> pipeline = CompletableFuture
                // Inizia task in modo asincrono con un Supplier
                .supplyAsync(() -> {
                    System.out.println("[" + Thread.currentThread().getName() + "] Step 1: supply nome");
                    return "bob";
                })
                // Poi applica una Function sempre in modo asincrono
                .thenApplyAsync(username -> {
                    System.out.println("[" + Thread.currentThread().getName() + "] Step 2: trasformo in maiuscolo");
                    return username.toUpperCase();
                })
                // Applica una Function in modo sincrono
                // (Viene eseguito sullo stesso thread che ha eseguito lo step precedente)
                .thenApply(username -> {
                    System.out.println("[" + Thread.currentThread().getName() + "] Step 3: aggiungo un saluto");
                    return "Ciao, " + username + "!";
                });

        System.out.println("Risultato pipeline: " + pipeline.get());
    }

    // 5. Combinazione
    static void esempioCombinazione() throws Exception {
        System.out.println("\n=== 5. Combinazione ===");

        CompletableFuture<Integer> futureEta = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return getEtaUtente("matteo");
                    } catch (InterruptedException e) {
                        throw new CompletionException(e);
                    }
                });

        CompletableFuture<String> futureEmail = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return getEmailUtente("matteo");
                    } catch (InterruptedException e) {
                        throw new CompletionException(e);
                    }
                });

        // Esegue entrambe in parallelo e poi le combina
        CompletableFuture<String> combinato = futureEta.thenCombine(futureEmail,
                (eta, email) -> String.format("matteo: eta %d, email %s", eta, email));

        System.out.println("Risultato combinato: " + combinato.get());
    }

    // 6. anyOf
    static void esempioAnyOf() throws Exception {
        System.out.println("\n=== 6. anyOf ===");

        CompletableFuture<String> futuroLento = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
            return "Risposta lenta";
        });

        CompletableFuture<String> futuroVeloce = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new CompletionException(e);
            }
            return "Risposta veloce";
        });

        // Prende il primo futuro a completarsi
        Object primo = CompletableFuture.anyOf(futuroLento, futuroVeloce).get();
        System.out.println("Primo risultato: " + primo);
    }

    //7. Gestione errori
    static void esempioGestioneErrori() throws Exception {
        System.out.println("\n=== 7. Gestione errori ===");

        // exceptionally ritorna un valore di fallback
        String risultato1 = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return taskCheFallisce();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .exceptionally(ex -> {
                    System.out.println("Eccezione: " + ex.getCause().getMessage());
                    return "valore di fallback";
                })
                .get();

        System.out.println("Risultato exceptionally: " + risultato1);

        // handle gestisce sia il valore che l'errore
        String risultato2 = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return taskCheFallisce();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .handle((valore, ex) -> {
                    if (ex != null) {
                        System.out.println("Errore: " + ex.getCause().getMessage());
                        return "valore di fallback";
                    }
                    return valore;
                })
                .get();

        System.out.println("Risultato handle: " + risultato2);
    }

    //8. Completazione manuale
    static void esempioCompletazioneManuale() throws Exception {
        System.out.println("\n=== 8. Completazione manuale ===");

        //Creo un future
        CompletableFuture<String> future = new CompletableFuture<>();
        
        //Dopo 500ms lo completo io
        Thread.sleep(500);
        future.complete("Futuro completato da qualcuno");

        System.out.println("Risultato: " + future.get());
    }

    //Main
    //vuoto statico pubblico principale lancia eccezione
    public static void main(String[] args) throws Exception {
        esempioFuture();
        esempioTimeoutECancellazione();
        esempioFuturiParalleli();
        esempioConcatenamento();
        esempioCombinazione();
        esempioAnyOf();
        esempioGestioneErrori();
        esempioCompletazioneManuale();
    }
}
