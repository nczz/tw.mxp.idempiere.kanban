# tw.mxp.idempiere.kanban — Phase 1 WBS（五層展開）

> 基於 PLANNING.md Phase 1 範圍，結合 iDempiere SPA Plugin Playbook、Maven/Tycho Build、AD Records Checklist 等實戰經驗展開。
> 每個 L5 任務都是可獨立驗證的原子工作項。

---

## WBS 結構總覽

| Level | 意義 | 數量 |
|-------|------|------|
| L1 | 專案 | 1 |
| L2 | 交付物（Work Package） | 6 |
| L3 | 子交付物 | ~20 |
| L4 | 工作活動 | ~50 |
| L5 | 原子任務 | ~120 |

---

## 1. 專案骨架與建置基礎設施

> 對應 Playbook Step 1~2。產出：可 `mvn verify` 通過的空 WAB bundle。

### 1.1 Maven/Tycho 專案結構

#### 1.1.1 Root pom.xml
- 1.1.1.1 定義 groupId `tw.idempiere`、artifactId `tw.mxp.idempiere.kanban.root`
- 1.1.1.2 列出三個 modules：parent / plugin / p2
- 1.1.1.3 設定 `<packaging>pom</packaging>`

#### 1.1.2 Parent pom.xml（tw.mxp.idempiere.kanban.parent）
- 1.1.2.1 設定 Tycho 4.0.8 pluginManagement（`tycho-maven-plugin`、`target-platform-configuration`）
- 1.1.2.2 設定 target platform repositories（iDempiere p2 + Eclipse archive URL `archive.eclipse.org`）
- 1.1.2.3 加入 `extraRequirements` for `org.eclipse.core.runtime`（Tycho resolver 需要）
- 1.1.2.4 設定 `idempiere.core.repository.url` property（可 `-D` 覆蓋）
- 1.1.2.5 設定 Java 17 compiler source/target

#### 1.1.3 Plugin pom.xml（tw.mxp.idempiere.kanban）
- 1.1.3.1 設定 `<packaging>eclipse-plugin</packaging>`
- 1.1.3.2 parent 指向 tw.mxp.idempiere.kanban.parent

#### 1.1.4 p2 Repository pom.xml + category.xml
- 1.1.4.1 設定 `<packaging>eclipse-repository</packaging>`
- 1.1.4.2 建立 category.xml 定義 feature category
- 1.1.4.3 設定 `includeAllDependencies=false`（避免打包 iDempiere 已有的依賴）

### 1.2 MANIFEST.MF（WAB 設定）

#### 1.2.1 Bundle 基本 headers
- 1.2.1.1 `Bundle-SymbolicName: tw.mxp.idempiere.kanban;singleton:=true`
- 1.2.1.2 `Bundle-Version: 14.0.0.qualifier`
- 1.2.1.3 `Bundle-Activator: tw.mxp.idempiere.kanban.KanbanActivator`

#### 1.2.2 WAB 必要 headers
- 1.2.2.1 `Web-ContextPath: kanban`（URL 前綴 `/kanban/`）
- 1.2.2.2 `Jetty-Environment: ee8`（⚠️ 致命：缺少則所有 servlet 404 且無錯誤訊息）
- 1.2.2.3 `Eclipse-BundleShape: dir`（exploded bundle，靜態資源需要）
- 1.2.2.4 `Bundle-ActivationPolicy: lazy`

#### 1.2.3 依賴宣告
- 1.2.3.1 `Import-Package`：javax.servlet、javax.crypto、org.osgi.framework、org.zkoss.zk.ui 等（不用 Require-Bundle，避免傳遞依賴地獄）
- 1.2.3.2 `Service-Component: OSGI-INF/*.xml`

### 1.3 WEB-INF/web.xml

#### 1.3.1 Filter 定義
- 1.3.1.1 AuthFilter mapping `/*`，內部跳過 `/web/*` 靜態資源路徑
- 1.3.1.2 NoCacheFilter mapping（`/web/*`，確保 SPA 更新不被快取）

#### 1.3.2 Servlet 定義
- 1.3.2.1 InitServlet → `/init`
- 1.3.2.2 CardsServlet → `/cards/*`（單一 servlet 處理所有 cards 路由，內部依 method + pathInfo 分派）

### 1.4 build.properties
- 1.4.1 `source.. = src/`
- 1.4.2 `output.. = target/classes/`
- 1.4.3 `bin.includes = META-INF/, ., OSGI-INF/, WEB-INF/, web/`（⚠️ 必須包含 `.` 否則 JAR 無 class）

### 1.5 OSGI-INF
- 1.5.1 KanbanActivator component XML
- 1.5.2 KanbanEventHandler component XML（即時推送用）

### 1.6 建置驗證
- 1.6.1 `mvn verify` 通過（空 bundle，無 Java 錯誤）
- 1.6.2 p2 repository 產出在 `tw.mxp.idempiere.kanban.p2/target/repository/`
- 1.6.3 JAR 內容檢查：`unzip -l` 確認 MANIFEST、class、web 資源都在

---

## 2. 資料庫與 AD 記錄

> 對應 Playbook Step 3。產出：Migration SQL 檔案 + Activator 中的 syncColumns()。
> 建立順序嚴格遵守外鍵依賴：AD_Element → AD_Reference → AD_Column → AD_Table → AD_Form → AD_Menu → AD_TreeNodeMM → AD_Form_Access → 翻譯記錄。

### 2.1 新建資料表 DDL（Java syncColumns）

#### 2.1.1 RK_Card_Member 表
- 2.1.1.1 AD_Element 建立：RK_Card_Member_ID、RK_Card_Member_UU、MemberRole
- 2.1.1.2 AD_Table 建立：RK_Card_Member（AccessLevel=3 Client+Org）
- 2.1.1.3 AD_Column 建立：所有欄位（R_Request_ID FK、AD_User_ID FK、MemberRole）
- 2.1.1.4 AD_Reference + AD_Ref_List 建立：MemberRole 清單（Observer/Assignee/Reviewer）
- 2.1.1.5 syncColumns() 中用 `MColumn.getSQLAdd()` 建立實體欄位（⚠️ 不用 ALTER TABLE SQL）
- 2.1.1.6 建立 unique index `RK_Card_Member_UQ(R_Request_ID, AD_User_ID) WHERE IsActive='Y'`

#### 2.1.2 RK_Card_Move_Log 表
- 2.1.2.1 AD_Element 建立：RK_Card_Move_Log_ID、RK_Card_Move_Log_UU、R_Status_ID_From、R_Status_ID_To、Note
- 2.1.2.2 AD_Table 建立：RK_Card_Move_Log（AccessLevel=3）
- 2.1.2.3 AD_Column 建立：所有欄位（R_Request_ID FK、R_Status_ID_From FK、R_Status_ID_To FK、Note）
- 2.1.2.4 syncColumns() 建立實體欄位

#### 2.1.3 RK_Request_Type_Config 表（Phase 1 只建表，不實作 auto-fill）
- 2.1.3.1 AD_Element 建立：RK_Request_Type_Config_ID、RK_Request_Type_Config_UU
- 2.1.3.2 AD_Table 建立：RK_Request_Type_Config（AccessLevel=3）
- 2.1.3.3 AD_Column 建立：R_RequestType_ID FK、Default_AD_Role_ID FK、Default_SalesRep_ID FK
- 2.1.3.4 syncColumns() 建立實體欄位
- 2.1.3.5 建立 unique index `RK_ReqTypeConfig_UQ(R_RequestType_ID, AD_Client_ID)`

#### 2.1.4 R_Request 預留欄位（只建 AD_Column，不實作功能）
- 2.1.4.1 AD_Element 建立：X_KanbanSeqNo
- 2.1.4.2 AD_Column 建立：R_Request 表的 X_KanbanSeqNo（INTEGER DEFAULT 0）
- 2.1.4.3 syncColumns() 建立實體欄位
- 2.1.4.4 目的：Phase 2 欄內卡片排序用，Phase 1 預留避免日後再跑 migration

### 2.2 AD_Form + AD_Menu 註冊

#### 2.2.1 AD_Form 記錄
- 2.2.1.1 INSERT AD_Form：Name='Request Kanban'、Classname='tw.mxp.idempiere.kanban.KanbanFormController'、AccessLevel='3'
- 2.2.1.2 UUID 冪等：`WHERE NOT EXISTS (SELECT 1 FROM AD_Form WHERE AD_Form_UU = '...')`

#### 2.2.2 AD_Menu 記錄
- 2.2.2.1 INSERT AD_Menu：Name='Request Kanban'、Action='X'、AD_Form_ID=上一步的 ID
- 2.2.2.2 INSERT AD_TreeNodeMM：Parent_ID 指向現有資料夾（⚠️ 不能是 0，root 只放 IsSummary='Y'）
- 2.2.2.3 AD_Tree_ID 從 `AD_ClientInfo.AD_Tree_Menu_ID` 查（通常是 10）

#### 2.2.3 角色權限
- 2.2.3.1 INSERT AD_Form_Access：為每個需要的 AD_Role_ID 建立一筆（IsReadWrite='Y'）

### 2.3 翻譯記錄（i18n）

#### 2.3.1 AD_Menu_Trl
- 2.3.1.1 為所有 `IsSystemLanguage='Y'` 的語言建立翻譯記錄（⚠️ 致命：缺少則非 en_US 語系看不到選單）
- 2.3.1.2 使用 `SELECT ... FROM AD_Language WHERE IsActive='Y' AND IsSystemLanguage='Y'` 動態產生

#### 2.3.2 AD_Form_Trl
- 2.3.2.1 同上模式，為 AD_Form 建立翻譯記錄

### 2.4 Activator 整合

#### 2.4.1 KanbanActivator extends Incremental2PackActivator
- 2.4.1.1 `start()` 呼叫 `super.start()` 觸發 2Pack
- 2.4.1.2 `afterPackIn()` 動態取得 IMappedFormFactory（⚠️ SCR @Reference 在 WAB 不觸發，用 getContext().getServiceReference()）
- 2.4.1.3 `afterPackIn()` 呼叫 `mappedFormFactory.scan(context, "tw.mxp.idempiere.kanban")`
- 2.4.1.4 `afterPackIn()` 執行 migration SQL（DML only，用 `DB.executeUpdateEx()`）
- 2.4.1.5 `afterPackIn()` 呼叫 `syncColumns()`（DDL via Java）
- 2.4.1.6 Migration 版本追蹤：`isMigrationApplied("12.0.0")` 防重複執行

---

## 3. 後端 API（Servlet 層）

> 對應 Playbook Step 5~6。產出：JwtUtil + AuthFilter + InitServlet + CardsServlet。
> 所有 Servlet 跑在 Jetty HTTP thread，沒有 ZK session context。`Env.getCtx()` 是空的。

### 3.1 認證基礎設施

#### 3.1.1 JwtUtil.java（從 appointment TokenServlet 抽取）
- 3.1.1.1 `generate(clientId, orgId, userId, roleId)` → JWT string（**HMAC-SHA512**，與 iDempiere REST API 一致）
- 3.1.1.2 `validate(token)` → Claims（AD_Client_ID、AD_Org_ID、AD_User_ID、AD_Role_ID）
- 3.1.1.3 JWT secret：`MSysConfig.getValue("REST_TOKEN_SECRET", "")`（共用 iDempiere REST API 的 secret）
- 3.1.1.4 JWT header：`{"alg":"HS512","typ":"JWT","kid":"idempiere"}`
- 3.1.1.5 JWT claims：sub、AD_Client_ID、AD_User_ID、AD_Role_ID、AD_Org_ID、AD_Session_ID、iss、exp
- 3.1.1.6 預設過期時間 1 小時（appointment 用 7 天，kanban 改短 + refresh 機制更安全）

#### 3.1.2 AuthFilter.java（複用 appointment）
- 3.1.2.1 從 `Authorization: Bearer` header 取 token → `JwtUtil.validate()`
- 3.1.2.2 設定 request attributes：AD_Client_ID、AD_Org_ID、AD_User_ID、AD_Role_ID（appointment 原版缺 AD_Role_ID，kanban 需補上）
- 3.1.2.3 mapping `/*`，內部跳過 `/web/*` 靜態資源路徑

#### 3.1.3 AuthContext.java（複用 appointment，補 getRoleId）
- 3.1.3.1 靜態方法：`getClientId(req)`、`getOrgId(req)`、`getUserId(req)`、`getRoleId(req)`
- 3.1.3.2 從 request attribute 讀取（不從 Env.getCtx()，WAB 沒有 ZK context）
- 3.1.3.3 ⚠️ appointment 原版只有 3 個 getter（無 getRoleId），kanban 需新增

#### 3.1.4 NoCacheFilter.java（複用 appointment）
- 3.1.4.1 設定 `Cache-Control: no-cache, no-store, must-revalidate`
- 3.1.4.2 mapping `/web/*`（SPA 靜態檔）

### 3.2 InitServlet

#### 3.2.1 GET `/init` 回傳結構
- 3.2.1.1 查詢 R_Status：`SELECT * FROM R_Status WHERE R_StatusCategory_ID IN (SELECT R_StatusCategory_ID FROM R_RequestType WHERE AD_Client_ID IN (0, ?)) ORDER BY SeqNo`
- 3.2.1.2 查詢 StatusConfig from AD_SysConfig：欄位顏色、WIP 限制等設定
- 3.2.1.3 查詢 user info：當前使用者名稱、角色、可用 scope 選項
- 3.2.1.4 查詢 R_RequestType 列表（scope filter 需要）
- 3.2.1.5 查詢 AD_Message 翻譯：`SELECT MsgText FROM AD_Message WHERE AD_Language=? AND Value IN (...)`

#### 3.2.2 回傳 JSON 格式設計
- 3.2.2.1 `statuses[]`：id、name、seqNo、color（from AD_SysConfig）
- 3.2.2.2 `requestTypes[]`：id、name
- 3.2.2.3 `user`：id、name、roleId
- 3.2.2.4 `messages{}`：i18n key-value pairs
- 3.2.2.5 `config{}`：wipLimit、defaultScope 等

### 3.3 CardsServlet（單一 servlet 處理所有 /cards/* 路由）

#### 3.3.1 路由分派
- 3.3.1.1 `doGet`：pathInfo 為空 → 卡片列表；pathInfo 為 `/{id}` → 單張卡片（Phase 2）
- 3.3.1.2 `doPost`：pathInfo 含 `/move` → 移動卡片；pathInfo 為空 → 新建（Phase 2）
- 3.3.1.3 `doPut`：pathInfo 為 `/{id}` → 更新卡片（Phase 2）

#### 3.3.2 GET 卡片列表查詢邏輯
- 3.3.2.1 接收參數：`scope`（Private/Subordinates/Team/All）、`requestTypeId`、`closed`（預設 false）
- 3.3.2.2 狀態過濾：`JOIN R_Status s ON r.R_Status_ID = s.R_Status_ID AND s.IsClosed = ?`（closed=false → 'N'，closed=true → 'Y'）
- 3.3.2.3 Scope 過濾 SQL 建構（複用 requestkanbanform 的遞迴 CTE）

#### 3.3.3 Scope SQL 實作
- 3.3.3.1 Private scope：`WHERE r.AD_Client_ID = ? AND r.SalesRep_ID = ?`
- 3.3.3.2 Subordinates scope：`WHERE r.AD_Client_ID = ?` + 遞迴 CTE 查部屬 `WITH RECURSIVE subordinates AS (...)`
- 3.3.3.3 Team scope：`WHERE r.AD_Client_ID = ?` + 同 R_RequestType 的所有 SalesRep
- 3.3.3.4 All scope：`WHERE r.AD_Client_ID = ?`（R_Request 是交易記錄，AD_Client_ID 不會是 0，不需 `IN (0, ?)`）
- 3.3.3.5 所有 scope 的 org 過濾：使用者 org > 0 時加 `AND (r.AD_Org_ID = 0 OR r.AD_Org_ID = ?)`

#### 3.3.4 卡片資料組裝
- 3.3.4.1 主查詢：R_Request JOIN R_Status JOIN C_BPartner JOIN R_RequestType，取 DocumentNo、Summary、Priority、DueType、DateNextAction、SalesRep_ID、BPartner Name、RequestType Name
- 3.3.4.2 批次載入 avatar URL：`SELECT AD_User_ID, AD_Image_ID FROM AD_User WHERE AD_User_ID IN (...)`（避免 N+1）
- 3.3.4.3 計算 due badge：overdue / due today / upcoming（server-side 計算，前端顯示）
- 3.3.4.4 回傳 JSON：按 R_Status_ID 分組的卡片陣列

#### 3.3.5 多租戶安全
- 3.3.5.1 Master data（R_Status）查詢帶 `AD_Client_ID IN (0, :clientId)`；Transaction data（R_Request）帶 `AD_Client_ID = :clientId`
- 3.3.5.2 AD_Client_ID 和 AD_Org_ID 從 AuthContext 取（不信任前端傳入）

### 3.4 卡片移動邏輯（CardsServlet POST /cards/{id}/move）

#### 3.4.1 POST `/cards/{id}/move` 事務邏輯
- 3.4.1.1 接收 JSON：`{targetStatusId: int}`
- 3.4.1.2 驗證：卡片存在 + 屬於當前 client + 使用者有權限
- 3.4.1.3 開啟 Trx（單一事務）

#### 3.4.2 狀態更新
- 3.4.2.1 讀取 R_Request（`MRequest.get()`）
- 3.4.2.2 記錄舊 R_Status_ID
- 3.4.2.3 設定新 R_Status_ID + UpdatedBy = AuthContext.getUserId()
- 3.4.2.4 `request.saveEx(trxName)`

#### 3.4.3 移動記錄寫入
- 3.4.3.1 INSERT RK_Card_Move_Log：R_Request_ID、R_Status_ID_From、R_Status_ID_To、CreatedBy
- 3.4.3.2 CreatedBy 從 JWT 的 AD_User_ID 取（⚠️ 不能寫死 100 SuperUser）

#### 3.4.4 事件發布
- 3.4.4.1 `IEventManager.postEvent("kanban/refresh", properties)`
- 3.4.4.2 properties 包含 AD_Client_ID（讓 handler 過濾）
- 3.4.4.3 commit Trx

#### 3.4.5 錯誤處理
- 3.4.5.1 Trx rollback on exception
- 3.4.5.2 回傳 JSON error：`{error: "message"}`
- 3.4.5.3 ⚠️ PostgreSQL 錯誤訊息含 `\n`，必須 escape 後放入 JSON

---

## 4. ZK Form 整合

> 對應 Playbook Step 7。產出：KanbanFormController + KanbanForm + postMessage bridge。
> ⚠️ 核心避雷：`Executions.getCurrent()` 只在 FormController constructor 安全。

### 4.1 KanbanFormController（@Form）

#### 4.1.1 類別結構
- 4.1.1.1 `@Form` annotation（IMappedFormFactory 自動註冊）
- 4.1.1.2 `implements IFormController`
- 4.1.1.3 Constructor 中建立 KanbanForm 實例

#### 4.1.2 JWT 產生
- 4.1.2.1 在 constructor 中從 ZK context 取得：AD_Client_ID、AD_Org_ID、AD_User_ID、AD_Role_ID
- 4.1.2.2 呼叫 `JwtUtil.generate(clientId, orgId, userId, roleId)` 直接產生 JWT（不需 HTTP call）
- 4.1.2.3 ⚠️ AD_Org_ID 從 `Env.getContextAsInt(ctx, "#AD_Org_ID")` 取，不從 AD_Session 表（AD_Session.AD_Org_ID 可能是 0）

#### 4.1.3 iframe URL 建構
- 4.1.3.1 用 `Executions.getCurrent()` 取 scheme + serverName + serverPort（⚠️ 只在 constructor 安全）
- 4.1.3.2 組合完整 URL：`http://host:port/kanban/web/index.html#token=JWT`
- 4.1.3.3 傳給 `form.loadSpa(fullUrl)`（Form 只接收字串，不做路徑邏輯）

#### 4.1.4 postMessage bridge 註冊
- 4.1.4.1 `Selectors.wireEventListeners(form, this)` 綁定事件
- 4.1.4.2 監聽 SPA 的 `zoom` 訊息 → 觸發 `ZoomCommand`（`onZoom` 事件 + `{data: [columnName, recordId]}`）
- 4.1.4.3 監聽 SPA 的 `refresh-token` 訊息 → 重新產生 JWT 並 postMessage 回去

### 4.2 KanbanForm（CustomForm）

#### 4.2.1 UI 結構
- 4.2.1.1 extends `CustomForm`
- 4.2.1.2 建立 Iframe component（`new Iframe()`）
- 4.2.1.3 設定 Iframe 100% width/height
- 4.2.1.4 暴露 `getIframe()` 方法（FormController 的 postMessage bridge 需要 `iframe.getUuid()` 定位 widget）

#### 4.2.2 loadSpa 方法
- 4.2.2.1 `iframe.setSrc(fullUrl)`（接收完整 URL，不做任何路徑處理）
- 4.2.2.2 ⚠️ 不在 Form 裡呼叫 `Executions.getCurrent()`（加入最愛、序列化時 NPE）

### 4.3 postMessage 橋接（Client-side JS）

#### 4.3.1 ZK → SPA 方向（server push）
- 4.3.1.1 FormController 收到 OSGi event → `Executions.schedule()` 在 ZK desktop thread 執行
- 4.3.1.2 `Clients.evalJavaScript()` 注入 `postMessage("kanban-refresh")` 到 iframe

#### 4.3.2 SPA → ZK 方向（zoom）
- 4.3.2.1 SPA 呼叫 `window.parent.postMessage({type:'zoom', tableName, recordId})`
- 4.3.2.2 FormController 的 client-side listener 收到 → `zAu.send(new zk.Event(widget, 'onZoom', {data: [columnName, recordId]}))`
- 4.3.2.3 ZoomCommand AuService 攔截 → `AEnv.zoom(query)` 開啟記錄視窗

---

## 5. React SPA 前端

> 對應 Playbook Step 8。產出：Vite + React + TypeScript + dnd-kit 看板。

### 5.1 Vite 專案初始化

#### 5.1.1 專案建立
- 5.1.1.1 `npm create vite@latest spa -- --template react-ts`
- 5.1.1.2 安裝依賴：`@dnd-kit/react`、`@dnd-kit/dom`、`@dnd-kit/collision`、`@tanstack/react-query`、`tailwindcss`（若 @dnd-kit/react 尚未穩定，fallback 到 `@dnd-kit/core` + `@dnd-kit/sortable`）

#### 5.1.2 vite.config.ts
- 5.1.2.1 `base: './'`（相對路徑，iframe 內使用）
- 5.1.2.2 `build.outDir: '../tw.mxp.idempiere.kanban/web/spa'`
- 5.1.2.3 `build.emptyOutDir: true`

#### 5.1.3 Tailwind CSS 設定
- 5.1.3.1 `tailwind.config.js` + `postcss.config.js`
- 5.1.3.2 content 指向 `src/**/*.{ts,tsx}`

### 5.2 API Client 與認證

#### 5.2.1 Token 管理（useAuth hook）
- 5.2.1.1 從 URL hash 解析 JWT token（`window.location.hash`）
- 5.2.1.2 token 存入 state（不存 localStorage，iframe 生命週期內有效）
- 5.2.1.3 監聽 parent 的 `postMessage` 接收新 token（refresh-token）

#### 5.2.2 API fetch wrapper（api.ts）
- 5.2.2.1 context path 從 SPA URL 推導：`'/' + window.location.pathname.split('/')[1]` → `/kanban`（不硬編碼）
- 5.2.2.2 `kanbanFetch(path, options)` — 自動帶 context path + `Authorization: Bearer` header
- 5.2.2.3 base URL = `window.location.origin` + contextPath（同源，不需跨域）
- 5.2.2.4 401/403 處理：postMessage 請求 refresh token
- 5.2.2.5 JSON error 解析與統一錯誤處理

### 5.3 型別定義（types.ts）

#### 5.3.1 核心型別
- 5.3.1.1 `Card`：id、documentNo、summary、bpartnerName、requestTypeName、priority、dueType、dateNextAction、salesRepId、salesRepName、statusId
- 5.3.1.2 `Status`：id、name、seqNo、color
- 5.3.1.3 `InitData`：statuses、requestTypes、user、messages、config
- 5.3.1.4 `MovePayload`：cardId、targetStatusId

### 5.4 React Query Hooks

#### 5.4.1 useInit hook
- 5.4.1.1 `useQuery(['init'], () => kanbanFetch('/init'))`
- 5.4.1.2 staleTime 設長（init 資料不常變）

#### 5.4.2 useCards hook
- 5.4.2.1 `useQuery(['cards', scope, requestTypeId], () => kanbanFetch('/cards?scope=...'))`
- 5.4.2.2 支援 scope 和 requestType 參數變化時自動重新查詢

#### 5.4.3 useMoveCard hook
- 5.4.3.1 `useMutation` 呼叫 POST `/cards/{id}/move`
- 5.4.3.2 樂觀更新：拖放時立即更新 UI，API 失敗時 rollback
- 5.4.3.3 成功後 `invalidateQueries(['cards'])` 重新載入

### 5.5 看板元件

#### 5.5.1 KanbanBoard.tsx
- 5.5.1.1 `DndContext` provider（@dnd-kit/core）
- 5.5.1.2 `onDragEnd` handler：取得 `active.id`（cardId）和 `over.id`（targetStatusId）→ 呼叫 useMoveCard
- 5.5.1.3 水平排列 KanbanColumn（flex layout）
- 5.5.1.4 collision detection strategy：`closestCorners`

#### 5.5.2 KanbanColumn.tsx
- 5.5.2.1 `useDroppable({ id: status.id })`
- 5.5.2.2 欄位標題：status.name + 卡片數量 badge
- 5.5.2.3 欄位背景色：from status.color
- 5.5.2.4 垂直排列 KanbanCard

#### 5.5.3 KanbanCard.tsx
- 5.5.3.1 `useDraggable({ id: card.id })`
- 5.5.3.2 卡片佈局：DocumentNo（左上小字）、Summary（標題）、BPartner Name（副標題）、RequestType 標籤
- 5.5.3.3 Priority 顏色 badge（utils/priority.ts）：Urgent=red、High=orange、Medium=blue、Low=gray
- 5.5.3.4 Due badge：overdue=red、today=yellow、upcoming=green
- 5.5.3.5 底部：SalesRep avatar + 名稱
- 5.5.3.6 拖動時的 overlay 樣式（DragOverlay）
- 5.5.3.7 點擊卡片 → Phase 2 開啟 CardDetail modal（Phase 1 先不做）

### 5.6 ScopeFilter 元件

#### 5.6.1 ScopeFilter.tsx
- 5.6.1.1 四個選項：Private / Subordinates / Team / All
- 5.6.1.2 選項從 InitData 取得（server 決定使用者可用的 scope）
- 5.6.1.3 onChange 觸發 useCards 重新查詢

#### 5.6.2 RequestType 過濾
- 5.6.2.1 下拉選單：從 InitData.requestTypes 取得選項
- 5.6.2.2 預設「全部」

#### 5.6.3 視圖切換（看板 / 歸檔）
- 5.6.3.1 Toggle：「進行中」（預設）/ 「已結案」
- 5.6.3.2 進行中 → `closed=false`，看板可拖放
- 5.6.3.3 已結案 → `closed=true`，看板唯讀（禁用拖放），卡片按結案狀態分欄

### 5.7 App.tsx 主框架

#### 5.7.1 QueryClientProvider
- 5.7.1.1 React Query client 設定
- 5.7.1.2 預設 staleTime、retry 策略

#### 5.7.2 Layout
- 5.7.2.1 頂部工具列：ScopeFilter + RequestType filter
- 5.7.2.2 主區域：KanbanBoard（100% 剩餘高度）

#### 5.7.3 postMessage 監聽
- 5.7.3.1 `useEffect` 註冊 `window.addEventListener('message', handler)`
- 5.7.3.2 收到 `kanban-refresh` → `queryClient.invalidateQueries(['cards'])`
- 5.7.3.3 收到 `token-refresh` → 更新 token state

---

## 6. 即時推送與整合驗證

> 對應 Playbook Step 9~10 + PLANNING.md 驗收標準。

### 6.1 OSGi EventAdmin 跨 session 推送

#### 6.1.1 KanbanEventHandler（AbstractEventHandler）
- 6.1.1.1 `@Component(immediate = true)` + OSGI-INF XML
- 6.1.1.2 `@Reference(service = IEventManager.class)` bind/unbind
- 6.1.1.3 `initialize()` 中 `registerEvent("kanban/refresh")`

#### 6.1.2 事件處理邏輯
- 6.1.2.1 `doHandleEvent()` 收到 `kanban/refresh` 事件
- 6.1.2.2 從 event properties 取 AD_Client_ID（過濾：只推送給同 client 的 desktop）
- 6.1.2.3 遍歷所有 ZK Desktop → 找到有 KanbanForm 的 desktop
- 6.1.2.4 `Executions.schedule(desktop, ...)` 在目標 desktop 的 thread 執行 postMessage

#### 6.1.3 Server Push 啟用
- 6.1.3.1 KanbanFormController 中 `DesktopCleanup` listener 註冊/反註冊
- 6.1.3.2 `ServerPush.enableServerPush(desktop)` 啟用（如果尚未啟用）

### 6.2 EndTime 保護（IMappedModelFactory）

#### 6.2.1 MRequestKanban 模型
- 6.2.1.1 extends MRequest，加 `@Model` annotation
- 6.2.1.2 override `beforeSave()`：stash EndTime 到 PO attribute
- 6.2.1.3 override `afterSave()`：用 direct SQL 還原 EndTime
- 6.2.1.4 在 KanbanActivator.afterPackIn() 中動態取得 IMappedModelFactory 並 scan model package（⚠️ 與 IMappedFormFactory 相同，@Reference 在 WAB 不觸發）

### 6.3 整合測試

#### 6.3.1 建置驗證
- 6.3.1.1 `cd spa && npm run build`：SPA 產出到 `tw.mxp.idempiere.kanban/web/spa/`
- 6.3.1.2 `mvn verify`：OSGi bundle + p2 repository 建置成功
- 6.3.1.3 JAR 內容檢查：MANIFEST headers、class 檔案、web 資源、OSGI-INF、WEB-INF

#### 6.3.2 部署驗證
- 6.3.2.1 `update-prd.sh` 安裝 bundle（⚠️ 不用 `update-rest-extensions.sh`，那個硬編碼了 REST API bundle name）
- 6.3.2.2 Felix Web Console 確認 bundle 狀態 = Active
- 6.3.2.3 重啟 iDempiere

#### 6.3.3 功能驗收（對應 PLANNING.md 驗收標準）
- 6.3.3.1 ✅ 登入 iDempiere → 選單中看到 Request Kanban → 開啟 Form → iframe 載入 SPA
- 6.3.3.2 ✅ 卡片按 R_Status 分欄顯示（每欄標題 = status name）
- 6.3.3.3 ✅ 拖放卡片到不同欄位 → R_Request.R_Status_ID 更新 + RK_Card_Move_Log 寫入
- 6.3.3.4 ✅ 另一個 browser tab 開啟同一 Form → 即時看到卡片移動
- 6.3.3.5 ✅ Scope 過濾：切換 Private/Subordinates/Team/All → 卡片列表正確變化
- 6.3.3.6 ✅ 移除 bundle 後 iDempiere 其他功能不受影響

#### 6.3.4 非 en_US 語系驗證
- 6.3.4.1 切換到 zh_TW 語系 → 選單項目可見（翻譯記錄生效）
- 6.3.4.2 Form 標題顯示中文名稱

#### 6.3.5 多租戶驗證
- 6.3.5.1 不同 Client 的使用者只看到自己 Client 的卡片
- 6.3.5.2 跨 Client 的 move 操作被 server 端擋住

---

## 依賴關係與執行順序

```
1. 專案骨架 ──────────────────────────────────────┐
   （可獨立完成，不依賴其他 WP）                      │
                                                    ▼
2. 資料庫與 AD 記錄 ──┐                         3. 後端 API
   （依賴 1 的專案結構）│                            （依賴 1 的專案結構）
                       │                              │
                       ▼                              ▼
                  4. ZK Form 整合 ◄────────── 3. 後端 API
                     （依賴 2 的 AD_Form +          （JwtUtil + AuthFilter）
                      3 的 JwtUtil）
                                                    │
5. React SPA 前端 ◄─────────────────────────────────┘
   （依賴 3 的 API endpoints）
                       │
                       ▼
              6. 即時推送與整合驗證
                 （依賴 1~5 全部完成）
```

### 建議執行順序

| 順序 | WP | 預估工時 | 前置條件 |
|------|-----|---------|---------|
| 1 | 1. 專案骨架 | 2h | 無 |
| 2 | 2. 資料庫與 AD 記錄 | 3h | WP1 |
| 3 | 3.1 認證基礎設施（JwtUtil + AuthFilter） | 1h | WP1 |
| 4 | 3.2~3.4 Servlets | 4h | WP1 + WP3.1 |
| 5 | 4. ZK Form 整合 | 2h | WP2 + WP3.1 |
| 6 | 5.1~5.3 SPA 骨架 | 2h | 無（可與 WP1~4 並行） |
| 7 | 5.4~5.7 SPA 元件 | 4h | WP3（API ready） |
| 8 | 6.1 即時推送 | 2h | WP4 |
| 9 | 6.2~6.3 整合驗證 | 2h | 全部 |
| | **合計** | **~22h** | |

---

## 避雷清單速查（Phase 1 相關）

| # | 嚴重度 | 陷阱 | 出處 |
|---|--------|------|------|
| 1 | 🔴 致命 | `Jetty-Environment: ee8` 缺失 → 所有 servlet 404 | WBS 1.2.2.2 |
| 2 | 🔴 致命 | 翻譯記錄缺失 → 非 en_US 選單隱藏 | WBS 2.3 |
| 3 | 🔴 致命 | `DB.executeUpdateEx()` 不支援 DDL → 用 Java syncColumns() | WBS 2.1 |
| 4 | 🟡 嚴重 | `Executions.getCurrent()` 只在 Controller constructor 安全 | WBS 4.1.3.1 |
| 5 | 🟡 嚴重 | iframe 路徑被 ZK 加 `/webui/` 前綴 → 用完整 URL | WBS 4.1.3.2 |
| 6 | 🟡 嚴重 | WAB servlet 沒有 Env context → 從 AuthContext 取 | WBS 3.1.2.2 |
| 7 | 🟡 嚴重 | SCR @Reference 在 WAB 不觸發 → 動態 getServiceReference() | WBS 2.4.1.2 |
| 8 | 🟡 嚴重 | AD_TreeNodeMM Parent_ID=0 只能放資料夾 | WBS 2.2.2.2 |
| 9 | 🟢 輕微 | Import-Package 優於 Require-Bundle（避免傳遞依賴） | WBS 1.2.3.1 |
| 10 | 🟢 輕微 | Eclipse p2 URL 用 archive.eclipse.org | WBS 1.1.2.2 |
| 11 | 🟢 輕微 | build.properties 必須包含 `.` | WBS 1.4.3 |
| 12 | 🟢 輕微 | p2 pom `includeAllDependencies=false` | WBS 1.1.4.3 |
| 13 | 🟢 輕微 | PostgreSQL 錯誤訊息含 `\n` → escape 後放 JSON | WBS 3.4.5.3 |
