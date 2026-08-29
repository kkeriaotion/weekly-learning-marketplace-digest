package education.marketplace.digest;

import java.util.ArrayList;
import java.util.List;

public final class DigestPlanner {
    public record SellerAsset(String courseTitle, int publishedLessons) {}
    public record BuyerUpdate(String courseTitle, int completedLessons) {}
    public record OrderHandoff(String courseTitle, String nextStep, boolean ready) {}
    public record MarketplaceWeek(List<SellerAsset> sellerAssets, List<BuyerUpdate> buyerUpdates,
                                  List<OrderHandoff> orderHandoffs) {}

    public List<MarketplaceDigest> plan(MarketplaceWeek week) {
        List<MarketplaceDigest> digests = new ArrayList<>();

        List<String> sellerSections = new ArrayList<>();
        week.sellerAssets().stream()
                .filter(asset -> asset.publishedLessons() > 0)
                .forEach(asset -> sellerSections.add(asset.courseTitle() + ": "
                        + asset.publishedLessons() + " lessons ready for learners"));
        week.orderHandoffs().stream()
                .filter(OrderHandoff::ready)
                .forEach(order -> sellerSections.add(order.courseTitle() + ": handoff " + order.nextStep()));
        if (!sellerSections.isEmpty()) {
            digests.add(new MarketplaceDigest("sellers", "Your teaching marketplace week", sellerSections));
        }

        List<String> buyerSections = new ArrayList<>();
        week.buyerUpdates().stream()
                .filter(update -> update.completedLessons() > 0)
                .forEach(update -> buyerSections.add(update.courseTitle() + ": "
                        + update.completedLessons() + " lessons completed"));
        week.orderHandoffs().stream()
                .filter(OrderHandoff::ready)
                .forEach(order -> buyerSections.add(order.courseTitle() + ": next step " + order.nextStep()));
        if (!buyerSections.isEmpty()) {
            digests.add(new MarketplaceDigest("buyers", "Your learning marketplace week", buyerSections));
        }
        return List.copyOf(digests);
    }
}
