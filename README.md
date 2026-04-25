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

### 1. Run with Docker

```bash
docker run -p 8080:8080 id-forger-service
```

### 2. Generate an ID

```bash
curl http://localhost:8080/id
```

Response:

```json
{
  "id": "ORD-20260425-000123"
}
```

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
