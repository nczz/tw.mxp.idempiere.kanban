# tw.mxp.idempiere.kanban — 專案狀態

> 最後更新：2026-04-26 16:11
> GitHub: https://github.com/nczz/tw.mxp.idempiere.kanban
> iDempiere 相容：v11 ✅ v12 ✅ v13 ✅ v14 ✅

## 當前進度

### 已完成功能
- 看板 Board 視圖（拖曳移動 + Scope/RequestType 過濾 + Open/Closed 切換）
- 甘特圖 Gantt 視圖（StartDate/EndTime 時程條 + 今日線）
- 度量 Metrics 視圖（Cycle Time + Throughput 圖表）
- CardDetail modal（查看/編輯所有欄位 + 9 個 ERP SearchSelect + 留言 + 附件 + 移動歷程）
- 新建 Request dialog（完整欄位 + ERP Links）
- 搜尋（Summary/DocumentNo/BPartner）
- WIP 限制（per column + 警告/阻擋）
- 優先級顏色設定（color picker + 動態顏色）
- 卡片停滯指標（3天黃/7天紅）
- Blocked 標記（🚫 + Block/Unblock）
- FinalClose 確認對話框
- i18n（70+ AD_Message + zh_TW 翻譯）
- 預設看板（Backlog → To Do → In Progress → Review → Done → Archived）
- 設定對話視窗（Board Source + Status Management + WIP + Colors）
- 2Pack migration + 版本追蹤（v1.0.0 + v1.1.0）
- 附件上傳/下載/刪除（AD_Attachment）
- 留言（R_RequestUpdate + IME 中文輸入相容）

### 待完成（Phase 3 剩餘 + Phase 4）
- P3-4：Swimlanes（按專案/負責人分泳道）
- 設定對話視窗的狀態管理 UI 測試驗證
- i18n 完整性檢查（可能還有遺漏的寫死文字）
- Oracle 相容性測試
- 社群發佈準備（LICENSE、CONTRIBUTING、wiki 文件）

## 架構

```
KanbanFormController (@Form) → JWT (HS512, KANBAN_TOKEN_SECRET) → iframe
  → React SPA (@dnd-kit/core + React Query + Tailwind)
    → /init, /cards/*, /gantt, /metrics, /lookup, /attachments/*, /config
  → postMessage bridge (zoom, token refresh, server push)
```

## 關鍵決策
- Form 註冊：KanbanFormFactory (AnnotationBasedFormFactory) — v11-v14 相容
- 拖曳：@dnd-kit/core 6.x（不是 @dnd-kit/react，有 DOM 衝突）
- JWT：獨立 KANBAN_TOKEN_SECRET（不共用 REST API）
- Result 更新：direct SQL（MRequest.setResult 寫到 R_RequestUpdate）
- Migration：isMigrationApplied + recordMigration（對齊 appointment 模式）
- DB 操作：只在 afterPackIn 中（start 時 DB 未 ready）
- R_Status.Value：NOT NULL，INSERT 時必填

## Build & Deploy
```bash
bash build.sh                    # SPA + Maven 一次搞定
# 部署到 Docker
docker cp tw.mxp.idempiere.kanban/target/*.jar <container>:/opt/idempiere/plugins/
# 更新 bundles.info + 重啟
```

## 檔案結構
```
Java (16 classes):
  KanbanActivator, KanbanFormFactory, KanbanFormController, KanbanForm,
  JwtUtil, AuthFilter, AuthContext, NoCacheFilter,
  InitServlet, CardsServlet, LookupServlet, GanttServlet,
  MetricsServlet, ConfigServlet, AttachmentServlet, MRequestKanban

SPA (12 components):
  App, KanbanBoard, KanbanColumn, KanbanCard, CardDetail,
  NewCardDialog, SettingsDialog, ScopeFilter, SearchSelect,
  GanttView, MetricsView, i18n
```
