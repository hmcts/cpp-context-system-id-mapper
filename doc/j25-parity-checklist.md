# system-id-mapper — J25 parity checklist

Per CTP parity guide (Confluence 1990371020). J17 = source of truth. See `J25-PARITY-FINDINGS.md`
for full reasoning.

- [x] **Scan for BC triggers** — JDBC (no JPA), 1 Drools kbase, 1 liquibase.properties, no
      JsonObjectBuilder `add` sites, JDBC SQL unchanged J17→J25.
- [x] **BC-07** liquibase.hub.mode — removed from `system-id-mapper-liquibase` properties (J25 only).
- [x] **BC-20** Drools 0-rule guard — `AccessControlRuleCountTest` on kbase `SystemId.Mapping.API`
      (both branches).
- [x] **BC-01/02/04/05/06** JPA/Hibernate family — N/A (JDBC persistence, no `@Entity`).
- [x] **BC-11** JsonObjectBuilder null-add — N/A (no production add sites; parity per guide v6).
- [x] **BC-24** pgjdbc — covered by existing ITs; JDBC repo SQL unchanged.
- [x] **Golden master** — 4 test JSON unchanged; existing suite proves response parity.
- [ ] **J17 rit green** (JDK17 + cpp-developers-docker java-17).
- [ ] **J25 rit green** (JDK25 + cpp-developers-docker java-25).
- [ ] **Two PRs open** — J17 test-only vs `main`; J25 tests+fix vs `team/25.104.x`.
