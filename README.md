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

## 🏗️ Architecture

The project is a Gradle multi-module build with two independently deployable Spring Boot services and a shared library. Both services use the same PostgreSQL database: a **series** is a PostgreSQL sequence (`idfs_<name>`) plus a prefix, registered in the `series_details` table.

```
                 clients                         operators / internal tools
                    │                                        │
       GET /idgenerator/v1/ids/{series}        /idgenerator/v1/series/**
                    │                                        │
          ┌─────────▼─────────┐                    ┌─────────▼─────────┐
          │   id-forger-api   │                    │  id-forger-admin  │
          │   (port 27110)    │                    │   (port 27111)    │
          └─────────┬─────────┘                    └─────────┬─────────┘
                    │     nextval()           CREATE / DROP / setval()
                    └──────────────┐      ┌──────────────────┘
                                ┌──▼──────▼──┐
                                │ PostgreSQL │
                                └────────────┘
```

| Module                                           | Type                | Responsibility                                                                                          | Port    |
| ------------------------------------------------ | ------------------- | ------------------------------------------------------------------------------------------------------- | ------- |
| [`id-forger-api`](id-forger-api/README.md)       | Spring Boot service | High-volume ID generation hot path: `GET /idgenerator/v1/ids/{seriesName}`                              | `27110` |
| [`id-forger-admin`](id-forger-admin/README.md)   | Spring Boot service | Series management: create, inspect, reset and delete series under `/idgenerator/v1/series`              | `27111` |
| [`id-forger-common`](id-forger-common/README.md) | Java library        | Code shared by both services: API versioning, CORS, database credentials check, common exceptions       | —       |

Why two services:

* **Isolation of the hot path.** Admin traffic can never compete with ID generation for threads or connections, and each service can be scaled on its own.
* **Smaller attack surface.** The public-facing API cannot create, reset or drop sequences; its database code only calls `nextval`. The admin service has no authentication yet, so it can be kept on a private network while only the API is exposed.
* **No coordination needed.** Neither service caches series data. Every request reads the live `series_details` mapping, so a series created, reset or deleted through the admin service takes effect in the API immediately, across any number of instances.

### Repository layout

```
.
├── build.gradle            # conventions shared by all modules
├── settings.gradle
├── id-forger-common/       # shared library (+ test fixtures)
├── id-forger-api/          # ID generation service (Dockerfile, docs/openapi.yaml)
├── id-forger-admin/        # series management service (Dockerfile, docs/openapi.yaml)
├── docker/
│   ├── docker-compose.yml  # PostgreSQL + both services
│   └── pgsql/              # database image and init.sql (schema + seed series)
└── db-changes/             # individual DDL scripts for the shared schema
```

---

## ⚡ Quick Start

Requirements: JDK 21 and Docker.

### 1. Build

```bash
./gradlew clean build
```

This compiles every module, runs all unit tests and produces `id-forger-api/build/libs/id-forger-api-1.2.0.jar` and `id-forger-admin/build/libs/id-forger-admin-1.2.0.jar`.

### 2. Run everything with Docker Compose

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

This builds the images from the jars produced in step 1 and starts:

| Container          | Image                                  | Port    |
| ------------------ | -------------------------------------- | ------- |
| `id-generator-db`  | `codecraftlabs/idgeneratordb:1.0.0`    | `5432`  |
| `id-forger-api`    | `codecraftlabs/id-forger-api:1.2.0`    | `27110` |
| `id-forger-admin`  | `codecraftlabs/id-forger-admin:1.2.0`  | `27111` |

On first startup the database runs `docker/pgsql/init.sql`, which creates the `series_details` table and the `default` and `product` series.

> **Note:** `init.sql` only runs when the data volume is empty. Existing data in the `postgres_data` volume is kept across restarts and upgrades.

### 3. Create a series and generate IDs

```bash
# Create a series (admin service)
curl -X POST http://localhost:27111/idgenerator/v1/series \
  -H "Content-Type: application/json" \
  -d '{ "name": "orders", "prefix": "ORD", "startingValue": 500 }'

# Generate IDs from it (API service)
curl http://localhost:27110/idgenerator/v1/ids/orders
curl "http://localhost:27110/idgenerator/v1/ids/orders?format=prefixed"
```

```json
{ "id": "500" }
{ "id": "ORD000000000000501" }
```

### 4. Stop

```bash
docker compose -f docker/docker-compose.yml down   # keeps the database volume
```

To run a single service outside Docker (e.g. from your IDE or with `bootRun`), see its README.

---

## 📡 API documentation

Each service has its own OpenAPI 3.1 spec:

| Service           | Spec                                                                     | Endpoints                                          |
| ----------------- | ------------------------------------------------------------------------ | -------------------------------------------------- |
| `id-forger-api`   | [`id-forger-api/docs/openapi.yaml`](id-forger-api/docs/openapi.yaml)     | `GET /ids/{seriesName}`, health                    |
| `id-forger-admin` | [`id-forger-admin/docs/openapi.yaml`](id-forger-admin/docs/openapi.yaml) | `POST /series`, `PATCH`/`DELETE /series/{name}`, `GET /series/{name}/currentValue`, `GET /series/{name}/details`, health |

Use them to browse the API in a viewer such as [Swagger Editor](https://editor.swagger.io/), generate client SDKs with [OpenAPI Generator](https://openapi-generator.tech/), or import the endpoints into Postman or Insomnia.

> The specs are maintained by hand. When you add or change an endpoint, update the spec of the module it belongs to.

---

## 🧪 Tests

Every module keeps its tests in its own source sets. The same tasks can be run for all modules from the root, or for a single module with a `:<module>:` prefix.

| Task              | What it runs                                                                                     | Needs                          |
| ----------------- | ------------------------------------------------------------------------------------------------ | ------------------------------ |
| `test`            | Unit tests (`src/test`), part of `build`                                                         | —                              |
| `integrationTest` | Integration tests (`src/integrationTest`, tag `integration`); each service boots against its own PostgreSQL Testcontainer seeded with `docker/pgsql/init.sql` | Docker                         |
| `loadTest`        | Load tests (`src/integrationTest`, tag `load`) against an already running service                | Running service (e.g. compose) |

```bash
./gradlew build integrationTest                       # everything CI runs
./gradlew :id-forger-api:integrationTest              # one module
./gradlew :id-forger-api:loadTest --rerun -PloadTest.totalCalls=2000
./gradlew :id-forger-admin:loadTest --rerun
```

See the module READMEs for what each suite covers.

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

## 📊 Performance

To be added.

---

## 🔌 Integration

Works with any language via HTTP. Client code can be generated from the [ID Forger API spec](id-forger-api/docs/openapi.yaml).

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

### Known limitations

* **The admin service has no authentication yet.** Anyone who can reach `id-forger-admin` can create, delete or reset series. Resetting a sequence to a lower value makes it issue IDs that were already handed out. Until authentication is added, expose only `id-forger-api` publicly, and keep `id-forger-admin` on a private network (gateway, firewall or private subnet).

---

## 🛣️ Roadmap

* [ ] UI for admin/management purposes
* [ ] Authentication for the admin endpoints
* [x] Split ID generation and series management into separate services
* [x] API examples
* [x] OpenAPI specification

---

## 🤝 Contributing

Contributions are welcome. Feel free to open issues or PRs.

---

## ⭐ Support

If this project helps you, consider giving it a star.

It helps others discover it.
