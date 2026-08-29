package education.marketplace.digest;

import java.util.List;

public record MarketplaceDigest(String audience, String subject, List<String> sections) {
    public String render() {
        return subject + "\n\n" + String.join("\n", sections);
    }
}
