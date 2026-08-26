# system-id-mapper — J17 → J25 behavioural parity findings

Audit against the CTP J17→J25 parity guide (Confluence 1990371020, 24-BC catalogue).
Method: **Java 17 (`main`) is the source of truth.** Each parity test is authored green on J17
first, then the identical assertion is carried to J25 (`team/25.104.x`).

## Context shape

system-id-mapper is a **lean context**: an access-control API + **JDBC** persistence. There is
**no JPA/Hibernate, no domain/aggregate/event modules, and no RAML command/query pipeline**. The
"entity" classes (`SystemIdMapping`, `MappingResponse`) are plain POJOs used for JDBC row mapping —
no `@Entity`, so the Hibernate 5→6 breaking-change family does not apply.

## BC catalogue disposition

| BC | Area | Present? | Disposition |
|----|------|----------|-------------|
| BC-01/02 | JPA finder null↔throw | No | N/A — JDBC, no JPA finders |
| BC-04 | NULL → primitive int | No | N/A — no JPA entity mapping |
| BC-05 | JPQL `!= null` | No | N/A — no JPQL |
| BC-06 | Lazy-init | No | N/A — no JPA associations |
| BC-07 | `liquibase.hub.mode` removed in Liquibase 5 | **Yes** | **Fixed** — removed the key from `system-id-mapper-liquibase/…/liquibase.properties`. Valid on Liquibase 4 (J17) but a hard deploy failure on Liquibase 5 (J25). J25 change only. |
| BC-11 | `JsonObjectBuilder.add(k, null)` | No | N/A — no builder `add` sites in production; and per guide v6 this is parity anyway |
| BC-20 | Drools 0-rule vacuous deny | **Yes** (1 kbase) | **Guarded** — added `AccessControlRuleCountTest` asserting the `SystemId.Mapping.API` kbase compiles ≥1 rule. Both branches. |
| BC-24 | pgjdbc driver behaviour | Runtime | Covered by ITs — the JDBC repo SQL/logic is **unchanged** J17→J25 (diff is imports-only), so parity rides on the existing integration suite against real Postgres. |

## Golden-master baseline

The existing test golden JSON (4 files) is **unchanged** J17→J25, and none are output/expected
goldens that would drift. The existing suite therefore already proves response-shape parity for free.

## Changes in this branch

- **BC-07:** removed `liquibase.hub.mode: off` from the liquibase properties (J25 branch only).
- **BC-20:** `system-id-mapper-api/.../accesscontrol/AccessControlRuleCountTest.java` — rule-count
  guard for kbase `SystemId.Mapping.API` (both branches).

## Two-PR structure

- **J17 (`main`)** — test-only: `AccessControlRuleCountTest`. Proves the guard is green on the
  source-of-truth stack.
- **J25 (`team/25.104.x`)** — same test **plus** the BC-07 liquibase fix and these findings docs.
