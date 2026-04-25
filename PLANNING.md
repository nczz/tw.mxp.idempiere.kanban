# tw.mxp.idempiere.kanban — Phase 1 Planning

> Request Kanban Board for iDempiere
> Architecture: SPA (React) + Custom API + iDempiere AD Tables
> 基於 tw.idempiere.requestkanbanform（ZK MVVM）和 idempiere-appointment（SPA+API）的經驗重新設計

---

## 1. 設計目標

### 保留的好設計（來自 requestkanbanform）

| 設計 | 原因 |
|------|------|
| R_Request 作為核心資料 | 不重新發明工單系統，保留所有 core 功能（email、escalation） |
| R_Status 作為看板欄位 | 狀態定義在 AD 層，管理員可自訂 |
| 四級 Scope 過濾（Private/Subordinates/Team/All） | 遞迴 CTE 查詢部屬，完整的權限模型 |
| 跨 session 即時推送（OSGi EventAdmin + Server Push） | 生產驗證過的機制 |
| AD_Message i18n | iDempiere 標準做法 |
| StatusConfig from AD_SysConfig | 管理員可設定，不需改 code |
| 批次載入 avatar（避免 N+1） | 效能關鍵 |
| KanbanRowModel 不可變顯示模型 | 資料載入與顯示分離 |
| EndTime 保護（IModelFactory） | 繞過 core 的 RequestEventHandler 清除 EndTime |

### 改進的設計（來自 appointment 的 SPA+API 架構）

| 問題 | 現有做法 | 新做法 |
|------|---------|--------|
| 卡片成員 | 借用 r_requestupdates | 新建 RK_Card_Member 表 |
| RequestType→Role 對應 | Description 塞 JSON | 新建 RK_Request_Type_Config 表 |
| 移動歷程 | 無 | 新建 RK_Card_Move_Log 表 |
| 看板設定 | 硬編碼 | Phase 2：新建 RK_Board / RK_Board_Column 表 |
| 卡片縮圖 | 掃描所有附件 | Phase 2：R_Request 擴充欄位（X_Thumbnail_AD_Image_ID） |
| 前端框架 | ZK MVVM（1399 行 ViewModel） | React SPA（元件化、專業拖放庫） |
| API 層 | ViewModel 直接操作 PO | 自訂 Servlet API（單一事務、server-side 驗證） |
| 甘特圖 | Server-side HTML table | 前端甘特圖庫（frappe-gantt 或類似） |
| 即時更新 | OSGi EventAdmin + ZK Server Push | 保留 EventAdmin，SPA 透過 postMessage 接收 |

---

## 2. 架構

```
┌─────────────────────────────────────────────────────────┐
│ iDempiere ZK Desktop                                     │
│  ┌─────────────────────────────────────────────────────┐ │
│  │ KanbanFormController (@Form)                         │ │
│  │  ├─ JwtUtil.generate() → JWT from ZK session        │ │
│  │  ├─ Creates Iframe → /kanban/web/index.html#token=  │ │
│  │  ├─ postMessage bridge (zoom, refresh-token)        │ │
│  │  └─ OSGi EventAdmin subscriber → postMessage push   │ │
│  └─────────────────────────────────────────────────────┘ │
│                          │ iframe                        │
│  ┌───────────────────────▼─────────────────────────────┐ │
│  │ React SPA                                            │ │
│  │  ├─ Kanban View (dnd-kit drag-drop)                 │ │
│  │  ├─ List View (sortable table)          [Phase 2]   │ │
│  │  ├─ Gantt View (frappe-gantt)           [Phase 2]   │ │
│  │  └─ All API calls → /kanban/{endpoint}              │ │
│  └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│ WAB Servlets (/kanban context)                           │
│  ├─ AuthFilter (JWT validation)                         │
│  ├─ NoCacheFilter (/web/*)                              │
│  ├─ InitServlet     GET  /init                          │
│  ├─ CardsServlet    GET  /cards                         │
│  │                  POST /cards/{id}/move                │
│  │                  GET  /cards/{id}         [Phase 2]  │
│  │                  PUT  /cards/{id}         [Phase 2]  │
│  │                  POST /cards              [Phase 2]  │
│  │                  */members                [Phase 2]  │
│  ├─ GanttServlet    GET  /gantt              [Phase 2]  │
│  └─ AvatarServlet   GET  /avatar/{uid}       [Phase 2]  │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│ Database                                                 │
│  ├─ R_Request (core, 不改)                              │
│  ├─ R_Status (core, 不改)                               │
│  ├─ RK_Card_Member (新)                                 │
│  ├─ RK_Card_Move_Log (新)                               │
│  └─ RK_Request_Type_Config (新)                         │
└─────────────────────────────────────────────────────────┘
```

---

## 3. 新建資料表設計

### RK_Card_Member — 卡片成員

```sql
CREATE TABLE RK_Card_Member (
    RK_Card_Member_ID  SERIAL PRIMARY KEY,
    RK_Card_Member_UU  VARCHAR(36) DEFAULT generate_uuid(),
    AD_Client_ID       INTEGER NOT NULL,
    AD_Org_ID          INTEGER NOT NULL DEFAULT 0,
    IsActive           CHAR(1) NOT NULL DEFAULT 'Y',
    Created            TIMESTAMP NOT NULL DEFAULT NOW(),
    CreatedBy          INTEGER NOT NULL,
    Updated            TIMESTAMP NOT NULL DEFAULT NOW(),
    UpdatedBy          INTEGER NOT NULL,
    R_Request_ID       INTEGER NOT NULL REFERENCES R_Request(R_Request_ID),
    AD_User_ID         INTEGER NOT NULL REFERENCES AD_User(AD_User_ID),
    MemberRole         VARCHAR(20) DEFAULT 'Observer'  -- Observer / Assignee / Reviewer
);
CREATE UNIQUE INDEX RK_Card_Member_UQ ON RK_Card_Member(R_Request_ID, AD_User_ID) WHERE IsActive='Y';
```

取代借用 r_requestupdates。語意清晰，有 MemberRole 欄位。

### RK_Card_Move_Log — 移動歷程

```sql
CREATE TABLE RK_Card_Move_Log (
    RK_Card_Move_Log_ID  SERIAL PRIMARY KEY,
    RK_Card_Move_Log_UU  VARCHAR(36) DEFAULT generate_uuid(),
    AD_Client_ID         INTEGER NOT NULL,
    AD_Org_ID            INTEGER NOT NULL DEFAULT 0,
    IsActive             CHAR(1) NOT NULL DEFAULT 'Y',
    Created              TIMESTAMP NOT NULL DEFAULT NOW(),
    CreatedBy            INTEGER NOT NULL,
    Updated              TIMESTAMP NOT NULL DEFAULT NOW(),
    UpdatedBy            INTEGER NOT NULL,
    R_Request_ID         INTEGER NOT NULL REFERENCES R_Request(R_Request_ID),
    R_Status_ID_From     INTEGER REFERENCES R_Status(R_Status_ID),
    R_Status_ID_To       INTEGER NOT NULL REFERENCES R_Status(R_Status_ID),
    Note                 VARCHAR(2000)
);
```

每次拖動卡片自動記錄。誰移的（CreatedBy）、什麼時候（Created）、從哪到哪。

### RK_Request_Type_Config — Request Type 設定

```sql
CREATE TABLE RK_Request_Type_Config (
    RK_Request_Type_Config_ID  SERIAL PRIMARY KEY,
    RK_Request_Type_Config_UU  VARCHAR(36) DEFAULT generate_uuid(),
    AD_Client_ID               INTEGER NOT NULL,
    AD_Org_ID                  INTEGER NOT NULL DEFAULT 0,
    IsActive                   CHAR(1) NOT NULL DEFAULT 'Y',
    Created                    TIMESTAMP NOT NULL DEFAULT NOW(),
    CreatedBy                  INTEGER NOT NULL,
    Updated                    TIMESTAMP NOT NULL DEFAULT NOW(),
    UpdatedBy                  INTEGER NOT NULL,
    R_RequestType_ID           INTEGER NOT NULL REFERENCES R_RequestType(R_RequestType_ID),
    Default_AD_Role_ID         INTEGER REFERENCES AD_Role(AD_Role_ID),
    Default_SalesRep_ID        INTEGER REFERENCES AD_User(AD_User_ID)
);
CREATE UNIQUE INDEX RK_ReqTypeConfig_UQ ON RK_Request_Type_Config(R_RequestType_ID, AD_Client_ID);
```

取代 R_RequestType.Description 塞 JSON。正規的 FK 關聯。

---

## 4. API 設計

### 認證模型（JWT 橋接 ZK Session）

SPA 跑在 ZK Desktop 的 iframe 中，使用者已登入 iDempiere，不需要獨立認證。
但 WAB servlet（`/kanban/`）和 ZK（`/webui/`）是不同 servlet context，不共享 session cookie，
因此用 JWT 橋接 ZK session 的身份資訊。

```
使用者登入 iDempiere（ZK session 已建立）
  → 開啟 Kanban Form
    → FormController（ZK context）直接產生 JWT（JwtUtil.generate()）
      → iframe URL: /kanban/web/index.html#token=JWT
        → SPA 每次 API 呼叫帶 Authorization: Bearer JWT
          → AuthFilter 驗證 JWT → 設定 request attributes
```

**不需要 TokenServlet**——JWT 產生邏輯在 `JwtUtil` 工具類中，FormController 直接呼叫。

### Token 生命週期

| 事件 | 行為 |
|------|------|
| Form 開啟 | FormController 產生 JWT（HMAC-SHA512，secret = `MSysConfig.getValue("REST_TOKEN_SECRET")`，含 AD_Client_ID、AD_Org_ID、AD_User_ID、AD_Role_ID） |
| JWT 過期（預設 1 小時） | SPA 收到 401 → postMessage 請 ZK parent 重新產生 → FormController 用 JwtUtil 產生新 JWT → postMessage 回 SPA |
| ZK session 過期 | 整個 ZK desktop 失效，iframe 跟著失效，使用者需重新登入 |
| 使用者登出 | 同上 |

### API Endpoints

> Path 為 servlet mapping（相對於 `Web-ContextPath: kanban`），完整 URL = `/kanban` + path。
> 不使用 `/api/` 前綴——WAB context path 本身就是 namespace，避免與 core REST API（`/api/v1/`）混淆。

| Method | Path | 用途 | 事務 | Phase |
|--------|------|------|------|-------|
| GET | `/init` | 載入 statuses, config, user info, scope options | 唯讀 | 1 |
| GET | `/cards?scope=&requestTypeId=&closed=` | 取得卡片列表（預設 closed=false 只顯示進行中） | 唯讀 | 1 |
| POST | `/cards/{id}/move` | 移動卡片（更新 status + 寫 move log） | 單一事務 | 1 |
| GET | `/cards/{id}` | 取得單張卡片詳情（含成員、歷程） | 唯讀 | 2 |
| PUT | `/cards/{id}` | 更新卡片（priority, salesrep, dates） | 單一事務 | 2 |
| POST | `/cards` | 新建 Request | 單一事務 | 2 |
| POST | `/cards/{id}/members` | 新增成員 | 單一事務 | 2 |
| DELETE | `/cards/{id}/members/{userId}` | 移除成員 | 單一事務 | 2 |
| GET | `/gantt?scope=&from=&to=` | 甘特圖資料（JSON，非 HTML） | 唯讀 | 2 |
| GET | `/avatar/{userId}` | 使用者頭像圖片 | 唯讀，可快取 | 2 |

> `/cards/*` 由單一 CardsServlet 處理，內部依 HTTP method + pathInfo 分派。

### 關鍵 API 設計原則

1. **卡片移動是一個 API 呼叫**：更新 R_Request.R_Status_ID + 寫入 RK_Card_Move_Log + 觸發 EventAdmin，全在一個事務中
2. **甘特圖回傳 JSON 不是 HTML**：前端用甘特圖庫渲染，支援互動
3. **Avatar 獨立 endpoint**：可設 Cache-Control，避免每次刷新重新載入（Phase 2，Phase 1 用 initials）
4. **Scope 過濾在 server 端**：SQL WHERE 條件，不在前端過濾
5. **單一 Servlet 處理 cards 路由**：CardsServlet 解析 pathInfo 分派 GET/POST/PUT/DELETE，不用多個 servlet
6. **預設只顯示進行中卡片**：利用 R_Status.IsClosed 過濾，看板預設 `closed=false`，歸檔視圖用 `closed=true`（唯讀，不可拖放）

---

## 5. 卡片欄位設計

> 核心場景：ERP 採購/銷售流程中的專案管理。卡片必須讓使用者一眼看到業務脈絡。

### Phase 1 — 卡片摘要（KanbanCard）

| 欄位 | 來源 | 顯示方式 |
|------|------|---------|
| DocumentNo | R_Request | 左上角小字（如 `REQ-0042`） |
| Summary | R_Request | 卡片標題（主要文字） |
| C_BPartner_ID | R_Request → C_BPartner.Name | 業務夥伴名稱（客戶/供應商，卡片副標題） |
| Priority | R_Request | 顏色 badge（Urgent=紅、High=橘、Medium=藍、Low=灰） |
| DueType + DateNextAction | R_Request | 到期 badge（overdue=紅、today=黃、upcoming=綠） |
| SalesRep_ID | R_Request → AD_User.Name | 負責人 avatar + 名稱 |
| R_RequestType_ID | R_Request → R_RequestType.Name | 小標籤（區分採購/銷售/一般） |

Phase 1 的 SQL 多 JOIN C_BPartner 和 R_RequestType，成本低但資訊量大幅提升。

### Phase 2 — 卡片詳情（CardDetail modal）

點擊卡片開啟 modal，顯示完整 ERP 脈絡。所有關聯欄位可點擊 zoom 到 iDempiere 對應視窗。

**基本資訊：**

| 欄位 | 來源 | 可編輯 | Zoom |
|------|------|--------|------|
| DocumentNo | R_Request | ❌ | — |
| Summary | R_Request | ✅ | — |
| Result（備註） | R_Request | ✅ | — |
| R_RequestType_ID | R_Request | ✅ | — |
| R_Category_ID | R_Request | ✅ | — |
| Priority | R_Request | ✅ | — |
| R_Status_ID | R_Request | ✅（下拉） | — |

**人員：**

| 欄位 | 來源 | Zoom |
|------|------|------|
| CreatedBy | R_Request → AD_User | 👤 開單者 |
| AD_User_ID | R_Request → AD_User | 👤 請求者（可能與開單者不同） |
| SalesRep_ID | R_Request → AD_User | 👤 負責人（可編輯） |
| RK_Card_Member | 新表 | 👥 成員列表（Observer/Assignee/Reviewer） |

**ERP 關聯（採購/銷售脈絡）：**

| 欄位 | 來源 | Zoom 目標視窗 | 場景 |
|------|------|-------------|------|
| C_BPartner_ID | R_Request | 業務夥伴 | 客戶投訴、供應商問題 |
| M_Product_ID | R_Request | 產品 | 產品瑕疵、規格問題 |
| C_Order_ID | R_Request | 銷售訂單/採購訂單 | 訂單相關問題追蹤 |
| C_Invoice_ID | R_Request | 發票 | 帳務爭議 |
| C_Payment_ID | R_Request | 付款 | 付款問題 |
| C_Project_ID | R_Request | 專案 | 專案任務管理 |
| C_Campaign_ID | R_Request | 行銷活動 | 活動相關任務 |
| A_Asset_ID | R_Request | 資產 | 設備維護 |

> Zoom 透過 postMessage bridge 觸發 ZoomCommand，在 ZK Desktop 開啟對應視窗。
> 格式：`{type:'zoom', tableName:'C_BPartner', recordId: 117}` → `onZoom {data:['C_BPartner_ID','117']}`

**時間：**

| 欄位 | 來源 | 用途 |
|------|------|------|
| Created | R_Request | 開單時間 |
| DateNextAction | R_Request | 下次動作日（可編輯） |
| StartDate | R_Request | 開始日（甘特圖用，可編輯） |
| EndTime | R_Request | 結束日（甘特圖用，可編輯，有 IModelFactory 保護） |
| CloseDate | R_Request | 結案日 |

**歷程：**

| 資料 | 來源 | 顯示 |
|------|------|------|
| 移動歷程 | RK_Card_Move_Log | 時間軸：誰在什麼時候把卡片從 A 移到 B |
| 附件 | AD_Attachment（core） | 檔案列表 |

---

## 6. SPA 前端設計

### 技術選型

| 項目 | 選擇 | 原因 |
|------|------|------|
| 框架 | React 18 + TypeScript | 與 appointment 一致 |
| 建構 | Vite | 與 appointment 一致 |
| 拖放 | @dnd-kit/react v0.4+（新版 API） | 跨欄拖放用 group prop 自動處理、touch 內建、npm 標記 Stable |
| 甘特圖 | frappe-gantt | 輕量、MIT、支援拖放 |
| 狀態管理 | React Query (TanStack Query) | 快取、樂觀更新、自動重新驗證 |
| 樣式 | Tailwind CSS | 快速開發、一致性 |

### 元件結構

```
src/
├── main.tsx
├── App.tsx                    ← 路由：Kanban / List / Gantt
├── api.ts                     ← API client（複用 appointment 的 token bridge）
├── types.ts
├── hooks/
│   ├── useCards.ts            ← React Query: cards CRUD
│   ├── useGantt.ts            ← React Query: gantt data
│   └── useAuth.ts             ← Token 管理 + postMessage bridge
├── components/
│   ├── KanbanBoard.tsx        ← dnd-kit 拖放看板
│   ├── KanbanColumn.tsx       ← 單一狀態欄
│   ├── KanbanCard.tsx         ← 卡片（avatar、due badge、thumbnail）
│   ├── ListView.tsx           ← 分頁表格
│   ├── GanttView.tsx          ← frappe-gantt 包裝
│   ├── CardDetail.tsx         ← 卡片詳情 modal（成員、歷程、附件）
│   ├── NewRequestDialog.tsx   ← 新建 Request
│   ├── ScopeFilter.tsx        ← Private/Subordinates/Team/All
│   ├── SearchBar.tsx
│   └── UserAvatar.tsx         ← 頭像元件（圖片或 initials）
└── utils/
    └── priority.ts            ← 優先級顏色對應
```

---

## 7. 即時更新機制

保留 OSGi EventAdmin，透過 postMessage 橋接到 SPA：

```
User A 移動卡片
  → API: POST /kanban/cards/{id}/move
    → Server: 更新 DB + EventManager.postEvent("kanban/refresh")
      → All Desktops: OSGi EventHandler 收到
        → 檢查 Desktop 有 KanbanForm + 同 AD_Client_ID
          → Executions.schedule → Clients.evalJavaScript → postMessage to iframe
            → SPA: React Query invalidateQueries → 自動重新載入
```

### 前置條件

- KanbanFormController 開啟時呼叫 `ServerPush.enableServerPush(desktop)`
- FormController 註冊 `DesktopCleanup` listener，關閉時反註冊 EventHandler
- SPA 端不需要 WebSocket——postMessage 從 ZK parent 推送，React Query 負責重新載入

### Graceful Degradation

| 失敗點 | 行為 |
|--------|------|
| EventAdmin 推送失敗 | SPA 不會即時更新，但下次手動操作（切換 scope、拖放）會觸發 React Query refetch |
| Server Push 未啟用 | 同上，降級為手動刷新 |
| JWT 過期 | SPA 收到 401 → postMessage 請求 refresh → 成功則透明續期，失敗則顯示「請重新開啟表單」 |
| API 回傳 500 | 卡片拖放 rollback（樂觀更新回滾）→ 顯示錯誤 toast |

---

## 8. Phase 1 範圍

### 包含

- [ ] 專案骨架（WAB + Maven/Tycho + p2）
- [ ] 認證基礎設施（JwtUtil + AuthFilter，複用 appointment）
- [ ] Migration SQL（3 張新表 + AD_Form + AD_Menu + i18n）
- [ ] InitServlet（statuses, config）
- [ ] CardsServlet（GET cards by scope + POST move + log）
- [ ] React SPA 骨架（Vite + TypeScript）
- [ ] KanbanBoard + KanbanColumn + KanbanCard（dnd-kit）
- [ ] ScopeFilter（四級）
- [ ] 跨 session 即時推送（EventAdmin + postMessage）

### 五層 WBS 展開

詳見 [WBS.md](./WBS.md) — 6 個 L2 交付物、~20 個 L3 子交付物、~50 個 L4 活動、~120 個 L5 原子任務。

### 不包含（Phase 2）

- Gantt View
- List View
- Card Detail modal（成員管理、歷程、附件）
- 新建 Request dialog
- User Avatar endpoint
- 卡片縮圖
- RK_Board / RK_Board_Column（看板設定）
- RK_Request_Type_Config（auto-fill role）
- 搜尋功能

### 移除計劃

移除 bundle 時的行為：
- RK_* 表保留（資料不刪除，避免資料遺失）
- AD_Form、AD_Menu、AD_TreeNodeMM、AD_Form_Access 記錄保留（bundle 不在時選單自動隱藏）
- 翻譯記錄保留（無副作用）
- IModelFactory（MRequestKanban）移除後，core 的 MRequest 自動接管（EndTime 保護失效，但不影響功能）

### Phase 1 預留欄位

Phase 1 Migration 時一併建立，但不實作功能（避免 Phase 2 再跑 migration）：

- `X_KanbanSeqNo`（R_Request 擴充欄位）：欄內卡片排序用，INTEGER DEFAULT 0

### Phase 2+ 功能路線圖

> 基於看板方法論（Kanban Method）的標準能力，按優先級排列。
> RK_Card_Move_Log 是度量類功能的資料基石。

| 優先級 | 功能 | 資料來源 | 依賴 |
|--------|------|---------|------|
| 🔴 高 | 欄內卡片拖放排序 | X_KanbanSeqNo（Phase 1 預留） | Phase 1 |
| 🔴 高 | WIP 限制（軟/硬） | AD_SysConfig 或 RK_Board_Column | Phase 2 RK_Board |
| 🔴 高 | 卡片停滯指標（N 天未移動警告） | RK_Card_Move_Log | Phase 1 |
| 🟡 中 | Cycle Time / Lead Time 圖表 | RK_Card_Move_Log | Phase 1 |
| 🟡 中 | 累積流量圖（CFD） | RK_Card_Move_Log | Phase 1 |
| 🟡 中 | Swimlanes（按專案/負責人/優先級分泳道） | R_Request 現有欄位 | Phase 1 |
| 🟡 中 | Blocked 標記 + 原因 | R_Request.IsEscalated 或 X_ 擴充 | Phase 1 |
| 🟢 低 | 欄位策略（Definition of Done） | RK_Board_Column.Description | Phase 2 RK_Board |
| 🟢 低 | 批次操作（多卡片移動） | 現有 API 擴充 | Phase 1 |
| 🟢 低 | 吞吐量報表 | RK_Card_Move_Log | Phase 1 |

### Phase 1 驗收標準

1. 登入 iDempiere → 開啟 Kanban Form → 看到看板
2. 卡片按 R_Status 分欄顯示
3. 拖放卡片到不同欄位 → 狀態更新 + 移動記錄寫入 RK_Card_Move_Log
4. 另一個 browser tab 即時看到更新
5. Scope 過濾正常（Private/Subordinates/Team/All）
6. 安裝/移除不影響 iDempiere 其他功能

---

## 9. 從 appointment 複用的程式碼

| 檔案 | 複用方式 |
|------|---------|
| AuthFilter.java | 複製 + 改 package + **補上 AD_Role_ID attribute 設定**（appointment 原版缺少） |
| AuthContext.java | 複製 + **新增 `getRoleId(req)`**（appointment 原版只有 3 個 getter） |
| JwtUtil.java | 從 TokenServlet 抽取 `createJwt()` + `validate()` 為工具類（**HS512，secret = REST_TOKEN_SECRET**） |
| NoCacheFilter.java | 直接複製 |
| AppointmentFormController.java | 改名為 KanbanFormController，**移除 httpPost 自呼叫**，改為直接呼叫 JwtUtil |
| AppointmentForm.java | 改名為 KanbanForm，**確保暴露 `getIframe()` 方法** |
| api.ts (SPA) | 複用 token bridge + apptFetch 模式 |

---

## 10. 多租戶安全規則

> iDempiere 是多租戶架構：AD_Client_ID（租戶）→ AD_Org_ID（組織）。
> 所有 API endpoint 必須遵守以下規則，不能造成租戶與組織之間的資料污染。

### 讀取隔離

- 所有查詢必須帶 `AD_Client_ID` 過濾，無例外
- Master data（R_Status、AD_SysConfig、R_RequestType）：`AD_Client_ID IN (0, :clientId)`（system level client=0 對所有租戶可見）
- Transaction data（R_Request、RK_Card_Member、RK_Card_Move_Log）：`AD_Client_ID = :clientId`（交易記錄不會有 client=0）
- `:clientId` 從 JWT 解碼取得，不信任前端傳入
- 組織過濾：使用者 org > 0 時加 `AND (AD_Org_ID = 0 OR AD_Org_ID = :orgId)`；使用者 org = 0（`*`）時不加 org 過濾

### 寫入隔離

- 更新/刪除前驗證 `record.AD_Client_ID == jwt.AD_Client_ID`，不符回傳 403
- 新建記錄的 `AD_Client_ID` 和 `AD_Org_ID` 從 JWT 取得，不從前端 JSON 傳入
- `CreatedBy` / `UpdatedBy` 從 JWT 的 `AD_User_ID` 取得，不寫死 100（SuperUser）

### 認證

- AuthFilter mapping `/*`，內部跳過 `/web/*` 靜態資源路徑
- JWT claims 必須包含：AD_Client_ID、AD_Org_ID、AD_User_ID、AD_Role_ID

### 即時推送隔離

- EventAdmin 事件帶 AD_Client_ID property
- EventHandler 只推送給同 client 的 ZK Desktop

### AD_Org_ID 語義

| 值 | 意義 | 可見範圍 |
|----|------|---------|
| 0 | `*`（所有組織共用） | 該租戶所有組織 |
| > 0 | 特定組織 | 僅該組織 |

### 每個 Endpoint 的隔離 Checklist

| Endpoint | 讀取過濾 | 寫入驗證 | Phase |
|----------|---------|---------|-------|
| GET /init | ✅ client 過濾 statuses/config | — | 1 |
| GET /cards | ✅ client + org + scope 過濾 | — | 1 |
| POST /cards/{id}/move | ✅ 驗證 card 屬於 client | ✅ client 一致 | 1 |
| GET /cards/{id} | ✅ client 過濾 | — | 2 |
| PUT /cards/{id} | ✅ 驗證 card 屬於 client | ✅ client 一致 | 2 |
| POST /cards | — | ✅ client/org 從 JWT | 2 |
| POST /cards/{id}/members | ✅ 驗證 card 屬於 client | ✅ client 一致 | 2 |
| DELETE /cards/{id}/members/{uid} | ✅ 驗證 card 屬於 client | ✅ client 一致 | 2 |
| GET /gantt | ✅ client + org 過濾 | — | 2 |
| GET /avatar/{userId} | ✅ client 過濾 | — | 2 |

---

## 11. 從 requestkanbanform 複用的邏輯

| 邏輯 | 複用方式 |
|------|---------|
| Scope 過濾 SQL（含遞迴 CTE） | 移到 CardsServlet，保持相同 SQL |
| MRequestKanban EndTime 三明治 | 保留 IModelFactory + MRequestKanban |
| StatusConfig from AD_SysConfig | 移到 InitServlet 回傳 |
| 優先級顏色對應 | 移到前端 utils/priority.ts |
| Due badge 計算 | 移到前端（即時計算，不會過期） |
| Avatar 批次載入 | 移到 AvatarServlet（可快取） |
| OSGi EventAdmin 跨 session 推送 | 保留，加 postMessage 橋接 |
| AD_Message i18n | 保留，InitServlet 回傳翻譯 |

---

## 12. 領域知識需求與專家建議

> 開發者在動手前必須理解的領域知識，按架構層級分類。
> 每個知識點附帶專家建議和驗證事項。

### 12.1 iDempiere R_Request 資料模型

#### 關鍵關聯鏈

```
R_RequestType → R_StatusCategory_ID → R_StatusCategory
R_Status      → R_StatusCategory_ID → R_StatusCategory
R_Request     → R_RequestType_ID + R_Status_ID
```

**核心規則**：R_Request 的 R_Status_ID 必須屬於同一個 R_StatusCategory。MRequest.beforeSave() 會驗證，不符時自動重設為預設狀態。

#### InitServlet 查詢 statuses 的正確 SQL

```sql
SELECT s.R_Status_ID, s.Name, s.SeqNo, s.IsOpen, s.IsClosed, s.IsFinalClose
FROM R_Status s
JOIN R_RequestType rt ON rt.R_StatusCategory_ID = s.R_StatusCategory_ID
WHERE rt.R_RequestType_ID = ?
  AND s.IsActive = 'Y'
ORDER BY s.SeqNo
```

#### R_Status 行為（MRequest.beforeSave 自動觸發）

| 狀態旗標 | 自動行為 |
|---------|---------|
| IsOpen='Y' | 自動設定 StartDate（若為 null），清除 CloseDate |
| IsClosed='Y' | 自動設定 CloseDate（若為 null） |
| IsFinalClose='Y' | 設定 Processed='Y'（記錄變唯讀） |

#### Scope 過濾遞迴 CTE

```sql
WITH RECURSIVE subordinates AS (
    SELECT AD_User_ID FROM AD_User
    WHERE Supervisor_ID = :currentUserId AND IsActive = 'Y'
    UNION ALL
    SELECT u.AD_User_ID FROM AD_User u
    JOIN subordinates s ON u.Supervisor_ID = s.AD_User_ID
    WHERE u.IsActive = 'Y'
)
```

- Private：`WHERE r.SalesRep_ID = :currentUserId`
- Subordinates：`WHERE r.SalesRep_ID IN (SELECT AD_User_ID FROM subordinates) OR r.SalesRep_ID = :currentUserId`
- All：不加 SalesRep 過濾

> **⚠️ 專家建議**：Priority 欄位的值是字元不是數字（'1'=Urgent, '3'=High, '5'=Medium, '7'=Low, '9'=Minor）。前端排序和顏色對應要用字元比較。

> **✅ 驗證**：在目標 iDempiere 環境執行 `SELECT DISTINCT Priority FROM R_Request` 確認實際使用的值。

### 12.2 EndTime 保護（MRequestKanban + IModelFactory）

#### 機制

```java
// beforeSave: 暫存 EndTime 到 PO attribute（記憶體）
Object endTime = get_Value("EndTime");
if (endTime != null) set_Attribute("OriginalEndTime", endTime);
// super.beforeSave() 執行 → core 可能清除 EndTime
// afterSave: 用 direct SQL 還原
DB.executeUpdateEx("UPDATE R_Request SET EndTime=? WHERE R_Request_ID=?",
    new Object[]{stashedEndTime, getR_Request_ID()}, get_TrxName());
```

> **⚠️ 專家建議**：IModelFactory 是全域的——註冊後所有載入 R_Request 的程式碼（REST API、Window、Process）都會用 MRequestKanban。確保 MRequestKanban 的 beforeSave/afterSave 不會破壞其他功能。

> **✅ 驗證**：安裝 bundle 後，在 iDempiere 標準 Request 視窗修改一筆有 EndTime 的記錄，確認 EndTime 沒有被清除。

### 12.3 iDempiere Trx 事務管理

#### 推薦模式

```java
// Servlet 中最安全的事務模式——自動 commit/rollback/close
Trx.run(new TrxRunnable() {
    public void run(String trxName) {
        MRequest req = new MRequest(Env.getCtx(), requestId, trxName);
        req.setR_Status_ID(newStatusId);
        req.saveEx();  // 失敗拋 AdempiereException → 自動 rollback
    }
});
```

> **⚠️ 專家建議**：
> - `Trx.run()` 自動處理 commit/rollback/close，不要手動管理 Trx 除非有特殊需求
> - WAB servlet 沒有 ZK context，`Env.getCtx()` 是空的。使用 PO 前必須先設定 context：`Env.setContext(ctx, "#AD_Client_ID", clientId)` 等
> - `trx.close()` 必須在 finally 中呼叫，否則 connection leak。`Trx.run()` 幫你處理這個

> **✅ 驗證**：CardMove 成功時 R_Request + RK_Card_Move_Log 都寫入；失敗時兩者都 rollback（不會出現狀態改了但 log 沒寫的情況）。

### 12.4 ZK Server Push + Executions.schedule

#### 執行緒模型

```
Thread A（Servlet thread，User A 的 move 請求）
  → Trx.run() 更新 DB
  → EventManager.getInstance().sendEvent("kanban/refresh")

Thread B（同一 thread 或 OSGi worker thread）
  → KanbanEventHandler.handleEvent()
  → 遍歷已註冊的 Desktop
  → Executions.schedule(desktop, listener, event)  ← 非阻塞，放入佇列

Thread C（ZK server push thread，User B 的 Desktop）
  → 下一次 polling tick 取出佇列事件
  → 建立 Execution context
  → listener.onEvent() 執行 → Clients.evalJavaScript() → postMessage to iframe
```

#### 關鍵 API

```java
// 1. 啟用 Server Push（在 FormController 初始化時）
desktop.enableServerPush(true);

// 2. 從任意 thread 安全推送到 Desktop
if (desktop.isAlive()) {
    Executions.schedule(desktop, evt -> {
        // 這裡在 ZK execution context 中，可以安全操作 UI
        Clients.evalJavaScript(
            "var iframe = document.querySelector('iframe');" +
            "iframe.contentWindow.postMessage({type:'kanban-refresh'}, '*');"
        );
    }, new Event("onKanbanRefresh"));
}

// 3. postMessage bridge 需要 iframe UUID 定位 widget（在 FormController 中）
String iframeUuid = form.getIframe().getUuid();
String script =
    "(function(){window.addEventListener('message',function(e){" +
    "if(!e.data||!e.data.type)return;" +
    "var w=zk.Widget.$('#" + iframeUuid + "');" +
    "if(e.data.type==='refresh-token'){" +
    "zAu.send(new zk.Event(w,'onTokenRefresh',null));" +
    "}else if(e.data.type==='zoom'){" +
    "zAu.send(new zk.Event(w,'onZoom',{data:[e.data.tableName+'_ID',String(e.data.recordId)]}));" +
    "}});})();";
Clients.evalJavaScript(script);

// 4. Form 關閉時清理（用 onDetach 而非全域 DesktopCleanup）
this.addEventListener("onDetach", evt -> {
    EventManager.getInstance().unregister(this.eventHandler);
    desktop.enableServerPush(false);
});
```

> **⚠️ 專家建議**：
> - iDempiere 用 ZK CE（Community Edition），Server Push 是 **polling** 模式，延遲 1~15 秒。不是即時的。使用者會感受到延遲，這是正常的。
> - `Executions.schedule()` 的 listener 必須輕量——它阻塞該 Desktop 的事件處理。不要在裡面做 DB 查詢。
> - **一定要檢查 `desktop.isAlive()`**，否則推送到已關閉的 Desktop 會拋異常。
> - **一定要在 Form 關閉時 unregister EventHandler**，否則記憶體洩漏 + 推送到死 Desktop。
> - 用 `sendEvent`（同步）而非 `postEvent`（異步）可以降低延遲，但會阻塞發起者的 servlet thread。

> **✅ 驗證**：開兩個 browser tab 各開一個 Kanban Form，在 tab A 拖放卡片，tab B 在 15 秒內看到更新。

### 12.5 @dnd-kit 拖放庫

#### ⚠️ 重要：使用新版 API

PLANNING.md 原本寫 `@dnd-kit/core`，但 @dnd-kit 已有新版 API，**跨欄拖放大幅簡化**：

| | 舊版（@dnd-kit/core） | 新版（@dnd-kit/react） |
|---|---|---|
| 跨欄拖放 | 需手動管理 onDragOver + state | `useSortable` 的 `group` prop 自動處理 |
| Context | `<DndContext>` | `<DragDropProvider>` |
| 安裝 | `@dnd-kit/core` + `@dnd-kit/sortable` | `@dnd-kit/react` + `@dnd-kit/dom` |

#### 看板核心模式（新版 API）

```tsx
// 每張卡片用 useSortable，group = 所屬欄位 ID
function KanbanCard({ id, index, columnId }) {
  const { ref } = useSortable({
    id,
    index,
    group: columnId,      // ← 跨欄拖放的關鍵
    type: 'item',
  });
  return <div ref={ref}>...</div>;
}

// onDragEnd 取得來源欄和目標欄
onDragEnd={(event) => {
  if (event.canceled) return;
  const { source } = event.operation;
  if (isSortable(source)) {
    const { initialGroup, group } = source;
    // initialGroup = 來源欄 ID, group = 目標欄 ID
    if (initialGroup !== group) {
      // 跨欄移動 → 呼叫 API
      moveCard(source.id, group);
    }
  }
}}
```

> **⚠️ 專家建議**：
> - 先確認 `@dnd-kit/react` 的穩定版本是否已發布（截至研究時為 v1.x）。如果尚未穩定，fallback 到 `@dnd-kit/core` + `@dnd-kit/sortable`
> - Column 的 `collisionPriority` 設為 `CollisionPriority.Low`，讓卡片的碰撞偵測優先於欄位
> - Touch 支援是內建的（PointerSensor），不需額外設定
> - `onDragStart` 時做 snapshot（`structuredClone(items)`），`event.canceled` 時還原

> **✅ 驗證**：拖放卡片到另一欄 → UI 立即更新（樂觀）→ API 成功 → 重新載入確認一致。API 失敗 → UI rollback 到原位。

### 12.6 TanStack React Query 樂觀更新

#### 卡片移動的完整模式

```tsx
const queryClient = useQueryClient();

const moveCard = useMutation({
  mutationFn: (payload) =>
    kanbanFetch(`/cards/${payload.id}/move`, {
      method: 'POST',
      body: JSON.stringify({ targetStatusId: payload.targetStatusId }),
    }),

  onMutate: async (payload) => {
    await queryClient.cancelQueries({ queryKey: ['cards'] });
    const previous = queryClient.getQueryData(['cards']);
    queryClient.setQueryData(['cards'], (old) => /* 移動卡片到新欄 */);
    return { previous };
  },

  onError: (_err, _payload, context) => {
    queryClient.setQueryData(['cards'], context.previous);  // rollback
  },

  onSettled: () => {
    queryClient.invalidateQueries({ queryKey: ['cards'] });  // 重新載入
  },
});
```

#### 關鍵 API

| 方法 | 用途 | 時機 |
|------|------|------|
| `cancelQueries({ queryKey })` | 取消進行中的 refetch，避免覆蓋樂觀更新 | onMutate 開頭 |
| `getQueryData(queryKey)` | 同步讀取快取（snapshot） | onMutate |
| `setQueryData(queryKey, updater)` | 同步寫入快取（樂觀更新 / rollback） | onMutate / onError |
| `invalidateQueries({ queryKey })` | 標記過期 + 自動 refetch | onSettled |

> **⚠️ 專家建議**：
> - `onSettled`（不是 `onSuccess`）放 `invalidateQueries`——成功和失敗都要重新載入，確保 UI 與 server 一致
> - `cancelQueries` 在 `onMutate` 開頭呼叫——防止進行中的 refetch 覆蓋你的樂觀更新
> - 樂觀更新的 `setQueryData` updater 必須是 immutable 的（不要 mutate old array）

> **✅ 驗證**：快速連續拖放兩張卡片 → 兩次樂觀更新都正確 → 最終 refetch 結果與 UI 一致。

---

## 13. 開發者注意事項總覽

### 開發前必做

1. **Clone requestkanbanform 原始碼**（`git clone https://github.com/ray-idempiere/tw.idempiere.requestkanbanform`）——Scope CTE SQL、EventAdmin 推送、EndTime 保護的實作參考
2. **確認 @dnd-kit/react 版本**——v0.4.0 已標記 Stable（npm 週下載 49 萬），可直接使用
3. **在目標 iDempiere 環境確認 R_Status 設定**——`SELECT * FROM R_Status WHERE R_StatusCategory_ID = (SELECT R_StatusCategory_ID FROM R_RequestType WHERE R_RequestType_ID = ?) ORDER BY SeqNo`

### 開發中必記

4. **WAB servlet 沒有 Env context**——每個 servlet 方法開頭要 `Env.setContext(ctx, "#AD_Client_ID", AuthContext.getClientId(req))` 等
5. **Trx.run(TrxRunnable) 是唯一推薦的事務模式**——不要手動管理 Trx
6. **desktop.isAlive() 必須檢查**——推送前、每次 Executions.schedule() 前
7. **Form 關閉必須 unregister EventHandler**——否則記憶體洩漏

### 驗證 Checklist

| # | 驗證項目 | 預期結果 |
|---|---------|---------|
| 1 | `mvn verify` | 建置成功，p2 repository 產出 |
| 2 | Felix Console bundle 狀態 | Active |
| 3 | 非 en_US 語系選單可見 | 翻譯記錄生效 |
| 4 | 看板載入 | 只顯示 IsClosed='N' 的卡片 |
| 5 | 卡片顯示 BPartner + RequestType | SQL JOIN 正確 |
| 6 | 拖放卡片 | R_Status_ID 更新 + RK_Card_Move_Log 寫入（同一事務） |
| 7 | 拖放失敗 rollback | UI 回到原位，DB 無變更 |
| 8 | 跨 tab 即時推送 | 15 秒內另一 tab 看到更新 |
| 9 | Scope 切換 | Private 只看自己、Subordinates 看部屬、All 看全部 |
| 10 | 多租戶隔離 | 不同 Client 看不到彼此的卡片 |
| 11 | EndTime 保護 | 標準 Request 視窗修改記錄後 EndTime 不被清除 |
| 12 | Bundle 移除 | iDempiere 其他功能正常 |
| 13 | JWT 過期 | SPA 收到 401 → postMessage refresh → 透明續期 |
