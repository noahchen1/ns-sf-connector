## Overview

This connector synchronizes customer/account data between NetSuite and Salesforce using a queue-based workflow in Postgres.

## Data Model and Queueing

The connector currently uses two support tables:

- `sync_job`: worker queue for actionable sync operations
- `scheduled_sync_jobs`: metadata for scheduled sync executions

### `sync_job` tracks

- source and target systems
- record type and source/target record IDs
- operation type (`INSERT`, `UPDATE`)
- queue status and retry info (`PENDING`, attempts, availability, claim metadata)

## Sync Direction and Precedence

- The connector is **bi-directional**.
- If both systems modify the same mapped record, **NetSuite wins**.

### Current creation behavior

- NS-only record -> create in Salesforce
- SF-only record -> create in NetSuite

> Delete behavior is intentionally deferred for now.

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