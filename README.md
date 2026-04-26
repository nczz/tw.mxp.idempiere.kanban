# tw.mxp.idempiere.kanban — Request Kanban Board for iDempiere

Kanban board plugin for managing iDempiere R_Request records with drag-and-drop status transitions.

## Features

- **Kanban Board** — Drag-and-drop cards between status columns
- **Gantt Chart** — Timeline view with StartDate/EndTime task bars
- **Metrics** — Cycle Time per status + weekly Throughput charts
- **Card Detail** — View/edit all fields, ERP zoom links, comments, attachments, move history
- **New Request** — Create cards directly from the board with full field support
- **Search** — Filter by Summary, DocumentNo, Business Partner
- **9 ERP Links** — Searchable: BPartner, Product, Order, Invoice, Payment, Project, Campaign, Asset, Activity
- **Comments** — R_RequestUpdate discussion thread per card
- **Attachments** — Upload/download/delete files (AD_Attachment)
- **WIP Limits** — Per-column card limits with visual warnings
- **Blocked Marker** — Flag cards as blocked (待決) with visual indicator
- **Stale Indicators** — Cards not moved for 3+ days show warning
- **Priority Colors** — Configurable via settings dialog
- **Settings** — Board source, status management, WIP limits, priority colors
- **Default Board** — Auto-creates: Backlog → To Do → In Progress → Review → Done → Archived
- **Scope Filtering** — Private / Subordinates / All views
- **Open/Closed Toggle** — Switch between active and archived cards
- **Real-time Push** — Cross-session updates via OSGi EventAdmin + ZK Server Push
- **Multi-tenant** — Full AD_Client_ID / AD_Org_ID isolation
- **i18n** — 70+ AD_Message with en_US + zh_TW translations

## Compatibility

| iDempiere | Status |
|-----------|--------|
| v11 | ✅ Tested |
| v12 | ✅ Tested |
| v13 | ✅ Tested |
| v14 | ✅ Built against v14 source |

## Quick Start (Zero Manual Steps)

### 1. Build

```bash
bash build.sh
```

Or manually:
```bash
# SPA
docker run --rm -v "$(pwd)":/app -w /app/spa node:20-slim sh -c "npm install && npm run build"
# Plugin
docker run --rm -v "$(pwd)":/plugin -v "/path/to/idempiere":/idempiere -v "$HOME/.m2":/root/.m2 -w /plugin \
  maven:3.9-eclipse-temurin-17 mvn verify -Didempiere.repository=file:///idempiere/org.idempiere.p2/target/repository
```

### 2. Install

Copy JAR + add to bundles.info + restart:

```bash
JAR=tw.mxp.idempiere.kanban/target/tw.mxp.idempiere.kanban-14.0.0-SNAPSHOT.jar

# Copy to plugins
docker cp $JAR <container>:/opt/idempiere/plugins/

# Add to bundles.info
docker exec <container> bash -c "
  echo 'tw.mxp.idempiere.kanban,14.0.0,plugins/$(basename $JAR),4,false' >> \
  /opt/idempiere/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info
"

# Restart
docker restart <container>
```

### 3. Done

On first start, the plugin automatically:
- Creates 3 tables (RK_Card_Move_Log, RK_Card_Member, RK_Request_Type_Config)
- Creates AD_Form + AD_Menu + translations (en: Request Kanban / zh_TW: 需求看板)
- Grants access to all roles
- Creates default board (Backlog → To Do → In Progress → Review → Done → Archived)
- Creates 70+ i18n messages with zh_TW translations
- Sets DocumentNo prefix REQ for request numbering

**No manual SQL, no manual configuration.** Open iDempiere → find "Request Kanban" in menu → start using.

## Architecture

```
ZK Desktop → KanbanFormController (@Form)
  → JWT from ZK session (JwtUtil, HS512, KANBAN_TOKEN_SECRET)
  → iframe loads React SPA
    → Board / Gantt / Metrics views
    → /init, /cards/*, /gantt, /metrics, /lookup, /attachments/*, /config
    → postMessage bridge (zoom, token refresh, server push)
```

- **Backend**: WAB with 8 servlets (16 Java classes)
- **Frontend**: React 18 + TypeScript + @dnd-kit/core + TanStack Query + Tailwind CSS
- **Auth**: JWT bridging ZK session to WAB servlets (independent, no REST API dependency)
- **Build**: Maven/Tycho 4.0.8 + Vite 6, unified via `build.sh`

## License

GPL v2 — same as iDempiere.
