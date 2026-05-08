# Integration Test Suite — senior-app-2 Backend

## Test Environment

- **Framework:** JUnit 5 + Spring Boot Test + MockMvc
- **Database:** H2 in-memory (`jdbc:h2:mem:testdb`) — no MySQL required
- **Isolation:** `@Transactional` rolls back after each test
- **Auth simulation:** `@WithMockUser`

---

## Infrastructure Changes

Three files were modified to make tests work. No production logic was changed.

| File | Change |
|---|---|
| `pom.xml` | Added H2 dependency (`scope: test`) |
| `src/test/resources/application-test.properties` | H2 datasource config for test profile |
| `src/main/java/.../ProjectSchemaCompatibilityMigration.java` | Added `@Profile("!test")` — prevents MySQL-specific SQL from running on H2 |

---

## Test Files

### ProjectCreationIntegrationTest.java
**Endpoints:** `POST /api/projects`, `GET /api/project-templates/{id}`

| Test | Expected |
|---|---|
| Happy path — valid template | 200 OK |
| Missing required fields | 400 Bad Request |
| Invalid template ID | 400 Bad Request |
| STUDENT role (unauthorized) | 403 Forbidden |
| Duplicate group assignment | 400 Bad Request |

---

### GradePersistenceIntegrationTest.java
**Endpoint:** `POST /api/deliverable-submissions/{submissionId}/grades`

Test data is seeded via raw SQL (`JdbcTemplate`) to avoid deep FK chain issues on H2.

| Test | Expected |
|---|---|
| Happy path — valid grade | 200 OK + DB assertion |
| Non-existent submission ID | 400 Bad Request |
| Negative grade (bean validation) | 400 Bad Request |
| Duplicate rubric grade | 400 Bad Request |
| Authenticated user (no role restriction on controller) | 200 OK — documents current behaviour |

---

### AdvisorRequestIntegrationTest.java
**Endpoints:** `POST /api/advisor-requests`, `PUT /api/advisor-requests/decision`

> Note: `GlobalExceptionHandler` maps all `RuntimeException` to **400**, so conflict/forbidden stubs return 400 (not 409/403). Unauthenticated requests return **403** (Spring Security default).

| Test | Expected |
|---|---|
| Create request — happy path | 201 Created |
| Conflict stub | 400 Bad Request |
| Forbidden stub | 400 Bad Request |
| No authentication (create) | 403 Forbidden |
| Approve decision | 200 OK |
| Decline decision | 200 OK |
| Invalid decision type | 400 Bad Request |
| No authentication (decision) | 403 Forbidden |

---

### GradingEngineTest.java *(pre-existing)*
46 unit tests shipped with the project covering weighted aggregation, sprint contributions, coefficient effects, and rounding precision. Not written by us.

---

## Summary

| File | Tests | Status |
|---|---|---|
| `ProjectCreationIntegrationTest.java` | 5 | ✅ Pass |
| `GradePersistenceIntegrationTest.java` | 5 | ✅ Pass |
| `AdvisorRequestIntegrationTest.java` | 8 | ✅ Pass |
| `GradingEngineTest.java` *(pre-existing)* | 46 | ✅ Pass |
| **Total** | **64** | **0 failures** |

---

## Running the Tests

```bash
# Run all tests
mvn test

# Run only the new integration tests
mvn test -Dtest="ProjectCreationIntegrationTest,GradePersistenceIntegrationTest,AdvisorRequestIntegrationTest"
```
