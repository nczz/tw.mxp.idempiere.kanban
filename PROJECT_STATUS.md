# tw.mxp.idempiere.kanban — 專案狀態

> 最後更新：2026-04-26 17:22
> GitHub: https://github.com/nczz/tw.mxp.idempiere.kanban
> iDempiere 相容：v11 ✅ v12 ✅ v13 ✅ v14 ✅
> 全新安裝驗證：✅ 通過（v12 Docker）

## 下一步：Phase 4

P4-1：Swimlanes（Board 視圖加分組下拉：無/按專案/按負責人/按業務夥伴）
- 不是新視圖，是 Board 的選項
- 前端分組即可（卡片已有所有欄位）
- KanbanBoard 從一維 flex 改為二維 grid

## 已完成功能（Phase 1-3）

Board 視圖：拖曳移動 + Scope 過濾 + Open/Closed 切換 + WIP 限制 + 停滯指標 + Blocked 標記
Gantt 視圖：StartDate/EndTime 時程條 + 今日線
Metrics 視圖：Cycle Time + Throughput 圖表
CardDetail：查看/編輯所有欄位 + 9 個 ERP SearchSelect + 留言(R_RequestUpdate) + 附件(AD_Attachment) + 移動歷程
新建 Request：完整欄位 + ERP Links
搜尋：Summary/DocumentNo/BPartner
設定：Board Source + Status Management(含WIP) + Priority Colors（3 tabs）
預設看板：Backlog → To Do → In Progress → Review → Done → Archived
i18n：90 AD_Message + i18n/zh_TW.sql 匯入檔
Migration：v1.0.0(tables+form+menu) → v1.1.0(default board+messages) → v1.2.0(prefix REQ+label fix)

## 關鍵決策
- Form 註冊：KanbanFormFactory (AnnotationBasedFormFactory) — v11-v14 相容
- 拖曳：@dnd-kit/core 6.x（不是 @dnd-kit/react，有 DOM 衝突）
- JWT：獨立 KANBAN_TOKEN_SECRET
- Result 更新：direct SQL（MRequest.setResult 寫到 R_RequestUpdate）
- Migration name：getName()+"-migration"（避免跟 2Pack 版本號衝突）
- DB 操作：只在 afterPackIn 中（start 時 DB 未 ready）
- R_Status.Value：NOT NULL，INSERT 時必填
- i18n：安裝時自動建立已啟用語系翻譯，後啟用語系用 i18n/*.sql 匯入
- Priority labels：JOIN AD_Ref_List_Trl 取翻譯名稱

## 檔案結構
Java (16 classes): KanbanActivator, KanbanFormFactory, KanbanFormController, KanbanForm, JwtUtil, AuthFilter, AuthContext, NoCacheFilter, InitServlet, CardsServlet, LookupServlet, GanttServlet, MetricsServlet, ConfigServlet, AttachmentServlet, MRequestKanban
SPA (13 files): App, KanbanBoard, KanbanColumn, KanbanCard, CardDetail, NewCardDialog, SettingsDialog, ScopeFilter, SearchSelect, GanttView, MetricsView, i18n, priority

## Build
bash build.sh（SPA + Maven 一次搞定）
