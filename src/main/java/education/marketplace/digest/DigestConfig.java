package education.marketplace.digest;

public record DigestConfig(String apiKey, String publicTaskUrl, int port) {
    public static DigestConfig fromEnvironment() {
        String key = required("INFRAI_API_KEY");
        String taskUrl = required("DIGEST_TASK_URL");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        return new DigestConfig(key, taskUrl, port);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + " before starting the service");
        }
        return value;
    }
}
