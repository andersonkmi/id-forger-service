# ID Forger API

The ID generation service of [ID Forger](../README.md). It serves the high-volume, latency-sensitive hot path that clients call to obtain new IDs:

```
GET /idgenerator/v1/ids/{seriesName}[?format=...]
```

Series are created and managed by the separate [`id-forger-admin`](../id-forger-admin/README.md) service. Both services share one PostgreSQL database. This service only *advances* existing sequences (`nextval`); it cannot create, reset or drop them.

| | |
| --- | --- |
| Port | `27110` (override with `SERVER_PORT`) |
| Base path | `/idgenerator/v1` |
| OpenAPI spec | [`docs/openapi.yaml`](docs/openapi.yaml) |
| Artifact | `build/libs/id-forger-api-1.2.0.jar` |
| Docker image | `codecraftlabs/id-forger-api:1.2.0` |
| Main class | `org.codecraftlabs.idgenerator.api.IdForgerApiApplication` |

---

## How it works

1. `IdGenerationController` receives the request and asks `IdGenerationService` for an ID in the requested format.
2. `IdGenerationServiceFactory` picks the `IdFormatProcessor` registered under that format name (`plain` when the format is omitted or blank).
3. Every processor obtains the raw value from `IdGenerator`, which resolves the series and advances its sequence in **a single SQL statement**:

   ```sql
   SELECT nextval(sequence::regclass) AS next_value, prefix
   FROM series_details
   WHERE name = ?
   ```

   An unknown series returns no row and never consumes a sequence value. Nothing is cached, so changes made by the admin service (new series, resets, deletions, prefix changes) take effect on the next request, across any number of instances.
4. The processor formats the value, and the controller returns `{ "id": "..." }`.

Uniqueness is guaranteed by the PostgreSQL sequence. Values consumed by failed or abandoned requests are not reused, so gaps are possible.

### Source layout

```
src/main/java/org/codecraftlabs/idgenerator/api
├── IdForgerApiApplication.java
├── controller/          IdGenerationController
├── service/             IdGenerationService, IdGenerationServiceFactory, InvalidFormatException
│   └── processor/       IdGenerator + one IdFormatProcessor per format
├── repository/          UniqueIdMapper (MyBatis), UniqueIdMapperRepository, GeneratedId
└── util/                LuhnDigitNumberGenerator
```

Shared classes (API versioning, CORS, `IdResponse`, common exceptions, database credentials check) come from [`id-forger-common`](../id-forger-common/README.md).

---

## Running

The service needs a PostgreSQL database with the shared schema from [`docker/pgsql/init.sql`](../docker/pgsql/init.sql).

### With Docker Compose (whole stack)

From the repository root:

```bash
./gradlew :id-forger-api:build
docker compose -f docker/docker-compose.yml up -d --build postgres id-forger-api
```

### Locally with Gradle

Start only the database, then run the service with the `local` profile (which `bootRun` activates by default):

```bash
docker compose -f docker/docker-compose.yml up -d postgres
./gradlew :id-forger-api:bootRun
```

### With Java

```bash
./gradlew :id-forger-api:build
java -jar id-forger-api/build/libs/id-forger-api-1.2.0.jar --spring.profiles.active=local
```

### With Docker (this service only)

```bash
./gradlew :id-forger-api:build
docker build -t codecraftlabs/id-forger-api:1.2.0 id-forger-api
docker run -d --name id-forger-api -p 27110:27110 \
  -e DB_HOST=host.docker.internal -e DB_USER=idgen -e DB_PASSWORD=idgen \
  codecraftlabs/id-forger-api:1.2.0
```

### Configuration

| Environment variable   | Default       | Description                                                                 |
| ---------------------- | ------------- | --------------------------------------------------------------------------- |
| `DB_HOST`              | `localhost`   | PostgreSQL host                                                             |
| `DB_PORT`              | `5432`        | PostgreSQL port                                                             |
| `DB_NAME`              | `idgenerator` | Database name                                                               |
| `DB_USER`              | —             | Database user. **Required** outside the `local` profile                     |
| `DB_PASSWORD`          | —             | Database password. **Required** outside the `local` profile                 |
| `SERVER_PORT`          | `27110`       | HTTP port                                                                   |
| `CORS_ALLOWED_ORIGINS` | *(empty)*     | Comma-separated origins allowed to call `/idgenerator/**`; empty disables CORS |

The `local` profile (`application-local.properties`) defaults `DB_USER`/`DB_PASSWORD` to `idgen`/`idgen` and includes error messages in responses. Never activate it in production. Outside it, the service refuses to start without database credentials.

The connection pool allows up to 20 connections (`spring.datasource.hikari.maximum-pool-size`).

#### Enabling browser (CORS) access

Cross-origin requests from browsers are disabled by default. To allow them, set `CORS_ALLOWED_ORIGINS`; there is no wildcard option. At startup the service logs `CORS enabled for origins [...]`, or `No CORS origins configured` when the variable is unset.

```bash
CORS_ALLOWED_ORIGINS=http://localhost:8000 ./gradlew :id-forger-api:bootRun

# Allowed origin: 200 with an Access-Control-Allow-Origin header
curl -i -X OPTIONS http://localhost:27110/idgenerator/v1/ids/default \
  -H "Origin: http://localhost:8000" -H "Access-Control-Request-Method: GET"

# Any other origin: 403 Forbidden
curl -i -X OPTIONS http://localhost:27110/idgenerator/v1/ids/default \
  -H "Origin: http://evil.example.com" -H "Access-Control-Request-Method: GET"
```

### Logging

Logs go to the console and to `logs/id-forger-api.log`, relative to the working directory. The file rolls over daily and at `10MB`; archives go to `logs/archived/`. Retention can be tuned with `LOGGING_LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE`, `LOGGING_LOGBACK_ROLLINGPOLICY_MAX_HISTORY` (default `30` days) and `LOGGING_LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP` (default `1GB`).

Client errors (unknown series, invalid format) are logged as a single `WARN` line without a stack trace, so bad requests on the hot path cannot flood the logs. Server-side failures are logged at `ERROR` with the stack trace.

---

## API

The full contract is in [`docs/openapi.yaml`](docs/openapi.yaml). The examples use the `default` and `product` series that `init.sql` creates.

### Generate the next ID

```bash
curl http://localhost:27110/idgenerator/v1/ids/default
```

```json
{ "id": "42" }
```

### Output formats

Use the optional `format` query parameter. When omitted or blank, `plain` is used.

| Format        | Example request                     | Example output                                                     |
| ------------- | ----------------------------------- | ------------------------------------------------------------------ |
| `plain`       | `/ids/default?format=plain`         | `42`                                                               |
| `prefixed`    | `/ids/product?format=prefixed`      | `PROD000001451000042` (series prefix + value padded to 15 digits)  |
| `timestamped` | `/ids/default?format=timestamped`   | `000000000000042-1763049600000` (padded value + epoch millis)      |
| `luhn`        | `/ids/default?format=luhn`          | `422` (value + Luhn check digit)                                   |
| `base64`      | `/ids/default?format=base64`        | `NDI=` (Base64 of the plain value)                                 |
| `sha256`      | `/ids/default?format=sha256`        | `73475cb4...` (64-char hex SHA-256 of the plain value)             |

```bash
curl "http://localhost:27110/idgenerator/v1/ids/product?format=prefixed"
```

```json
{ "id": "PROD000001451000042" }
```

### Health check

```bash
curl http://localhost:27110/actuator/health
```

```json
{ "status": "UP" }
```

### Error responses

| Status            | When                                                                         |
| ----------------- | ---------------------------------------------------------------------------- |
| `400 Bad Request` | Unknown format, unsupported API version (e.g. `/idgenerator/v2/...`), or a database error while generating |
| `404 Not Found`   | Unknown series, or its backing sequence was dropped                          |

> Error messages are only included in the response body when the `local` profile is active.

### Adding a new format

Create a package-private class in `service/processor` that implements `IdFormatProcessor` and annotate it with `@Service("<format-name>")`. `IdGenerationServiceFactory` receives all processors by bean name, so no other change is needed. Add a unit test next to the other processor tests, a case to `IdGenerationIntegrationTest#testFormats`, and the new value to the `IdFormat` enum in `docs/openapi.yaml`.

---

## Tests

| Suite                | Location                     | Command                                         | Needs                        |
| -------------------- | ---------------------------- | ----------------------------------------------- | ---------------------------- |
| Unit tests           | `src/test/java`              | `./gradlew :id-forger-api:test`                 | —                            |
| Integration tests    | `src/integrationTest/java` (tag `integration`) | `./gradlew :id-forger-api:integrationTest` | Docker                       |
| Load test            | `src/integrationTest/java` (tag `load`)        | `./gradlew :id-forger-api:loadTest --rerun` | A running service            |

**Unit tests** cover the controller (MockMvc with the real API versioning strategy, error mapping and log levels), the service and factory, every format processor, `IdGenerator`, the repository's exception translation, and the Luhn generator.

**Integration tests** (`IdGenerationIntegrationTest`) boot the service on a random port against a `postgres:17.5` Testcontainer seeded with `docker/pgsql/init.sql`. They cover every format, sequential IDs, sequence ranges, error codes, API versioning and the health endpoint. Series management is not part of this service, so tests that need a series to be created, reset or deleted do it in the database directly, the way the admin service does, and check that the API sees the change immediately. They also check that the `/series` endpoints are *not* exposed here.

**Load test** (`IdGenerationLoadTest`) sends concurrent requests (100 threads) to `GET /ids/default` on a running instance. It fails if any call fails **or if any ID is returned twice**.

```bash
docker compose -f docker/docker-compose.yml up -d --build
./gradlew :id-forger-api:loadTest --rerun -PloadTest.totalCalls=2000
```

| Property               | Default                  |
| ---------------------- | ------------------------ |
| `loadTest.baseUrl`     | `http://localhost:27110` |
| `loadTest.totalCalls`  | `100000`                 |

`--rerun` is needed because Gradle otherwise skips a task it considers up to date. The results are printed in an `=== Results ===` block.
