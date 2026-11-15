# ID Forger Common

Plain Java library (not runnable) with the code shared by [`id-forger-api`](../id-forger-api/README.md) and [`id-forger-admin`](../id-forger-admin/README.md). Both services add it as an `implementation` dependency and include its package in component scanning:

```java
@SpringBootApplication(scanBasePackages = {
        "org.codecraftlabs.idgenerator.api",
        "org.codecraftlabs.idgenerator.common"
})
```

## Contents

| Package                                         | Class                       | Purpose                                                                                                   |
| ----------------------------------------------- | --------------------------- | --------------------------------------------------------------------------------------------------------- |
| `org.codecraftlabs.idgenerator.common`          | `DatabaseCredentialsCheck`  | Stops the service at startup when `DB_USER`/`DB_PASSWORD` are missing, instead of failing on first query |
| `org.codecraftlabs.idgenerator.common.web`      | `ApiPaths`                  | Base path `/idgenerator/{version}` and supported versions                                                |
|                                                 | `ApiVersioningConfig`       | Resolves the API version from the path segment (`v1`); unsupported versions return `400`                 |
|                                                 | `CorsConfig`                | Registers CORS for `/idgenerator/**` from `idgenerator.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`)    |
|                                                 | `IdResponse`                | `{ "id": "..." }` response body used by both services                                                    |
| `org.codecraftlabs.idgenerator.common.repository` | `DatabaseException`, `SequenceNotFoundException` | Repository-level exceptions                                             |
|                                                 | `SqlStates`                 | PostgreSQL SQLState codes and a helper to match them on a Spring `DataAccessException`                    |
| `org.codecraftlabs.idgenerator.common.service`  | `InvalidSeriesException`, `IdNotGeneratedException` | Service-level exceptions (mapped to `404` / `400` by the controllers)                |

Only add code here when **both** services need it. Code used by a single service belongs in that service's module.

## Test fixtures

The module applies Gradle's `java-test-fixtures` plugin. The services get the fixtures through `testImplementation testFixtures(project(':id-forger-common'))`, which also makes them available to their integration and load tests.

| Class                       | Purpose                                                                                     |
| --------------------------- | ------------------------------------------------------------------------------------------- |
| `ApiVersioningTestSupport`  | Builds the same path-segment API version strategy as `ApiVersioningConfig`, for standalone MockMvc setups |
| `ApiCallResult`             | Result record used by the load tests                                                        |

## Tests

```bash
./gradlew :id-forger-common:test
```

Unit tests cover `DatabaseCredentialsCheck`, `CorsConfig` and `SqlStates`.
