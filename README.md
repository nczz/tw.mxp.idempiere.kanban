# tw.mxp.idempiere.kanban — Request Kanban Board for iDempiere

Kanban board plugin for managing iDempiere R_Request records with drag-and-drop status transitions.

## Features

- **Kanban Board** — Drag-and-drop cards between status columns
- **Swimlanes** — Group cards by Project / Sales Rep / Business Partner / Priority
- **Gantt Chart** — Timeline view with StartDate/EndTime task bars
- **Metrics** — Cycle Time per status + weekly Throughput (filtered by board source)
- **Activity Timeline** — Unified audit log: status moves + comments + field changes (AD_ChangeLog)
- **Card Detail** — View/edit all fields, ERP zoom links, comments, attachments
- **New Request** — Create cards with status/board selection and full field support
- **Search** — Filter by Summary, DocumentNo, Business Partner
- **10 ERP Links** — Searchable dropdowns: BPartner, Product, Order, Invoice, Payment, Project, Campaign, Asset, Activity, User
- **Comments** — R_RequestUpdate discussion thread per card
- **Attachments** — Upload/download/delete files (AD_Attachment)
- **WIP Limits** — Per-column card limits with visual warnings
- **Blocked Marker** — Flag cards as blocked with visual indicator
- **Stale Indicators** — Cards not moved for 3+ days show warning
- **Priority Colors** — Configurable via settings dialog
- **Settings** — Board source (per-user), status management with ordering, WIP limits, priority colors
- **Shared Status Warning** — Detects shared status categories, option to make independent
- **Default Board** — Auto-creates: Backlog → To Do → In Progress → Review → Done → Archived
- **Scope Filtering** — Private / Subordinates / All views
- **Open/Closed Toggle** — Switch between active and archived cards
- **Real-time Push** — Cross-session updates via OSGi EventAdmin + ZK Server Push
- **Multi-tenant** — Full AD_Client_ID / AD_Org_ID isolation
- **i18n** — 100+ AD_Message with en_US + zh_TW translations, priority labels from AD_Ref_List_Trl

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

### 2. Install

**Option A: update-prd.sh (recommended)**

Copy the p2 repository to the server, then:

```bash
cd /opt/idempiere
./update-prd.sh file:///path/to/tw.mxp.idempiere.kanban.p2/target/repository tw.mxp.idempiere.kanban
```

Or from a remote URL:

```bash
./update-prd.sh https://your-server/kanban-p2-repo tw.mxp.idempiere.kanban
```

**Option B: Manual copy**

```bash
JAR=tw.mxp.idempiere.kanban/target/tw.mxp.idempiere.kanban-14.0.0-SNAPSHOT.jar

docker cp $JAR <container>:/opt/idempiere/plugins/
docker exec <container> bash -c "
  echo 'tw.mxp.idempiere.kanban,14.0.0,plugins/$(basename $JAR),4,false' >> \
  /opt/idempiere/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info
"
docker restart <container>
```

### 3. Done

On first start, the plugin automatically:
- Creates 3 tables (RK_Card_Move_Log, RK_Card_Member, RK_Request_Type_Config)
- Creates AD_Form + AD_Menu + translations
- Grants access to all roles
- Creates default board (Backlog → To Do → In Progress → Review → Done → Archived)
- Creates 100+ i18n messages with zh_TW translations
- Sets DocumentNo prefix REQ for request numbering
- Enables Change Log on R_Request for audit trail

**No manual SQL, no manual configuration.**

### Language Packs (Optional)

If you enable a new language AFTER installing the plugin:

```bash
psql -U adempiere -d idempiere -f i18n/zh_TW.sql
```

Languages enabled BEFORE install are translated automatically.

## Architecture

```
ZK Desktop → KanbanFormController (@Form)
  → JWT from ZK session (JwtUtil, HS512, KANBAN_TOKEN_SECRET)
  → iframe loads React SPA
    → Board (+ Swimlanes) / Gantt / Metrics views
    → /init, /cards/*, /gantt, /metrics, /lookup, /attachments/*, /config
    → postMessage bridge (zoom, token refresh, server push)
```

- **Backend**: WAB with 8 servlets (16 Java classes)
- **Frontend**: React 18 + TypeScript + @dnd-kit/core + TanStack Query + Tailwind CSS
- **Auth**: JWT bridging ZK session to WAB servlets (independent, no REST API dependency)
- **Data**: Per-user preferences (AD_Preference), org-level config (AD_SysConfig)
- **Build**: Maven/Tycho 4.0.8 + Vite 6, unified via `build.sh`

## License

GPL v2 — same as iDempiere.
