package education.marketplace.digest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class MarketplaceDigestApplication {
    private MarketplaceDigestApplication() {}

    public static void main(String[] args) throws Exception {
        DigestConfig config = DigestConfig.fromEnvironment();
        if (args.length == 1 && args[0].equals("--register")) {
            String jobId = new InfraiCronClient(config.apiKey())
                    .createWeeklyDigest("0 9 * * 1", config.publicTaskUrl());
            System.out.println("Weekly marketplace digest scheduled: " + jobId);
            return;
        }

        DigestDeliveryService delivery = new DigestDeliveryService(new DigestPlanner());
        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/tasks/weekly-marketplace-digest", exchange -> handle(exchange, delivery));
        server.start();
        System.out.println("Digest task listening on port " + config.port());
    }

    private static void handle(HttpExchange exchange, DigestDeliveryService delivery) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            respond(exchange, 405, "{\"sent\":false}");
            return;
        }
        int count = delivery.sendWeeklyDigest().size();
        respond(exchange, 200, "{\"sent\":true,\"audiences\":" + count + "}");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
