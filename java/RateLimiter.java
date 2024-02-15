import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

class RateLimiter {
    private ReentrantLock lock = new ReentrantLock();
    private int rpm; // requests per minute
    private Duration period; // time period for rpm
    private Instant lastTime; // last request time
    private Duration spacing; // minimum time between requests

    public RateLimiter(int rpm) {
        this.lastTime = Instant.ofEpochSecond(0);
        this.rpm = rpm;
        this.period = Duration.ofMinutes(1);
        int spacingMillis = (int) (1000 * (float) this.period.getSeconds() / rpm);
        this.spacing = Duration.ofMillis(spacingMillis); // seconds per request
    }

    public void schedule(int i) {
        // space the requests
        lock.lock();
        var now = Instant.now();
        try {
            var diff = Duration.between(this.lastTime, now);
            // if the time since the last call is less than the spacing, wait
            if (diff.compareTo(this.spacing) <= 0) {
                this.lastTime = now;
                var waitTime = this.spacing.minus(diff);
                Thread.sleep(waitTime); // this just sleeps this virtual thread. The lock prevents other thread from
                // proceeding
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.lastTime = Instant.now(); // update now after wait
            this.lock.unlock();
        }
    }

    public String toString() {
        return "RateLimiter: " + rpm + " rpm | " + spacing.toMillis() / 1000.0 + " seconds spacing";
    }
}
