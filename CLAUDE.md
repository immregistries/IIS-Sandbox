# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

IIS Sandbox is a multi-tenant Immunization Information System built on HAPI FHIR JPA Server. It provides a FHIR R4/R5
API, HL7v2 messaging (VXU/QBP), vaccine forecasting, patient matching (MDM), SMART Health Links, and a JSP-based web UI.
Packaged as a WAR deployed on Tomcat.

## Build & Run

**Prerequisites:** JDK 17, Maven 3.8.3+, `jq` (for dependency script)

```bash
# Install external dependencies (required before first build)
mkdir ../temp-dependencies
./dependencies.sh build ../temp-dependencies

# Build all modules
mvn clean install

# Run locally with embedded H2 databases
cp example.env .env  # then configure
set -a; source .env; set +a
java -jar iis-web/target/iis.war

# Docker (requires PostgreSQL + MySQL)
mvn clean install && docker-compose up
```

The app is available at `http://localhost:8080/iis/home` (UI) and `http://localhost:8080/iis/fhir/metadata` (FHIR API).

**Run tests (skipped by default in `prod` profile):**

```bash
mvn test -P'!prod'                          # unit tests
mvn verify -P'!prod'                        # integration tests (failsafe)
mvn test -pl iis-web -P'!prod'              # tests in a single module
mvn test -pl iis-logic -P'!prod' -Dtest=ClassName  # single test class
```

Tests are skipped by default because the `prod` Maven profile (active by default) sets `maven.test.skip=true`.

## Module Architecture

13 Maven modules with this dependency flow (bottom-up):

```
iis-constants          → shared enums/constants
iis-core-model         → JPA entities, DB config (dual-datasource: PostgreSQL for FHIR, MySQL for IIS)
iis-core-commons       → security utils, tenant management, JWT, subscriptions
iis-fhir-annotations   → Spring conditional annotations for FHIR version switching (R4/R5)
iis-mapping            → bidirectional mapping between IIS domain models and FHIR resources
iis-logic-immds        → vaccine forecast/recommendation logic (CDS connections)
iis-logic              → core business logic (VXU processing, tenant comparison, FHIR diff/patch)
iis-fhir-interceptors  → HAPI interceptors (identifier resolution, group authority, logging)
iis-fhir-bulk-export   → FHIR Bulk Data export ($export)
iis-fhir-immds         → FHIR endpoints for immunization recommendations
iis-fhir-server        → HAPI FHIR JPA server config, MDM, partitioning
iis-smart-health-link-card → SMART Health Link card/manifest generation
iis-web                → Spring Boot entry point, controllers, security (WAR packaging)
```

## iis-web Module Details

Entry point: `org.immregistries.iis.kernal.Application` — Spring Boot app extending `SpringBootServletInitializer`.

Package `org.immregistries.iis.kernal` (note: "kernal" is an intentional legacy spelling).

### Controller layers

- `controllers/servlet/` — JSP-backed UI controllers (Home, Patient, Vaccination, Message, etc.)
- `controllers/rest/` — REST API controllers, all extend `BaseTenantTiedRest`
- `controllers/rest/shlink/` — SMART Health Link REST endpoints
- `controllers/servlet/legacy/` — deprecated feature controllers (Covid, FITS, VCI)
- `controllers/filters/` — `TenantUrlFilter` extracts tenant name from URL path

### Key classes

- `HapiFhirServerRegistrationConfig` — registers the HAPI FHIR `RestfulServer` servlet at `/fhir/*`
- `ServerSecurityConfig` — Spring Security filter chain (form login, OAuth2/GitHub, JWT SMART auth)
- `FilterRegistrationConfig` / `TenantUrlFilter` — multi-tenant URL routing

### Multi-tenancy

Tenant name is extracted from the URL path (e.g., `/tenant/{name}/...`) via `TenantUrlFilter` and stored as a request
attribute (`IisRequestAttribute.TENANT_NAME_URL`). HAPI FHIR partitioning is enabled with
`request_tenant_partitioning_mode: true`.

## Key Configuration

- `iis-web/src/main/resources/application.yaml` — main Spring/HAPI config
- `example.env` — environment variable template (copy to `.env`)
- Spring profiles: `h2` (in-memory dev), `prod` (PostgreSQL + MySQL)
- FHIR version controlled by `FHIR_VERSION` env var (default R4), with conditional beans (`OnR4Condition`,
  `OnR5Condition`)
- MDM (Master Data Management) rules: `iis-web/src/main/resources/mdm-rules.json`

## External Dependencies

The project depends on forks/specific commits of libraries not on Maven Central, managed via `dependencies.json` and
`dependencies.sh`. Key ones:

- **Modded HAPI FHIR** (`hapi-fhir-Subscription-custom`) — custom fork, parent POM version `8.0.0-IIS`
- **vaccination_deduplication** — NIST vaccine dedup
- **Lonestar-Forecaster** / **ForecastConnector** — vaccine forecasting
- **MQE-validator** — HL7 message validation
- **mismo-match** — patient matching
- **v2tofhir** — HL7v2 to FHIR conversion

## CI

GitHub Actions (`maven.yml`): builds with `mvn -B package` on JDK 17 for all branches and PRs to master.