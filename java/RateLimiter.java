import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

class RateLimiter {
    private ReentrantLock lock = new ReentrantLock();
    private int rpm; // requests per minute
    private Duration period; // time period for rpm
    private Instant lastTime; // last request time
    private Duration spacing; // minimum time between requests

    public RateLimiter(int rpm) {
        this.rpm = rpm;
        this.period = Duration.ofMinutes(1);
        this.spacing = Duration.ofMillis(60_000 / rpm);
    }

    public void schedule(int i) {
        // space the requests

        lock.lock();
        var now = Instant.now();
        try {
            // if(lastTime == null && now.from(lastTime).compareTo(this.spacing) > 0) {
            // if r.lastTime != nil && now.Sub(*r.lastTime) <= r.spacing {
            if (true) {
                lastTime = now;
                var wait = this.spacing.minus(Duration.between(lastTime, now).abs());
                Thread.sleep(wait); // TODO: does this sleep all threads or just this one?
            }
        } catch (

        Exception e) {
            e.printStackTrace();
        } finally {
            this.lock.unlock();
        }
    }

    public void clear() {
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

            System.out.println("Response Code: " + statusCode);
            System.out.println("Cleared:\n" + responseBody);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void results() {
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
            int statusCode = response.statusCode();
            String responseBody = response.body();

            System.out.println("Response Code: " + statusCode);
            System.out.println("Results Response Body:\n" + responseBody);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return "RateLimiter: " + rpm + " rpm | " + spacing.toMillis() / 1000.0 + " seconds spacing";
    }
}
