package education.marketplace.digest;

import java.util.List;

public final class DigestPlannerTest {
    public static void main(String[] args) {
        DigestPlanner planner = new DigestPlanner();
        DigestPlanner.MarketplaceWeek week = new DigestPlanner.MarketplaceWeek(
                List.of(
                        new DigestPlanner.SellerAsset("Algebra Foundations", 3),
                        new DigestPlanner.SellerAsset("Draft Study Skills", 0)),
                List.of(
                        new DigestPlanner.BuyerUpdate("Algebra Foundations", 2),
                        new DigestPlanner.BuyerUpdate("Unopened Writing Lab", 0)),
                List.of(
                        new DigestPlanner.OrderHandoff("Algebra Foundations", "download the practice set", true),
                        new DigestPlanner.OrderHandoff("Writing Lab", "await instructor review", false)));

        List<MarketplaceDigest> result = planner.plan(week);

        check(result.size() == 2, "seller and buyer digests should both be planned");
        check(result.get(0).sections().equals(List.of(
                "Algebra Foundations: 3 lessons ready for learners",
                "Algebra Foundations: handoff download the practice set")),
                "seller digest should contain published assets and ready handoffs only");
        check(result.get(1).sections().equals(List.of(
                "Algebra Foundations: 2 lessons completed",
                "Algebra Foundations: next step download the practice set")),
                "buyer digest should contain learning progress and ready handoffs only");
        System.out.println("DigestPlannerTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
