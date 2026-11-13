# ID Forger Service

> A centralized, flexible ID generation service for distributed systems.

Stop relying on random UUIDs when your system needs **structure, ordering, and control**.

ID Forger Service gives you **predictable, customizable, and scalable IDs** via a simple API.

---

## 🚀 Why this exists

Most systems default to UUIDs. They work—but they come with tradeoffs:

* ❌ No ordering (bad for databases & indexing)
* ❌ No meaning (hard to debug)
* ❌ No control over format
* ❌ Hard to coordinate across services

Other solutions (like Snowflake or ULID) improve some aspects—but still lack flexibility.

**ID Forger Service solves this by acting as a dedicated ID engine for your system.**

---

## ✨ What makes it different

* 🔧 **Custom ID formats** — define your own structure
* 🌍 **Centralized generation** — consistent across all services
* ⚡ **High performance** — built for distributed environments
* 🧩 **Pluggable strategies** — adapt to your use case
* 📡 **API-first** — works with any language

---

## 🧠 When should you use it?

Use this if you need:

* Ordered IDs for databases
* Human-readable identifiers
* Multi-tenant ID patterns
* Consistent ID generation across microservices
* More control than UUID/ULID provides

---

## ⚖️ Comparison

Different tools solve different problems. Here’s an honest comparison:

| Feature              | UUID  | ULID  | Snowflake | ID Forger |
| -------------------- | ----- | ----- | --------- | --------- |
| Requires service     | ❌     | ❌     | ❌         | ✅         |
| Sortable             | ❌     | ✅     | ✅         | ✅*        |
| Human-readable       | ❌     | ⚠️    | ❌         | ✅         |
| Custom format        | ❌     | ❌     | ❌         | ✅         |
| Operational overhead | ✅ low | ✅ low | ⚠️ medium | ❌ higher  |

* depends on configuration
---

## ⚡ Quick Start

### 1. Start PostgreSQL

The service requires a PostgreSQL database. A ready-to-use image is defined in `docker/pgsql/Dockerfile` (based on `postgres:17.5`). On first startup it runs `docker/pgsql/init.sql`, which creates the `default_sequence` and `product_sequence` sequences.

Build the image:

```bash
docker image build -t codecraftlabs/id-forger-service-dev:1.0.0 docker/pgsql
```

Create and start the container. The image has no built-in password, so `POSTGRES_PASSWORD` must be set when it starts:

```bash
docker run -d --name id-forger-dev-db -p 5432:5432 -e POSTGRES_PASSWORD=idgen -v id-forger-dev-db-pgdata:/var/lib/postgresql/data codecraftlabs/id-forger-service-dev:1.0.0
```

Default connection settings for the `local` profile:

| Setting  | Value         |
| -------- | ------------- |
| Host     | `localhost`   |
| Port     | `5432`        |
| Database | `idgenerator` |
| User     | `idgen`       |
| Password | `idgen`       |

These can be overridden for the application via the `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` and `DB_PASSWORD` environment variables.

The default user and password only exist in the `local` profile (`application-local.properties`). Outside of it, `DB_USER` and `DB_PASSWORD` are required and the application refuses to start without them.

Verify the database:

```bash
docker exec -it id-forger-dev-db psql -U idgen -d idgenerator -c "SELECT sequencename FROM pg_sequences;"
```

### 2. Run the application

Locally with Gradle (`bootRun` activates the `local` profile):

```bash
./gradlew bootRun
```

Or running the Java command:

```
export SPRING_PROFILES_ACTIVE=local
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=\*:5005 -jar ./build/libs/id-forger-service-1.2.0.jar --spring.profiles.active=local
```

Or with Docker:

- Build the application:

```bash
gradle clean build
```

- Create the application image using the following command:

```bash
docker build -t codecraftlabs/id-forger-service:1.2.0 docker/app
```

- Start the container using the following command:


```bash
docker run -d --name id-generator -p 27110:27110 codecraftlabs/id-forger-service:1.2.0
```

### 3. Generate an ID

```bash
curl http://localhost:27110/idgenerator/v1/ids/default
```

Response:

```json
{
  "id": "1"
}
```

See [API Examples](#-api-examples) for every available endpoint.

### Managing the database container

```bash
docker stop id-forger-dev-db     # stop
docker start id-forger-dev-db    # start again
docker rm -f id-forger-dev-db    # remove the container
docker volume rm id-forger-dev-db-pgdata  # remove data (init.sql runs again on next creation)
```

> **Note:** `init.sql` only runs when the data volume is empty. To pick up changes to it, remove the volume and recreate the container.

---

## 📡 API Examples

All endpoints live under `/idgenerator/v1` and the service listens on port `27110` by default. The version is part of the path; an unsupported version (e.g. `/idgenerator/v2/...`) returns `400 Bad Request`.

The examples below use the `default` and `product` series that `init.sql` creates.

### Generate the next ID

```bash
curl http://localhost:27110/idgenerator/v1/ids/default
```

```json
{ "id": "42" }
```

#### Output formats

Use the optional `format` query parameter to change how the ID is rendered. When omitted or blank, `plain` is used.

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

An unknown series returns `404 Not Found`; an unknown format returns `400 Bad Request`.

### Create a series

Only `name` and `prefix` are required. Numeric options that are left out fall back to the database defaults (start/min `1`, increment `1`, max `9223372036854775807`).

```bash
curl -X POST http://localhost:27110/idgenerator/v1/series \
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

Creating a series that already exists also returns `400 Bad Request`.

### Get the current value of a series

Returns the last generated value without advancing the sequence.

```bash
curl http://localhost:27110/idgenerator/v1/series/default/currentValue
```

```json
{ "id": "42" }
```

### Get series details

```bash
curl http://localhost:27110/idgenerator/v1/series/product/details
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
curl -X PATCH http://localhost:27110/idgenerator/v1/series/product \
  -H "Content-Type: application/json" \
  -d '{ "newLastValue": 1451500000 }'
```

```json
{ "id": "1451500000" }
```

### Delete a series

Removes the series together with its backing database sequence. The endpoint requires the `Content-Type: application/json` header (other content types return `415 Unsupported Media Type`).

```bash
curl -X DELETE http://localhost:27110/idgenerator/v1/series/orders \
  -H "Content-Type: application/json"
```

Response (`200 OK`):

```
Done
```

### Health check

```bash
curl http://localhost:27110/actuator/health
```

```json
{ "status": "UP" }
```

### Error responses

| Status                        | When                                                                 |
| ----------------------------- | -------------------------------------------------------------------- |
| `400 Bad Request`             | Invalid format, validation errors, out-of-range values, unsupported API version |
| `404 Not Found`               | Unknown series                                                       |
| `415 Unsupported Media Type`  | Missing or non-JSON `Content-Type` on POST/PATCH/DELETE              |
| `503 Service Unavailable`     | Admin (`/series`) endpoints saturated; retry after the `Retry-After` hint |

> Error messages are only included in the response body when the `local` profile is active.

---

## 🧩 Example Use Cases

### E-commerce orders

```
ORD-20260425-000123
```

### Multi-tenant systems

```
TENANT1-USER-000045
```

### Logs & tracing

```
TRACE-9F82K3
```

---

## 🏗️ Architecture Overview

* Stateless API layer
* Pluggable ID strategies
* Scalable horizontally

---

## 📊 Performance

To be added.

---

## 🔌 Integration

Works with any language via HTTP.

Planned:

* Go SDK
* Java SDK
* CLI tool

---

## ❗ Tradeoffs

Be aware of the tradeoffs when choosing this approach:

* Requires running and maintaining a service
* Adds network latency compared to local ID generation
* Potential single point of failure (can be mitigated with replication)

If you want zero infrastructure, UUID/ULID may be a better fit.

---

## 🛣️ Roadmap

* [ ] UI for admin/management purposes
* [x] API examples

---

## 🤝 Contributing

Contributions are welcome. Feel free to open issues or PRs.

---

## ⭐ Support

If this project helps you, consider giving it a star.

It helps others discover it.
