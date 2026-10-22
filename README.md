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
docker build -t codecraftlabs/idgeneratordb:1.0.0 docker/pgsql
```

Create and start the container:

```bash
docker run -d --name id-generator-db -p 5432:5432 -v id-generator-pgdata:/var/lib/postgresql/data codecraftlabs/idgeneratordb:1.0.0
```

Default connection settings (matching `application.properties`):

| Setting  | Value         |
| -------- | ------------- |
| Host     | `localhost`   |
| Port     | `5432`        |
| Database | `idgenerator` |
| User     | `idgen`       |
| Password | `idgen`       |

These can be overridden for the application via the `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` and `DB_PASSWORD` environment variables.

Verify the database:

```bash
docker exec -it id-generator-db psql -U idgen -d idgenerator -c "SELECT sequencename FROM pg_sequences;"
```

### 2. Run the application

Locally with Gradle:

```bash
./gradlew bootRun
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
docker run -d --name id-generator -p 8080:8080 codecraftlabs/idgenerator:1.2.0
```

### 3. Generate an ID

```bash
curl http://localhost:8080/id
```

Response:

```json
{
  "id": "ORD-20260425-000123"
}
```

### Managing the database container

```bash
docker stop id-generator-db     # stop
docker start id-generator-db    # start again
docker rm -f id-generator-db    # remove the container
docker volume rm id-generator-pgdata  # remove data (init.sql runs again on next creation)
```

> **Note:** `init.sql` only runs when the data volume is empty. To pick up changes to it, remove the volume and recreate the container.

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
* [ ] API examples

---

## 🤝 Contributing

Contributions are welcome. Feel free to open issues or PRs.

---

## ⭐ Support

If this project helps you, consider giving it a star.

It helps others discover it.
