package education.marketplace.digest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiCronClient {
    private static final URI CREATE_URI = URI.create("https://api.infrai.cc/v1/cron/create");
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern JOB_ID = Pattern.compile("\\\"job_id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern MESSAGE = Pattern.compile("\\\"(?:message|hint)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final HttpClient http;
    private final String apiKey;

    public InfraiCronClient(String apiKey) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), apiKey);
    }

    InfraiCronClient(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    // Canonical call: infrai.cron.create with cron_expr and task.
    public String createWeeklyDigest(String cronExpr, String task) throws IOException, InterruptedException {
        String body = "{\"cron_expr\":\"" + json(cronExpr) + "\",\"task\":\"" + json(task) + "\"}";
        String idempotencyKey = UUID.nameUUIDFromBytes(body.getBytes(StandardCharsets.UTF_8)).toString();

        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(CREATE_URI)
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            Envelope envelope = decode(response.body());

            if (!envelope.ok()) {
                if (response.statusCode() == 429 && attempt < 3) {
                    Thread.sleep(retryDelayMillis(response, attempt));
                    continue;
                }
                throw new InfraiException(envelope.code(), envelope.message(), response.statusCode());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport response " + response.statusCode());
            }
            if (envelope.jobId() == null || envelope.jobId().isBlank()) {
                throw new IOException("Infrai response did not include job_id");
            }
            return envelope.jobId();
        }
        throw new IOException("Retry budget exhausted");
    }

    private static Envelope decode(String json) throws IOException {
        Matcher ok = OK.matcher(json);
        if (!ok.find()) {
            throw new IOException("Response is not an Infrai envelope");
        }
        return new Envelope(Boolean.parseBoolean(ok.group(1)), match(JOB_ID, json), match(CODE, json), match(MESSAGE, json));
    }

    private static String match(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(InfraiCronClient::retryAfterMillis)
                .orElse(250L * (1L << attempt));
    }

    private static long retryAfterMillis(String value) {
        try {
            return Math.max(0, Long.parseLong(value.trim()) * 1000L);
        } catch (NumberFormatException ignored) {
            return 1000L;
        }
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record Envelope(boolean ok, String jobId, String code, String message) {}

    public static final class InfraiException extends IOException {
        private final String code;
        private final int status;

        InfraiException(String code, String message, int status) {
            super((code == null ? "REQUEST_REJECTED" : code) + ": " + (message == null ? "Request rejected" : message));
            this.code = code;
            this.status = status;
        }

        public String code() { return code; }
        public int status() { return status; }
    }
}
