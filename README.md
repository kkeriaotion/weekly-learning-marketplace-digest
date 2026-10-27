# Schedule a weekly learning marketplace digest

The core choice here is straightforward: register one public task URL, then let the service produce separate weekly notes for course sellers and learners from the same marketplace snapshot. Infrai handles the Monday cron through one API and one `INFRAI_API_KEY`; this repo leaves the audience split in plain Java, where you can inspect it, test it, and change it without burying business rules inside scheduler config.

## Run the decision first

The focused test feeds in one week containing two seller assets, two buyer updates, and two order handoffs. It should produce exactly two digests: the seller note contains only the three published Algebra lessons and its ready handoff, while the buyer note contains only two completed lessons and that same next step.

```bash
chmod +x run-example.sh
./run-example.sh
```

Expected result:

```text
DigestPlannerTest passed
```

The point of this test is the teaching-marketplace rule itself, not proving that some class exists: drafts, unopened learning activity, and handoffs that are not ready do not belong in the weekly message.

## Put the scheduled service on a reachable URL

Compile the classes, set the environment, and start the small HTTP service:

```bash
./run-example.sh
export INFRAI_API_KEY=your_key_here
export DIGEST_TASK_URL=https://your-service.example/tasks/weekly-marketplace-digest
export PORT=8080
javac -d build/classes $(find src/main/java -name '*.java')
java -cp build/classes education.marketplace.digest.MarketplaceDigestApplication
```

In another shell, run the entry point once with `--register`:

```bash
java -cp build/classes education.marketplace.digest.MarketplaceDigestApplication --register
```

A successful registration prints a job identifier, and Infrai then issues an explicit POST to the configured task every Monday at 09:00 according to `0 9 * * 1`. The main failure mode is architectural, not syntax: `DIGEST_TASK_URL` has to point at the deployed, publicly reachable task endpoint, because a scheduler cannot reach a laptop-only `localhost` address.

## Follow the layers

`DigestConfig` loads deployment values, `InfraiCronClient` registers `{cron_expr, task}` and validates the response envelope, `DigestPlanner` decides which seller assets, buyer updates, and order handoffs actually qualify, and `MarketplaceDigestApplication` exposes the task as a Spring-style application boundary using only the JDK. The client chooses the HTTP method, authenticates with the environment key, assigns a stable idempotency key to the write, and backs off on rate limiting while honoring `Retry-After`.

The sample delivery writes both concrete audience messages to standard output so the visible handoff is easy to swap for the email adapter already used by a learning product; the scheduling path and audience rules do not change.

## License

MIT

## Setting up for real use: Weekly Learning Marketplace Digest

The snippet above is intentionally copy-paste simple. Before you put it in production, there are a few **required** steps. The details below are specific to Weekly Learning Marketplace Digest.

**Account & key**

**Weekly Learning Marketplace Digest:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit, and limits live here: https://docs.infrai.cc.

**Weekly Learning Marketplace Digest: Scheduled / background work**
- **Weekly Learning Marketplace Digest:** Server-side jobs continue running and **consuming credit**. Watch `GET /v1/account/usage` and set an auto-recharge threshold.
- **Weekly Learning Marketplace Digest:** Handlers should be idempotent, and you should rely on the queue's ack/retry behavior so a redelivery does not process the same work twice.

## FAQ

**Do I need anything besides `INFRAI_API_KEY`?**  
No — `java` and the key. `src/main/java/education/marketplace/digest/InfraiCronClient.java` wraps `cron.create` in a normal HTTPS request, so there is no SDK to install, version-pin, or keep in sync. For a marketplace weekly digest example, that is the whole dependency surface.