## Overview

This connector synchronizes customer/account data between NetSuite and Salesforce using a queue-based workflow in Postgres.

## Data Model and Queueing

The connector currently uses two support tables:

- `sync_job`: worker queue for actionable sync operations
- `scheduled_sync_jobs`: metadata for scheduled sync executions

### `sync_job` tracks

- source and target systems
- record type and source/target record IDs
- operation type (`INSERT`, `UPDATE`, `RECONCILE`)
- queue status and retry info (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`,
  `SUPERSEDED`, attempts, availability, and claim metadata)

## Sync Workflow

Customer synchronization is separated into three stages:

1. `service.sync.discovery.customer` scans both systems from the persisted
   `lastSuccessfulAt` watermark and plans queue jobs.
2. `service.sync.job` owns typed routing, queue lifecycle, worker dispatch,
   completion, and retry behavior.
3. `service.sync.customer` loads linked state and delegates to one operation
   component per supported direction. Field comparison and conflict handling
   live under `customer.conflict`.

The scheduled scan uses a configurable fixed delay:

```yaml
app.sync.customer.poll-delay-ms: 10000
```

## Sync Direction and Precedence

- The connector is **bi-directional**.
- The most recently modified record wins.
- NetSuite wins when the timestamps are equal.

### Current creation behavior

- NS-only record -> create in Salesforce
- linked records -> update the older system
- SF-only record -> ignored until NetSuite customer creation is supported

Delete behavior and Salesforce-to-NetSuite creation are intentionally deferred.

## Date/Time Handling

### Salesforce query filter

For SOQL filters on `LastModifiedDate`:

- use a datetime literal
- do **not** wrap it in quotes
- expected format example: `2026-04-20T00:00:00Z`

### Salesforce response parsing

Salesforce returns values like:

`2026-04-06T15:55:16.000+0000`

Notes:

- `LocalDateTime` can fail unless format is explicitly configured
- if using `LocalDateTime`, use:
    - `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")`
- preferred long-term type: `OffsetDateTime`

### NetSuite response parsing

NetSuite date parsing issues were addressed by formatting in SuiteQL:

```sql
TO_CHAR(customer.lastmodifieddate, 'YYYY-MM-DD HH24:MI:SS') AS lastmodifieddate
