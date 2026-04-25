# tw.mxp.idempiere.kanban — Request Kanban Board for iDempiere

Kanban board plugin for managing iDempiere R_Request records with drag-and-drop status transitions.

## Features (Phase 1)

- **Kanban Board** — Drag-and-drop cards between status columns
- **Scope Filtering** — Private / Subordinates / All views
- **Request Type Filtering** — Filter by request type
- **ERP Context** — Cards show Business Partner, Request Type, Priority, Due Date, Sales Rep
- **Move History** — Every status change is logged in RK_Card_Move_Log
- **Real-time Push** — Cross-session updates via OSGi EventAdmin + ZK Server Push
- **Multi-tenant** — Full AD_Client_ID / AD_Org_ID isolation
- **i18n** — Menu and form translations for all system languages

## Compatibility

| iDempiere | Status |
|-----------|--------|
| v11 | ✅ Tested |
| v12 | ✅ Tested |
| v13 | ✅ Tested |
| v14 | ✅ Built against v14 source |

## Architecture

```
ZK Desktop → KanbanFormController (@Form)
  → JWT from ZK session (JwtUtil, HS512)
  → iframe loads React SPA at /kanban/web/index.html
    → SPA calls /kanban/init, /kanban/cards
    → Drag-drop → POST /kanban/cards/{id}/move
    → Real-time refresh via postMessage bridge
```

- **Backend**: WAB (Web Application Bundle) with custom servlets
- **Frontend**: React 18 + TypeScript + @dnd-kit/react + TanStack Query + Tailwind CSS
- **Auth**: JWT bridging ZK session to WAB servlets (no separate login)
- **Build**: Maven/Tycho 4.0.8, produces p2 repository

## Build

### Prerequisites

- Docker (for build and test)
- iDempiere source built (`org.idempiere.p2/target/repository/`)

### Build Plugin (Java)

```bash
docker run --rm \
  -v "$(pwd)":/plugin \
  -v "/path/to/idempiere":/idempiere \
  -v "$HOME/.m2":/root/.m2 \
  -w /plugin \
  maven:3.9-eclipse-temurin-17 \
  mvn verify -Didempiere.repository=file:///idempiere/org.idempiere.p2/target/repository
```

### Build SPA (React)

```bash
docker run --rm \
  -v "$(pwd)":/app \
  -w /app/spa \
  node:20-slim \
  sh -c "npm install && npm run build"
```

SPA output goes to `tw.mxp.idempiere.kanban/web/`.

## Install

### Option 1: Felix Web Console

```bash
curl -u "SuperUser:System" \
  -F "bundlefile=@tw.mxp.idempiere.kanban/target/tw.mxp.idempiere.kanban-14.0.0-SNAPSHOT.jar" \
  -F "action=install" -F "bundlestartlevel=4" \
  http://localhost:8080/osgi/system/console/bundles
```

### Option 2: File System

```bash
cp tw.mxp.idempiere.kanban/target/tw.mxp.idempiere.kanban-*.jar /opt/idempiere/plugins/
echo 'tw.mxp.idempiere.kanban,14.0.0,plugins/tw.mxp.idempiere.kanban.jar,4,false' >> \
  /opt/idempiere/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info
# Restart iDempiere
```

### Run Migration

After first install, execute the migration SQL to create tables and menu:

```bash
psql -U adempiere -d idempiere -f tw.mxp.idempiere.kanban/migration/postgresql/202604260100_RK_CreateTables.sql
```

## Project Structure

```
tw.mxp.idempiere.kanban/
├── tw.mxp.idempiere.kanban.parent/     Maven/Tycho parent pom
├── tw.mxp.idempiere.kanban/            OSGi plugin (WAB)
│   ├── src/tw/mxp/idempiere/kanban/    Java source (10 classes)
│   ├── META-INF/MANIFEST.MF       OSGi + WAB headers
│   ├── WEB-INF/web.xml            Servlet mappings
│   ├── OSGI-INF/                   SCR component XML
│   ├── web/                        SPA build output
│   └── migration/                  SQL (postgresql + oracle)
├── tw.mxp.idempiere.kanban.p2/         p2 repository output
└── spa/                            React source (not deployed)
```

## License

GPL v2 — same as iDempiere.
