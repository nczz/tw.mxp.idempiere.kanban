# idempiere-kanban — Request Kanban Board for iDempiere

Visual Kanban board for managing iDempiere R_Request records with drag-and-drop, Gantt chart, metrics, notifications, and deep ERP integration.

## Features

**Board**
- Drag-and-drop cards between status columns + within-column reorder
- Swimlanes: group by Project / Sales Rep / Business Partner / Priority
- WIP limits with visual warnings
- Blocked marker + stale indicators (3d/7d)
- FinalClose confirmation
- Org filter (role-based, multi-org support)
- Scope filter: My Cards / My Team / All

**Views**
- Board + Gantt Chart + Metrics (all follow board source + org filter)
- Cycle Time per status + weekly Throughput charts

**Card Management**
- Create with status/org/request type selection
- Edit all fields with 10 searchable ERP dropdowns (BPartner, Product, Order, Invoice, Payment, Project, Campaign, Asset, Activity, User)
- Comments with @mention autocomplete
- File attachments (upload/download/delete)
- Activity Timeline: status moves + comments + field changes (AD_ChangeLog)

**Notifications**
- Watch/Unwatch cards (auto-watch: creator + assignee)
- @mention in comments → auto-watch + notify
- Status change / comment / assignment → AD_Note + HTML Email to watchers
- Per-user language for all notification templates

**Scheduled Reminders (daily)**
- Due tomorrow / Due today → notify watchers
- Overdue (1 day) → notify watchers
- 3 days overdue → escalate to supervisor
- 7 days overdue → auto-block (IsEscalated)
- Start tomorrow → notify assignee
- Toggle: AD_SysConfig `KANBAN_REMINDER_ENABLED`

**Settings**
- Board source (per-user, radio list + rename + create + 🔗 zoom)
- Status management (▲▼ ordering, shared status detection + make independent)
- WIP limits + Priority colors
- All settings zoom-linkable to iDempiere master records

**Enterprise**
- Multi-tenant, multi-org (AD_Role_OrgAccess)
- Per-user preferences (AD_Preference)
- i18n: 120+ AD_Message keys, en_US + zh_TW, extensible via `i18n/*.sql`
- JWT auth independent from REST API
- Real-time push via OSGi EventAdmin + ZK Server Push
- Change Log auto-enabled on R_Request

## Compatibility

| iDempiere | Status |
|-----------|--------|
| v11 – v14 | ✅ |

## Quick Start

### 1. Build

```bash
bash build.sh
```

### 2. Install

**Option A: update-prd.sh (recommended)**

```bash
cd /opt/idempiere
./update-prd.sh file:///path/to/tw.mxp.idempiere.kanban.p2/target/repository tw.mxp.idempiere.kanban
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

On first start, the plugin automatically creates:
- 3 tables, AD_Form + AD_Menu + translations, role access
- Default board (Backlog → To Do → In Progress → Review → Done → Archived)
- 120+ i18n messages with zh_TW translations
- DocumentNo prefix REQ, Change Log on R_Request
- AD_Process + AD_Scheduler for daily reminders
- AD_SysConfig KANBAN_REMINDER_ENABLED=Y

**No manual SQL, no manual configuration.**

### Language Packs

If you enable a new language AFTER installing:

```bash
psql -U adempiere -d idempiere -f i18n/zh_TW.sql
```

## Architecture

```
ZK Desktop → KanbanFormController (@Form)
  → JWT from ZK session (JwtUtil, HS512, KANBAN_TOKEN_SECRET)
  → iframe loads React SPA
    → Board (+ Swimlanes) / Gantt / Metrics views
    → /init, /cards/*, /gantt, /metrics, /lookup, /attachments/*, /config
    → postMessage bridge (zoom, token refresh, server push)

KanbanReminderProcess (AD_Scheduler, daily)
  → scans due/overdue/starting cards
  → AD_Note + HTML Email via NotificationHelper
```

- **Backend**: WAB with 9 servlets + 1 scheduler process (18 Java classes)
- **Frontend**: React 18 + TypeScript + @dnd-kit/core + TanStack Query + Tailwind CSS
- **Auth**: JWT bridging ZK session (independent, no REST API dependency)
- **Data**: Per-user preferences (AD_Preference), org-level config (AD_SysConfig)
- **Notifications**: AD_Note + HTML Email, per-user i18n templates
- **Build**: Maven/Tycho 4.0.8 + Vite 6, unified via `build.sh`

## Documentation

- [Field Mapping](docs/field-mapping.md) — Kanban fields ↔ iDempiere tables

## License

GPL v2 — same as iDempiere.
