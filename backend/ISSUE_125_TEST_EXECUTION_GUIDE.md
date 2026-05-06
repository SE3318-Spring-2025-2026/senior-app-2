# Issue #125: Test Execution Guide

## Quick Start

### Prerequisites
- Java 11+
- Maven 3.6+
- Spring Boot 3.0+
- H2 Database (included with Spring Boot test dependencies)

### Install Dependencies
```bash
cd backend
mvn clean install
```

---

## Running Tests

### 1. Run All Issue #125 Tests
```bash
mvn test -Dtest=Issue125SyncIntegrationTest
```

**Expected Output:**
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.seniorapp.Issue125SyncIntegrationTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.345 s
[INFO] BUILD SUCCESS
```

### 2. Run Specific Test Case
```bash
# Success scenario test
mvn test -Dtest=Issue125SyncIntegrationTest#testSyncInitiation_Success_GitHubSource_Returns202AndPersistedAuditLog

# Timeout scenario test
mvn test -Dtest=Issue125SyncIntegrationTest#testSyncInitiation_Failure_GitHubTimeout_Returns500

# Full flow with fixtures test
mvn test -Dtest=Issue125SyncIntegrationTest#testSyncInitiation_FullFlow_WithFixtureData
```

### 3. Run with Verbose Output
```bash
mvn test -Dtest=Issue125SyncIntegrationTest -X
```

### 4. Run with Code Coverage
```bash
mvn test -Dtest=Issue125SyncIntegrationTest jacoco:report
# Coverage report: target/site/jacoco/index.html
```

---

## Test Cases Overview

### Test 1: Success - GitHub Sync with AuditLog Persistence
```bash
mvn test -Dtest=Issue125SyncIntegrationTest#testSyncInitiation_Success_GitHubSource_Returns202AndPersistedAuditLog
```

**What it tests:**
- ✅ Valid GitHub sync request
- ✅ Returns 202 Accepted
- ✅ AuditLog persisted to database
- ✅ Audit log contains correct data

**Expected output:**
```
testSyncInitiation_Success_GitHubSource_Returns202AndPersistedAuditLog
[INFO] Tests run: 1, Failures: 0, Errors: 0, Time elapsed: 2.123 s - in com.seniorapp.Issue125SyncIntegrationTest
[INFO] BUILD SUCCESS
```

### Test 2: Success - Both GitHub + Jira Sync
```bash
mvn test -Dtest=Issue125SyncIntegrationTest#testSyncInitiation_Success_BothSources_Returns202
```

**What it tests:**
- ✅ Sync with both GitHub and Jira
- ✅ Returns 202 Accepted
- ✅ Audit log mentions both sources

### Test 3: Failure - GitHub API Timeout
```bash
mvn test -Dtest=Issue125SyncIntegrationTest#testSyncInitiation_Failure_GitHubTimeout_Returns500
```

**What it tests:**
- ❌ TimeoutException from GitHub API (30+ seconds)
- ❌ Returns 500 Internal Server Error
- ❌ Error is logged gracefully
- ❌ System doesn't crash

**Mock behavior:**
```
SyncService.initiateSync() throws TimeoutException
```

### Test 4: Failure - Invalid Request (Missing groupId)
```bash
mvn test -Dtest=Issue125SyncIntegrationTest#testSyncInitiation_Failure_MissingGroupId_Returns400
```

**What it tests:**
- ❌ Request validation
- ❌ Returns 400 Bad Request
- ❌ No audit log created for validation errors

### Test 5: Failure - No Authentication
```bash
mvn test -Dtest=Issue125SyncIntegrationTest#testSyncInitiation_Failure_NoAuthentication_Returns401
```

**What it tests:**
- ❌ Missing authentication token
- ❌ Returns 401 Unauthorized
- ❌ No access to endpoint without auth

### Test 6: Full Flow with Fixture Data
```bash
mvn test -Dtest=Issue125SyncIntegrationTest#testSyncInitiation_FullFlow_WithFixtureData
```

**What it tests:**
- ✅ End-to-end integration
- ✅ Deterministic fixture data (no real API calls)
- ✅ Audit trail with user information
- ✅ Response contains valid job ID

**Fixture data:**
```json
{
  "repositories": [
    {"id": 1, "name": "repo-1", "language": "Java", "stars": 42},
    {"id": 2, "name": "repo-2", "language": "TypeScript", "stars": 18},
    {"id": 3, "name": "repo-3", "language": "Python", "stars": 99}
  ],
  "commits": 245,
  "pullRequests": 12,
  "issues": 3
}
```

### Test 7: Audit Trail - Multiple Syncs
```bash
mvn test -Dtest=Issue125SyncIntegrationTest#testAuditTrail_MultipleSyncRequests_AllPersisted
```

**What it tests:**
- ✅ Multiple sync requests create separate audit logs
- ✅ All logs are queryable by module
- ✅ Each sync gets its own audit record

### Test 8: System Recovery After Timeout
```bash
mvn test -Dtest=Issue125SyncIntegrationTest#testRecovery_AfterTimeout_NextSyncSucceeds
```

**What it tests:**
- 🔄 First request: Timeout → 500 error
- 🔄 Second request: Success → 202 accepted
- 🔄 No cascading failures

---

## Manual API Testing with cURL

### 1. Get JWT Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testteamlead@example.com",
    "password": "password123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "email": "testteamlead@example.com",
    "fullName": "Test Team Leader",
    "role": "TEAM_LEADER"
  }
}
```

### 2. Initiate Sync - GitHub Only
```bash
curl -X POST http://localhost:8080/api/ingestion/sync \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "source": "GITHUB",
    "groupId": "group-123",
    "integrationTokens": {
      "pat": "ghp_xxx...",
      "oauth": null
    }
  }'
```

**Response (202 Accepted):**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "accepted",
  "createdAt": "2024-05-06T10:30:00Z",
  "message": "Sync job initiated for group: group-123"
}
```

### 3. Initiate Sync - Both GitHub & Jira
```bash
curl -X POST http://localhost:8080/api/ingestion/sync \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "source": "BOTH",
    "groupId": "group-123",
    "integrationTokens": {
      "pat": "ghp_xxx...",
      "oauth": "Bearer oauth_token..."
    }
  }'
```

**Response (202 Accepted):**
```json
{
  "jobId": "660f9501-f40c-52e5-b827-557766551111",
  "status": "accepted",
  "createdAt": "2024-05-06T10:31:00Z",
  "message": "Sync job initiated for group: group-123"
}
```

### 4. Check Sync Job Status
```bash
curl -X GET "http://localhost:8080/api/ingestion/status/550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer $TOKEN"
```

**Response:**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "running|completed|failed",
  "message": "Sync job is in progress"
}
```

### 5. Invalid Request - Missing groupId
```bash
curl -X POST http://localhost:8080/api/ingestion/sync \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "source": "GITHUB"
  }'
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2024-05-06T10:32:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "groupId: must not be blank"
}
```

### 6. No Authentication
```bash
curl -X POST http://localhost:8080/api/ingestion/sync \
  -H "Content-Type: application/json" \
  -d '{
    "source": "GITHUB",
    "groupId": "group-123"
  }'
```

**Response (401 Unauthorized):**
```json
{
  "timestamp": "2024-05-06T10:33:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Unauthorized"
}
```

---

## Verifying Database Persistence

### Check AuditLog in H2 Console

1. Start application in dev mode:
```bash
mvn spring-boot:run
```

2. Open H2 Console:
```
http://localhost:8080/h2-console
```

3. Default credentials (if configured):
```
URL: jdbc:h2:mem:testdb
User: sa
Password: (empty)
```

4. Query audit logs:
```sql
SELECT * FROM audit_log 
WHERE module = 'ingestion' 
ORDER BY created_at DESC;
```

5. Expected output:
```
| id | user_id | user_role    | module    | action           | status  | severity | message                                    | created_at          |
|----|---------|--------------|-----------|------------------|---------|----------|----------------------------------------------|---------------------|
| 1  | 1       | TEAM_LEADER  | ingestion | sync_initiated   | success | info     | Data sync initiated for groupId=group-123   | 2024-05-06 10:30:00 |
| 2  | 1       | TEAM_LEADER  | ingestion | sync_initiated   | success | info     | Data sync initiated for groupId=group-456   | 2024-05-06 10:31:00 |
```

---

## Test Output Example

### Successful Test Run
```bash
$ mvn test -Dtest=Issue125SyncIntegrationTest

[INFO] Scanning for projects...
[INFO] 
[INFO] --------< com.seniorapp:senior-app-backend >--------
[INFO] Building Senior App Backend 1.0-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-surefire-plugin:2.22.2:test (default-test) @ senior-app-backend ---
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.seniorapp.Issue125SyncIntegrationTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 14.567 s - in com.seniorapp.Issue125SyncIntegrationTest
[INFO] 
[INFO] -------------------------------------------------------
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] -------------------------------------------------------
[INFO] 
[INFO] BUILD SUCCESS
[INFO] 
[INFO] Total time:  22.345 s
[INFO] Total CPU time:  45.678 s
```

### Individual Test Results
```
testSyncInitiation_Success_GitHubSource_Returns202AndPersistedAuditLog ............... PASSED (1.234 s)
testSyncInitiation_Success_BothSources_Returns202 ................................. PASSED (0.987 s)
testSyncInitiation_Failure_GitHubTimeout_Returns500 ............................... PASSED (2.345 s)
testSyncInitiation_Failure_MissingGroupId_Returns400 .............................. PASSED (0.567 s)
testSyncInitiation_Failure_NoAuthentication_Returns401 ............................ PASSED (0.456 s)
testSyncInitiation_FullFlow_WithFixtureData ....................................... PASSED (1.123 s)
testAuditTrail_MultipleSyncRequests_AllPersisted .................................. PASSED (1.789 s)
testRecovery_AfterTimeout_NextSyncSucceeds ........................................ PASSED (3.456 s)
```

---

## Troubleshooting

### Test Fails: "No qualifying bean of type 'SyncService'"
**Solution:** Ensure `@MockBean` is importing from Spring Boot Test
```java
import org.springframework.boot.test.mock.mockito.MockBean;
```

### Test Fails: "Timeout waiting for response"
**Solution:** Increase test timeout in Maven Surefire plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Dorg.apache.commons.logging.simplelog.defaultlog=debug</argLine>
        <forkedProcessTimeoutInSeconds>60</forkedProcessTimeoutInSeconds>
    </configuration>
</plugin>
```

### Test Fails: "AuditLog not found in database"
**Solution:** Check that LogService is being called and H2 database is properly configured
```bash
# Enable SQL logging
mvn test -Dtest=Issue125SyncIntegrationTest \
  -Dspring.jpa.show-sql=true \
  -Dspring.jpa.properties.hibernate.format_sql=true
```

### Test Fails: "Mock not configured"
**Solution:** Ensure `doReturn()` or `doThrow()` is called in test setup
```java
@BeforeEach
void setUp() {
    SyncResponse mockResponse = new SyncResponse();
    doReturn(mockResponse)
        .when(syncService)
        .initiateSync(any(SyncRequest.class), anyLong(), anyString());
}
```

---

## CI/CD Integration

### GitHub Actions
```yaml
name: Issue #125 Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '11'
      - run: cd backend && mvn test -Dtest=Issue125SyncIntegrationTest
      - run: cd backend && mvn jacoco:report
      - uses: codecov/codecov-action@v2
```

### Jenkins
```groovy
pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                sh 'cd backend && mvn test -Dtest=Issue125SyncIntegrationTest'
            }
        }
        stage('Coverage') {
            steps {
                sh 'cd backend && mvn jacoco:report'
                publishHTML([
                    reportDir: 'backend/target/site/jacoco',
                    reportFiles: 'index.html',
                    reportName: 'Code Coverage Report'
                ])
            }
        }
    }
}
```

---

## Performance Benchmarking

### Measure Test Execution Time
```bash
# Time all tests
time mvn test -Dtest=Issue125SyncIntegrationTest

# Expected: ~15 seconds total
# Per test: ~1-3 seconds
```

### Profile Test Execution
```bash
mvn test -Dtest=Issue125SyncIntegrationTest \
  -DargLine="-Xmx512m" \
  --profile code-coverage
```

---

## Success Indicators

✅ **All tests pass** - 8/8 tests pass without errors
✅ **Fast execution** - Complete suite runs in <20 seconds
✅ **No real API calls** - All HTTP requests are mocked
✅ **Database verified** - AuditLog persisted successfully
✅ **Error handling works** - Timeouts handled gracefully
✅ **Coverage good** - 80%+ code coverage

---

## Next Steps

1. **Integrate into CI/CD** - Add to GitHub Actions or Jenkins
2. **Run nightly** - Schedule daily test runs
3. **Monitor coverage** - Track code coverage metrics
4. **Add load tests** - Test with concurrent sync requests
5. **Performance tuning** - Optimize async sync processing

---

For detailed documentation, see [ISSUE_125_TEST_DOCUMENTATION.md](ISSUE_125_TEST_DOCUMENTATION.md)
