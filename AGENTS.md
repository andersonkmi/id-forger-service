# AGENTS.md

Gradle multi-module project (JDK 21, Spring Boot 4, PostgreSQL, Docker):

- `id-forger-common`: shared java-library (+ `testFixtures`)
- `id-forger-api`: ID generation service, port 27110
- `id-forger-admin`: series management service, port 27111

Shared build conventions live in the root `build.gradle` (`subprojects {}` and the `configure([...])` block for the two services).

## Commands

```bash
./gradlew clean build                 # compile + unit tests, all modules
./gradlew integrationTest             # Testcontainers integration tests (needs Docker)
./gradlew :id-forger-api:test         # single module
docker compose -f docker/docker-compose.yml up -d --build   # db + both services (after build)
./gradlew :id-forger-api:loadTest --rerun -PloadTest.totalCalls=2000
./gradlew :id-forger-admin:loadTest --rerun
docker compose -f docker/docker-compose.yml down            # never add -v: it deletes series data
```

## Conventions

- Code used by only one service goes in that service's module; `id-forger-common` is only for code both need.
- Integration tests read the shared schema from `docker/pgsql/init.sql` (passed in as the `integrationTest.initSql` system property).
- Each service has its own hand-maintained OpenAPI spec in `<module>/docs/openapi.yaml`; update it when endpoints change.
- Version `1.2.0` appears in the root `build.gradle`, both Dockerfiles, `docker/docker-compose.yml`, the specs and the READMEs. Keep them in sync.
