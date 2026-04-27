# idempiere-kanban — 專案狀態

> 最後更新：2026-04-27 20:06
> GitHub: https://github.com/nczz/idempiere-kanban
> iDempiere 相容：v11 – v14

## 已完成功能

Board：拖曳移動 + 欄內排序 + Swimlanes + WIP 限制 + Blocked + 停滯指標 + FinalClose
Views：Board / Gantt / Metrics（都跟隨 requestType + org 過濾）
Card：建立（選狀態/Org）+ 編輯 + 10 個 SearchSelect + 留言(@mention) + 附件 + Activity Timeline
Settings：Board Source(per-user) + Status Management(排序/共用偵測/獨立化) + WIP + Colors + 🔗zoom
Notifications：Watch/Unwatch + @mention + AD_Note + HTML Email + per-user i18n
Reminders：每日排程（明天到期/今天到期/逾期/3天通知主管/7天自動Blocked/明天開始）
Enterprise：多租戶 + 多組織(Role OrgAccess) + Org filter + Scope(My Cards/My Team/All) + 120+ i18n keys
Migration：v1.0.0 → v2.0.2（tables, form, menu, board, messages, ChangeLog, Process, Scheduler）

## 關鍵決策
- Form 註冊：KanbanFormFactory (AnnotationBasedFormFactory)
- 拖曳：@dnd-kit/core 6.x
- JWT：獨立 KANBAN_TOKEN_SECRET
- Migration name：getName()+"-migration"（避免 2Pack 衝突）
- Board Source：AD_Preference（per-user）
- WIP/Colors：AD_SysConfig（org-level）
- Org filter：AD_Role_OrgAccess（不是 JWT orgId）
- CreatedBy：0（System）
- Priority Reference：AD_Reference_ID=154
- Swimlanes：每行獨立 DndContext
- Activity Timeline：RK_Card_Move_Log + R_RequestUpdate + AD_ChangeLog
- Notifications：AD_Note + MClient.sendEMail(isHtml=true)
- Scheduler：AD_Process + AD_Schedule + AD_Scheduler 三層
- Env.setContext：已知 thread safety 限制（同 REST API）

## 檔案結構
Java (18 classes): KanbanActivator, KanbanFormFactory, KanbanFormController, KanbanForm, JwtUtil, AuthFilter, AuthContext, NoCacheFilter, InitServlet, CardsServlet, LookupServlet, GanttServlet, MetricsServlet, ConfigServlet, AttachmentServlet, MRequestKanban, NotificationHelper, KanbanReminderProcess
SPA (14 files): App, KanbanBoard, KanbanColumn, KanbanCard, CardDetail, NewCardDialog, SettingsDialog, ScopeFilter, SearchSelect, MentionInput, GanttView, MetricsView, i18n, priority

## 未來規劃
| 優先級 | 功能 |
|--------|------|
| 🟡 | 通知 badge（🔔 工具列未讀數） |
| 🟡 | 批次操作（多選卡片） |
| 🟡 | 卡片模板 |
| 🟡 | Dashboard 首頁 |
| 🟢 | 匯出 CSV/PDF |
| 🟢 | 鍵盤快捷鍵 |
| 🟢 | Oracle 相容性 |
| 🟢 | 社群發佈（LICENSE, CONTRIBUTING） |

## Build
bash build.sh
