import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

class App {
    public static void main(String[] args) throws InterruptedException, Exception {
        int n = 600/10;
        var rl = new RateLimiter(600);
        System.out.println(rl);
        if (args.length > 0)
            return;
        run(n, rl);
        // runPool(n);
    }

    /*
     * One worker per request
     */
    public static void run(int n, RateLimiter rl) throws InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        clear();
        System.out.println();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < n; i++) {
                int j = i + 1;
                executor.submit(() -> {
                    // create a request
                    String url = String.format("http://localhost:3000?i=%d", j);
                    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                            .GET()
                            .build();

                    rl.schedule(j);

                    // send the request
                    CompletableFuture<HttpResponse<String>> responseFut = client.sendAsync(request,
                            HttpResponse.BodyHandlers.ofString());

                    responseFut.join();
                    // var response = responseFut.join();
                    // Access the response status code and body
                    // int statusCode = response.statusCode();
                    // String responseBody = response.body();

                    // System.out.println("Response Code: " + statusCode);
                    // System.out.println("Response Body:\n" + responseBody);
                    // System.out.println();
                });
            }
        }
        System.out.println();
        results();
    }

    /*
     * worker pool
     */
    public static void runPool(int n) throws Exception {
        throw new Exception("ahhh" + n);
    }

    public static void clear() {
        HttpClient client = HttpClient.newHttpClient();
        // create a request
        String url = "http://localhost:3000";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .DELETE()
                .build();
        try {
            // send the request
            CompletableFuture<HttpResponse<String>> responseFut = client.sendAsync(request,
                    HttpResponse.BodyHandlers.ofString());

            var response = responseFut.join();
            // Access the response status code and body
            int statusCode = response.statusCode();
            String responseBody = response.body();

            // System.out.println("Response Code: " + statusCode);
            System.out.println("Cleared:\n" + responseBody);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void results() {
        HttpClient client = HttpClient.newHttpClient();
        // create a request
        String url = "http://localhost:3000/results";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();
        try {
            // send the request
            CompletableFuture<HttpResponse<String>> responseFut = client.sendAsync(request,
                    HttpResponse.BodyHandlers.ofString());

            var response = responseFut.join();
            // Access the response status code and body
            // int statusCode = response.statusCode();
            String responseBody = response.body();

            // System.out.println("Response Code: " + statusCode);
            System.out.println("Results Response Body:\n" + responseBody);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
