# ID Forger Service

A lightweight REST microservice for generating unique, concurrent-safe IDs backed by PostgreSQL sequences. Each *series* maps to a dedicated database sequence, so different domains (e.g. orders, products, users) produce non-overlapping IDs without any coordination overhead.

## Features

- **Multiple output formats** — plain integer, Base64, SHA-256, Luhn-valid, timestamped, or series-prefixed
- **Named series** — isolate ID spaces per domain with independent sequences
- **Concurrent-safe** — leverages PostgreSQL sequence atomicity; no application-level locking required
- **Sequence management** — inspect metadata, read the current value, or reset a sequence via REST
- **Production-ready** — HikariCP connection pool (20 connections), graceful shutdown, Spring Boot Actuator

## Tech Stack

- Java 21 / Spring Boot 3.5
- PostgreSQL (sequences via MyBatis)
- Docker / Docker Compose
- Gradle

## Quick Start

The fastest way to run the service is with Docker Compose, which starts both the database and the application.

**Prerequisites:** Docker Desktop

```bash
# 1. Build the application image
docker image build -t codecraftlabs/idgeneratorapp:1.1.1 -f ./docker/app/Dockerfile .

# 2. Start the stack
cd docker && docker-compose up -d
```

The service is available at `http://localhost:27110`.

```bash
# Generate your first ID
curl -s http://localhost:27110/idgenerator/v1/ids/default
```

To stop and remove all containers and volumes:

```bash
docker-compose down -v
```

## Running Locally

**Prerequisites:** Java 21, Docker Desktop (for the database)

### 1. Start the database

```bash
# Build the database image
docker image build -t codecraftlabs/idgenerator:1.0.0 ./docker/pgsql

# Start the container
docker container run --detach --name idgenerator --publish 5432:5432 codecraftlabs/idgenerator:1.0.0
```

### 2. Build and run the application

```bash
# Build
./gradlew clean build

# Run
java -jar ./build/libs/id-forger-service-1.2.0.jar
```

The service listens on port **27110** by default.

### Remote debugging

```bash
java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n \
     -jar ./build/libs/id-forger-service-1.2.0.jar
```

## Configuration

All settings can be overridden via environment variables.

| Environment Variable | Default     | Description              |
|----------------------|-------------|--------------------------|
| `DB_HOST`            | `localhost` | PostgreSQL host          |
| `DB_PORT`            | `5432`      | PostgreSQL port          |
| `DB_NAME`            | `idgenerator` | Database name          |
| `DB_USER`            | `idgen`     | Database user            |
| `DB_PASSWORD`        | `idgen`     | Database password        |

## ID Formats

Pass the `format` query parameter to control how the raw sequence value is returned.

| Format        | Description                                              |
|---------------|----------------------------------------------------------|
| `plain`       | Raw integer (default when `format` is omitted)           |
| `base64`      | Base64-encoded string of the integer                     |
| `sha256`      | SHA-256 hash of the integer                              |
| `luhn`        | Luhn-valid number derived from the sequence value        |
| `timestamped` | Integer concatenated with the current time in milliseconds |
| `prefixed`    | Integer prefixed with the series name                    |

## API Reference

All endpoints are served under `/idgenerator/v1` and return `application/json`.

Examples below assume the service is running on `localhost:27110`. Replace `default` with any configured series name (e.g. `product`).

---

### Generate the next ID

`GET /idgenerator/v1/ids/{seriesName}`

Returns the next unique ID for the series and advances the sequence.

```bash
# Plain (default)
curl -s http://localhost:27110/idgenerator/v1/ids/default

# Base64-encoded
curl -s "http://localhost:27110/idgenerator/v1/ids/default?format=base64"

# Prefixed with the series name
curl -s "http://localhost:27110/idgenerator/v1/ids/product?format=prefixed"

# Luhn-valid number
curl -s "http://localhost:27110/idgenerator/v1/ids/default?format=luhn"

# SHA-256 hash
curl -s "http://localhost:27110/idgenerator/v1/ids/default?format=sha256"

# Timestamped
curl -s "http://localhost:27110/idgenerator/v1/ids/default?format=timestamped"
```

---

### Get the current value (non-advancing)

`GET /idgenerator/v1/ids/{seriesName}/currentValue`

Returns the most recently generated ID without advancing the sequence.

```bash
curl -s http://localhost:27110/idgenerator/v1/ids/default/currentValue
```

---

### Get sequence details

`GET /idgenerator/v1/ids/{seriesName}/details`

Returns metadata about the underlying database sequence (current value, increment, min/max bounds).

```bash
curl -s http://localhost:27110/idgenerator/v1/ids/default/details
```

---

### Reset a sequence

`PUT /idgenerator/v1/ids/{seriesName}`

Sets the sequence's last value so the next generated ID starts after the provided number. Useful for migrations or manual corrections.

```bash
curl -s -X PUT http://localhost:27110/idgenerator/v1/ids/default \
  -H "Content-Type: application/json" \
  -d '{"newLastValue": 1000}'
```

## Adding a New Series

A series is backed by a named PostgreSQL sequence. To add one, create and run a migration script:

```sql
-- example: orders series, starting from 5000000
CREATE SEQUENCE orders_sequence
  START WITH 5000000
  MINVALUE 5000000
  MAXVALUE 5999999
  INCREMENT BY 1;
```

See `db-changes/` for existing examples.

## Testing

```bash
# Unit tests
./gradlew test

# Integration tests (requires a running database)
./gradlew integrationTest
```

## License

This project is licensed under the terms in the [LICENSE](LICENSE) file.
