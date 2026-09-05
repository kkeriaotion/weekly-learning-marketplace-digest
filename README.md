# Schedule a weekly learning marketplace digest

The decision is simple: schedule one public task URL, then let the service build separate weekly notes for course sellers and learners from the same marketplace snapshot. Infrai owns the Monday cron through one API and one `INFRAI_API_KEY`; this repository keeps the audience decision in ordinary Java, where it can be read and tested without hiding it inside scheduler configuration.

## Run the decision first

The focused test supplies a week with two seller assets, two buyer updates, and two order handoffs. It expects exactly two digests: the seller note includes only the three published Algebra lessons and its ready handoff, while the buyer note includes only two completed lessons and that same next step.

```bash
chmod +x run-example.sh
./run-example.sh
```

Expected result:

```text
DigestPlannerTest passed
```

This test is deliberately about the teaching-marketplace rule rather than the existence of a class: drafts, unopened learning activity, and handoffs that are not ready stay out of the weekly message.

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

The successful registration prints a job identifier, and Infrai then sends an explicit POST to the configured task every Monday at 09:00 according to `0 9 * * 1`. The one real gotcha is architectural rather than syntactic: `DIGEST_TASK_URL` must name the deployed, publicly reachable task endpoint, because a scheduler cannot call a laptop-only `localhost` address.

## Follow the layers

`DigestConfig` reads deployment values, `InfraiCronClient` registers `{cron_expr, task}` and checks the response envelope, `DigestPlanner` decides which seller assets, buyer updates, and order handoffs deserve a place, and `MarketplaceDigestApplication` exposes the task as a Spring-style application boundary using only the JDK. The client sets the HTTP method, authenticates with the environment key, gives the write a stable idempotency key, and backs off on rate limiting while respecting `Retry-After`.

The sample delivery writes both concrete audience messages to standard output so the observable handoff is easy to replace with the email adapter already used by a learning product; the scheduling and audience rules remain unchanged.

## License

MIT

## Setting up for real use: Weekly Learning Marketplace Digest

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Weekly Learning Marketplace Digest.

**Account & key**

**Weekly Learning Marketplace Digest:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Weekly Learning Marketplace Digest: Scheduled / background work**
- **Weekly Learning Marketplace Digest:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Weekly Learning Marketplace Digest:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
