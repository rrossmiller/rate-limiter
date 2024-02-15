package com.rate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

class App {
    public static void main(String[] args) throws InterruptedException, Exception {
        int n = 600 / 10;
        var rl = new RateLimiter(600);
        System.out.println(rl);
        if (args.length > 0)
            return;

        System.out.println("N reqs = " + n);
        System.out.println("Should take a minimum of " + n * 60 / 600 + " seconds");
        // run(n, rl);
        runPool(n, rl);
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
                    // CompletableFuture<HttpResponse<String>> responseFut =
                    // client.sendAsync(request,
                    var responseFut = client.sendAsync(request,
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
     * worker pool. This is generally not necessary. It's totally fine to spin up as
     * many vthreads as there are tasks
     */
    public static void runPool(int n, RateLimiter rl) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        clear();
        System.out.println();
        // add tasks to the queue
        var q = new ArrayBlockingQueue<HttpRequest>(n, true);
        for (int i = 0; i < n; i++) {
            String url = String.format("http://localhost:3000?i=%d", i);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .build();
            q.add(request);
        }

        rl.setBar(n);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            int j=i;
            var t = Thread.ofVirtual().start(() -> {
                while (!q.isEmpty()) {
                    try {
                        var req = q.take();
                        rl.schedule(j);
                        // send the request
                        CompletableFuture<HttpResponse<String>> responseFut = client.sendAsync(req,
                                HttpResponse.BodyHandlers.ofString());

                        responseFut.join();
                    } catch (InterruptedException e) {
                    }
                }
            });
            threads.add(t);
        }
        while (!threads.isEmpty()) {
            threads = threads.stream().filter(e -> e.isAlive()).collect(Collectors.toList());
        }

        System.out.println();
        results();
    }

    /*
     * Helper methods
     */
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
