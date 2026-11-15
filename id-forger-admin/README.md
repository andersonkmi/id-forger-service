# ID Forger Admin

The series management service of [ID Forger](../README.md). It creates, inspects, resets and deletes the **series** that the [`id-forger-api`](../id-forger-api/README.md) service generates IDs from.

A series is a PostgreSQL sequence named `idfs_<name>` plus a prefix, registered in the `series_details` table. Both services share the same database, and neither caches series data, so every change made here takes effect in the API on its next request.

| | |
| --- | --- |
| Port | `27111` (override with `SERVER_PORT`) |
| Base path | `/idgenerator/v1/series` |
| OpenAPI spec | [`docs/openapi.yaml`](docs/openapi.yaml) |
| Artifact | `build/libs/id-forger-admin-1.2.0.jar` |
| Docker image | `codecraftlabs/id-forger-admin:1.2.0` |
| Main class | `org.codecraftlabs.idgenerator.admin.IdForgerAdminApplication` |

> **Security: this service has no authentication yet.** Anyone who can reach it can create, delete or reset series. Resetting a sequence to a lower value makes the API issue IDs that were already handed out. Keep it on a private network (gateway, firewall or private subnet) and expose only `id-forger-api` publicly.

---

## How it works

* `SequenceAdminController` validates requests (Jakarta Bean Validation) and maps service exceptions to HTTP status codes.
* `SequenceAdminService` resolves series names through the live `series_details` mapping and translates repository exceptions.
* `SequenceAdminRepository` wraps the `SequenceAdminMapper` MyBatis mapper. Creating a series (`CREATE SEQUENCE` + insert into `series_details` + read-back) and deleting one (`DROP SEQUENCE` + delete from `series_details`) each run in a single transaction, so a failure leaves nothing half-done.
* Every endpoint is protected by a Resilience4j **semaphore bulkhead** named `admin` (8 concurrent calls, no waiting). Excess calls fail fast with `503 Service Unavailable` and a `Retry-After: 1` header, so bursts of admin traffic cannot exhaust database connections shared with the ID generation service.

### Source layout

```
src/main/java/org/codecraftlabs/idgenerator/admin
├── IdForgerAdminApplication.java
├── controller/   SequenceAdminController, Series, SeriesLastValue, SequenceDataResponse
├── service/      SequenceAdminService, SeriesData + service exceptions
└── repository/   SequenceAdminMapper (MyBatis), SequenceAdminRepository, Sequence, SeriesDetail
```

Shared classes (API versioning, CORS, `IdResponse`, common exceptions, database credentials check) come from [`id-forger-common`](../id-forger-common/README.md).

---

## Running

The service needs a PostgreSQL database with the shared schema from [`docker/pgsql/init.sql`](../docker/pgsql/init.sql).

### With Docker Compose (whole stack)

From the repository root:

```bash
./gradlew :id-forger-admin:build
docker compose -f docker/docker-compose.yml up -d --build postgres id-forger-admin
```

### Locally with Gradle

Start only the database, then run the service with the `local` profile (which `bootRun` activates by default):

```bash
docker compose -f docker/docker-compose.yml up -d postgres
./gradlew :id-forger-admin:bootRun
```

### With Java

```bash
./gradlew :id-forger-admin:build
java -jar id-forger-admin/build/libs/id-forger-admin-1.2.0.jar --spring.profiles.active=local
```

### With Docker (this service only)

```bash
./gradlew :id-forger-admin:build
docker build -t codecraftlabs/id-forger-admin:1.2.0 id-forger-admin
docker run -d --name id-forger-admin -p 27111:27111 \
  -e DB_HOST=host.docker.internal -e DB_USER=idgen -e DB_PASSWORD=idgen \
  codecraftlabs/id-forger-admin:1.2.0
```

### Configuration

| Environment variable   | Default       | Description                                                                 |
| ---------------------- | ------------- | --------------------------------------------------------------------------- |
| `DB_HOST`              | `localhost`   | PostgreSQL host                                                             |
| `DB_PORT`              | `5432`        | PostgreSQL port                                                             |
| `DB_NAME`              | `idgenerator` | Database name                                                               |
| `DB_USER`              | —             | Database user. **Required** outside the `local` profile                     |
| `DB_PASSWORD`          | —             | Database password. **Required** outside the `local` profile                 |
| `SERVER_PORT`          | `27111`       | HTTP port                                                                   |
| `CORS_ALLOWED_ORIGINS` | *(empty)*     | Comma-separated origins allowed to call `/idgenerator/**`; empty disables CORS |

The `local` profile (`application-local.properties`) defaults `DB_USER`/`DB_PASSWORD` to `idgen`/`idgen` and includes error messages in responses. Never activate it in production. Outside it, the service refuses to start without database credentials.

| Property                                                   | Default | Description                                   |
| ---------------------------------------------------------- | ------- | --------------------------------------------- |
| `resilience4j.bulkhead.instances.admin.max-concurrent-calls` | `8`     | Concurrent admin calls before `503`         |
| `resilience4j.bulkhead.instances.admin.max-wait-duration`    | `0`     | How long an excess call waits for a slot    |
| `spring.datasource.hikari.maximum-pool-size`                 | `10`    | Database connections (sized for the bulkhead) |

Spring Boot properties can also be set as environment variables, e.g. `RESILIENCE4J_BULKHEAD_INSTANCES_ADMIN_MAXCONCURRENTCALLS=4`.

### Logging

Logs go to the console and to `logs/id-forger-admin.log`, relative to the working directory. Rotation and retention work the same as in the API service and use the same `LOGGING_LOGBACK_ROLLINGPOLICY_*` variables.

Client errors (unknown series, existing series) are logged as a single `WARN` line. Server-side failures are logged at `ERROR` with the stack trace.

---

## API

The full contract is in [`docs/openapi.yaml`](docs/openapi.yaml). The examples use the `default` and `product` series that `init.sql` creates.

### Create a series

Only `name` and `prefix` are required. Numeric options that are left out fall back to the database defaults (start/min `1`, increment `1`, max `9223372036854775807`).

```bash
curl -X POST http://localhost:27111/idgenerator/v1/series \
  -H "Content-Type: application/json" \
  -d '{
        "name": "orders",
        "prefix": "ORD",
        "startingValue": 500,
        "incrementBy": 5,
        "minimumValue": 500,
        "maximumValue": 10000
      }'
```

Response (`201 Created`):

```json
{
  "name": "orders",
  "prefix": "ORD",
  "startingValue": 500,
  "minimumValue": 500,
  "maximumValue": 10000,
  "incrementBy": 5
}
```

Validation rules:

* `name`: lowercase letters and digits only, up to 50 characters
* `prefix`: letters and digits only, up to 10 characters
* `startingValue`, `minimumValue`, `maximumValue`: zero or positive
* `incrementBy`: positive

Invalid fields return `400 Bad Request` with a map of field errors, e.g.:

```json
{ "name": "name must contain only lowercase letters and digits" }
```

Creating a series that already exists returns `409 Conflict`.

### Get the current value of a series

Returns the last generated value without advancing the sequence. Returns `400` if no ID has been generated from the series yet.

```bash
curl http://localhost:27111/idgenerator/v1/series/default/currentValue
```

```json
{ "id": "42" }
```

### Get series details

```bash
curl http://localhost:27111/idgenerator/v1/series/product/details
```

```json
{
  "schema": "public",
  "name": "idfs_product",
  "startValue": 1451000000,
  "minValue": 1451000000,
  "maxValue": 1451999999,
  "incrementBy": 1,
  "cacheSize": 1,
  "cycle": false,
  "lastValue": 1451000042
}
```

### Reset the last value of a series

The next generated ID will be `newLastValue + incrementBy`. The value must stay within the series' min/max range, otherwise `400 Bad Request` is returned.

```bash
curl -X PATCH http://localhost:27111/idgenerator/v1/series/product \
  -H "Content-Type: application/json" \
  -d '{ "newLastValue": 1451500000 }'
```

```json
{ "id": "1451500000" }
```

> Resetting to a lower value than the current one makes the API hand out IDs that were already issued.

### Delete a series

Removes the series together with its backing database sequence. The request has no body, so no `Content-Type` header is needed.

```bash
curl -i -X DELETE http://localhost:27111/idgenerator/v1/series/orders
```

Response: `204 No Content` with an empty body. An unknown series returns `404 Not Found`.

### Health check

```bash
curl http://localhost:27111/actuator/health
```

```json
{ "status": "UP" }
```

### Error responses

| Status                        | When                                                                 |
| ----------------------------- | -------------------------------------------------------------------- |
| `400 Bad Request`             | Validation errors, out-of-range values, unsupported API version, current value of an unused series |
| `404 Not Found`               | Unknown series                                                       |
| `409 Conflict`                | Creating a series that already exists                                |
| `415 Unsupported Media Type`  | Missing or non-JSON `Content-Type` on POST/PATCH                     |
| `503 Service Unavailable`     | Bulkhead full; retry after the `Retry-After` hint                    |

> Error messages are only included in the response body when the `local` profile is active.

---

## Tests

| Suite                | Location                     | Command                                           | Needs                        |
| -------------------- | ---------------------------- | ------------------------------------------------- | ---------------------------- |
| Unit tests           | `src/test/java`              | `./gradlew :id-forger-admin:test`                 | —                            |
| Integration tests    | `src/integrationTest/java` (tag `integration`) | `./gradlew :id-forger-admin:integrationTest` | Docker                     |
| Load test            | `src/integrationTest/java` (tag `load`)        | `./gradlew :id-forger-admin:loadTest --rerun` | A running service          |

**Unit tests** cover the controller (MockMvc with the real API versioning strategy, validation, status mapping, bulkhead `503` handling, log levels), the service, and the repository's transactions and SQLState-based exception translation.

**Integration tests** (`SeriesAdminIntegrationTest`) boot the service on a random port against a `postgres:17.5` Testcontainer seeded with `docker/pgsql/init.sql`. They cover create (including defaults, validation, `409` and `415`), details, current value, reset (including out-of-range), delete and re-create, API versioning and the health endpoint. ID generation is not part of this service, so tests that need to consume IDs advance the sequence in the database with the same statement the API uses. They also check that `/ids` is *not* exposed here.

**Load test** (`SeriesAdminLoadTest`) sends concurrent requests (50 threads) to the read-only `GET /series/default/details` endpoint on a running instance, so it never changes series data. Every call must either succeed (`200`) or be rejected by the bulkhead (`503` with `Retry-After`); any other outcome (timeouts, other errors) fails the test. The `=== Results ===` block shows how many calls the bulkhead rejected.

```bash
docker compose -f docker/docker-compose.yml up -d --build
./gradlew :id-forger-admin:loadTest --rerun
```

| Property               | Default                  |
| ---------------------- | ------------------------ |
| `loadTest.baseUrl`     | `http://localhost:27111` |
| `loadTest.totalCalls`  | `2000`                   |
