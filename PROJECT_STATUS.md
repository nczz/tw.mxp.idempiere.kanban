# tw.mxp.idempiere.kanban — 專案狀態

> 最後更新：2026-04-26 22:30
> GitHub: https://github.com/nczz/tw.mxp.idempiere.kanban
> iDempiere 相容：v11 ✅ v12 ✅ v13 ✅ v14 ✅

## 已完成功能

Board：拖曳移動 + Scope 過濾 + Open/Closed + WIP 限制 + 停滯指標 + Blocked 標記
Swimlanes：按專案/負責人/業務夥伴/優先級分組（獨立 DndContext 隔離拖曳）
Gantt：StartDate/EndTime 時程條 + 今日線（跟隨 requestType 過濾）
Metrics：Cycle Time + Throughput（跟隨 requestType 過濾）
Activity Timeline：moves(RK_Card_Move_Log) + comments(R_RequestUpdate) + field changes(AD_ChangeLog) 合併時間軸
CardDetail：查看/編輯所有欄位 + 10 個 SearchSelect(含 AD_User) + 留言 + 附件 + Activity
NewCard：選狀態欄 + 需求類型預設當前看板 + 負責人 SearchSelect
搜尋：Summary/DocumentNo/BPartner
設定：Board Source(per-user, radio list + 改名 + 新增 + 🔗zoom) + Status Management(▲▼排序 + 共用警告 + 獨立化) + WIP + Priority Colors
預設看板：Backlog → To Do → In Progress → Review → Done → Archived
i18n：100+ AD_Message + i18n/zh_TW.sql + Priority labels from AD_Ref_List_Trl
Migration：v1.0.0→v1.5.0（tables, form, menu, board, messages, ChangeLog）

## 關鍵決策
- Form 註冊：KanbanFormFactory (AnnotationBasedFormFactory) — v11-v14 相容
- 拖曳：@dnd-kit/core 6.x（不是 @dnd-kit/react）
- JWT：獨立 KANBAN_TOKEN_SECRET
- Result 更新：direct SQL（MRequest.setResult 寫到 R_RequestUpdate）
- Migration name：getName()+"-migration"（避免跟 2Pack 版本號衝突）
- DB 操作：只在 afterPackIn 中（start 時 DB 未 ready）
- R_Status.Value：NOT NULL，INSERT 時必填
- i18n：安裝時自動建立已啟用語系翻譯，後啟用語系用 i18n/*.sql 匯入
- Priority labels：JOIN AD_Ref_List_Trl + setPriorityLabels()
- Board Source：AD_Preference（per-user），不是 AD_SysConfig
- WIP/Colors：AD_SysConfig（org-level）
- CreatedBy：0（System），不是 100（SuperUser）
- Priority Reference：AD_Reference_ID=154（標準值）
- Swimlanes：每行獨立 DndContext（方案 A，業界標準）
- Activity Timeline：合併 RK_Card_Move_Log + R_RequestUpdate + AD_ChangeLog
- Change Log：安裝時自動啟用 R_Request IsChangeLog

## 未來規劃

| 優先級 | 功能 | 說明 |
|--------|------|------|
| 🟡 | @mention 通知 | 留言中 @某人 → AD_Note 或 email 通知 |
| 🟡 | 批次操作 | 多選卡片 → 批次移動/指派/變更優先級 |
| 🟡 | 卡片模板 | 常用 Request 預設值，一鍵建立 |
| 🟡 | Dashboard 首頁 | 我的卡片摘要 + 到期提醒 + 今日待辦 |
| 🟢 | 匯出報表 | CSV/PDF 匯出看板資料 |
| 🟢 | 鍵盤快捷鍵 | N=新增、/=搜尋、1-3=切換視圖 |
| 🟢 | 社群發佈準備 | LICENSE、CONTRIBUTING.md、wiki、demo 影片 |
| 🟢 | Oracle 相容性測試 | migration SQL 有 oracle/ 版本但未測試 |

## Build
bash build.sh（SPA + Maven 一次搞定）
