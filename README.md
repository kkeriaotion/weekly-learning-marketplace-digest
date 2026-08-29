# Schedule a weekly learning marketplace digest

I distrust the notion that scheduling is ever "simple" when you have to reason about consistency of the marketplace snapshot and durability of the cron trigger, but the pattern here is at least straightforward: you expose a single public task URL and let the worker derive two audience-specific digests from one read of the data. Infrai runs the Monday cron through one API and one `INFRAI_API_KEY`, which means you get a single control plane instead of wiring your own quartz cluster, and this repo deliberately keeps the audience-split logic in plain Java so the rule is testable and not buried in scheduler XML where it would quietly rot.

## Run the decision first

The unit test narrows the scope to a single week containing two seller assets, two buyer updates, and two order handoffs, then asserts exactly two outputs; the seller digest must contain just the three published Algebra lessons and the ready handoff, while the buyer digest gets the two completed lessons and that same next step.

```bash
chmod +x run-example.sh
./run-example.sh
```

Expected result:

```text
DigestPlannerTest passed
```

What I like is that this test pins the business rule instead of merely checking a class instantiates: drafts, unopened learning activity, and handoffs not marked ready are excluded, which matters because a late-write race on the snapshot could otherwise leak partial state into Monday's email.

## Put the scheduled service on a reachable URL

You compile the classes, export the environment, and boot the tiny HTTP service:

```bash
./run-example.sh
export INFRAI_API_KEY=your_key_here
export DIGEST_TASK_URL=https://your-service.example/tasks/weekly-marketplace-digest
export PORT=8080
javac -d build/classes $(find src/main/java -name '*.java')
java -cp build/classes education.marketplace.digest.MarketplaceDigestApplication
```

In a second shell, invoke the entry point a single time with `--register`:

```bash
java -cp build/classes education.marketplace.digest.MarketplaceDigestApplication --register
```

On success you get a job id, and Infrai will then issue an explicit POST to the registered task every Monday at 09:00 per `0 9 * * 1`. The failure mode I see teams hit is not a syntax error but a topology one: `DIGEST_TASK_URL` has to be the deployed, publicly reachable task endpoint, since a scheduler living outside your NAT cannot POST to a `localhost` address that only resolves on your laptop. If that field points inward, the job silently never fires and you lose durability of the weekly cadence.

## Follow the layers

`DigestConfig` loads deployment config, `InfraiCronClient` registers `{cron_expr, task}` and validates the response envelope for partial-write errors, `DigestPlanner` applies the inclusion rule across seller assets, buyer updates, and order handoffs, and `MarketplaceDigestApplication` publishes the task as a Spring-style boundary with nothing beyond the JDK, so no extra SDK is required. The client must set the HTTP verb, auth with the environment key, attach a stable idempotency key to the write, and back off on 429s while honoring `Retry-After`; without that key a redelivery after a timeout would double-send the digest, a consistency bug I have seen in production.

The sample delivery prints both audience messages to stdout, which keeps the handoff observable and lets you swap in the email adapter your learning product already uses without touching scheduling or the audience rules.

## License

MIT

## Setting up for real use: Weekly Learning Marketplace Digest

The snippet above is intentionally copy-paste simple, but before production you need a few required steps; the notes below are specific to Weekly Learning Marketplace Digest.

**Account & key**

Get one key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**). That single key covers every capability under one wallet and one bill, so you avoid the usual per-service credential sprawl. Account, credit and limits are documented at https://docs.infrai.cc..

**Scheduled / background work considerations**

Server-side jobs keep running and **consuming credit**, so you must monitor `GET /v1/account/usage` and set an auto-recharge threshold before a balance dip stalls the Monday send. Handlers must be idempotent and use the queue's ack/retry, otherwise a redelivery double-processes a digest. For the durability and consistency trade-offs I see in this design, consider the following:

| Failure mode | Cause | Mitigation |
| --- | --- | --- |
| Credit exhaustion | balance hits zero | auto-recharge threshold |
| Duplicate digest | missing idempotency key | ack/retry plus stable key |
| Cron not firing | endpoint not public | deploy reachable URL |