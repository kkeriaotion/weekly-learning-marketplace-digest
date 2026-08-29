package education.marketplace.digest;

import java.util.List;

public final class DigestDeliveryService {
    private final DigestPlanner planner;

    public DigestDeliveryService(DigestPlanner planner) {
        this.planner = planner;
    }

    public List<MarketplaceDigest> sendWeeklyDigest() {
        DigestPlanner.MarketplaceWeek week = new DigestPlanner.MarketplaceWeek(
                List.of(new DigestPlanner.SellerAsset("Designing Clear Lessons", 4)),
                List.of(new DigestPlanner.BuyerUpdate("Designing Clear Lessons", 2)),
                List.of(new DigestPlanner.OrderHandoff("Designing Clear Lessons", "open the instructor notes", true)));
        List<MarketplaceDigest> digests = planner.plan(week);
        digests.forEach(digest -> System.out.println("Sent to " + digest.audience() + "\n" + digest.render()));
        return digests;
    }
}
