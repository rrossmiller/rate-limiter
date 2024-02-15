import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

class App {
    public static void main(String[] args) throws InterruptedException, Exception {
        HttpClient client = HttpClient.newHttpClient();
        var rl = new RateLimiter(600);

        // if (args.length == 0)
        // return;
        rl.clear();
        System.out.println();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 1200; i++) {
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

                    var response = responseFut.join();
                    // Access the response status code and body
                    // int statusCode = response.statusCode();
                    // String responseBody = response.body();

                    // System.out.println("Response Code: " + statusCode);
                    // System.out.println("Response Body:\n" + responseBody);
                    // System.out.println();
                });
            }
            // var t1 = Thread.ofVirtual().start(r);
            // var t2 = Thread.ofVirtual().start(r);
            // System.out.println();
            // t1.join();
            // System.out.println();
            // t2.join();

        }
        System.out.println();
        rl.results();
    }
}
