# tw.mxp.idempiere.kanban — 專案狀態

> 最後更新：2026-04-26
> GitHub: https://github.com/nczz/tw.mxp.idempiere.kanban

## 已完成

### Phase 1（全部完成 ✅）
- 專案骨架：Maven/Tycho 4.0.8 + WAB + p2
- 認證：JwtUtil（HS512, KANBAN_TOKEN_SECRET）+ AuthFilter + AuthContext
- InitServlet：statuses + requestTypes + priorities + salesReps + bpartners + projects
- CardsServlet：GET list（scope/closed/requestType/search）+ POST move + GET detail + PUT update + POST create
- LookupServlet：通用 ERP 實體搜尋（9 張表）
- KanbanFormController + KanbanForm：@Form + JWT + postMessage bridge
- Server Push：EventAdmin + Executions.schedule
- MRequestKanban：@Model EndTime 保護
- KanbanFormFactory：AnnotationBasedFormFactory（v11-v14 相容）
- 2Pack migration + 自動建表/選單/翻譯
- React SPA：@dnd-kit/core + React Query + Tailwind
- 相容性：v11 ✅ v12 ✅ v13 ✅ v14 ✅

### Phase 2（進行中）
- ✅ CardDetail modal：查看/編輯所有欄位 + ERP zoom links（9 個）+ 移動歷程
- ✅ NewCardDialog：Summary + RequestType + Priority + SalesRep + DateNextAction
- ✅ 搜尋功能：Summary + DocumentNo + BPartner name
- ✅ SearchSelect 元件：可搜尋的 ERP 關聯選擇器（backend lookup）
- ✅ Open/Closed 切換：工具列 toggle
- ⬜ 欄內排序（X_KanbanSeqNo）
- ⬜ 卡片停滯指標
- ⬜ i18n（AD_Message）

## 架構決策

| 決策 | 選擇 | 原因 |
|------|------|------|
| Form 註冊 | AnnotationBasedFormFactory | IMappedFormFactory.scan 在 v12 不存在 |
| 拖曳庫 | @dnd-kit/core 6.x | @dnd-kit/react v0.4 有 DOM 衝突 |
| JWT Secret | KANBAN_TOKEN_SECRET | 獨立於 REST API |
| Result 更新 | Direct SQL | MRequest.setResult 寫到 R_RequestUpdate |
| ERP 搜尋 | LookupServlet /lookup | 通用 endpoint，9 張表 |
| API 路徑 | 無 /api/ 前綴 | WAB context path 就是 namespace |
| Bundle-Version | 14.0.0 | 匹配 iDempiere 版本 |

## 關鍵檔案

```
tw.mxp.idempiere.kanban/
├── src/tw/mxp/idempiere/kanban/
│   ├── KanbanActivator.java      2Pack + factory scan + 自動建表/選單
│   ├── KanbanFormFactory.java     AnnotationBasedFormFactory (v11-v14)
│   ├── KanbanFormController.java  @Form + JWT + postMessage + Server Push
│   ├── KanbanForm.java            CustomForm + Iframe
│   ├── JwtUtil.java               HS512 JWT (KANBAN_TOKEN_SECRET)
│   ├── AuthFilter.java            JWT validation → request attributes
│   ├── AuthContext.java            4 getters (client/org/user/role)
│   ├── InitServlet.java           GET /init (statuses/types/priorities/salesReps)
│   ├── CardsServlet.java          GET/POST/PUT /cards/* (list/detail/move/create/update)
│   ├── LookupServlet.java         GET /lookup?table=&search= (9 tables)
│   ├── MRequestKanban.java        @Model EndTime protection
│   └── NoCacheFilter.java         Cache-Control headers
spa/src/
├── App.tsx                        主框架 + toolbar + modals
├── api.ts                         kanbanFetch + token + zoom bridge
├── types.ts                       Card/Status/InitData types
├── hooks/useCards.ts              useInit/useCards/useMoveCard/useUpdateCard/useCreateCard/useCardDetail
└── components/
    ├── KanbanBoard.tsx            DndContext + DragOverlay
    ├── KanbanColumn.tsx           useDroppable + DraggableCard
    ├── KanbanCard.tsx             卡片 UI
    ├── CardDetail.tsx             查看/編輯 modal + ERP SearchSelect
    ├── NewCardDialog.tsx          新建 Request
    ├── ScopeFilter.tsx            scope + requestType filter
    └── SearchSelect.tsx           可搜尋的 ERP 關聯選擇器
```

## Build & Deploy

```bash
# 統一 build（SPA → Maven）
bash build.sh

# 部署到 iDempiere
docker cp tw.mxp.idempiere.kanban/target/tw.mxp.idempiere.kanban-*.jar <container>:/opt/idempiere/plugins/
# 加入 bundles.info + 重啟
```

## 已知限制

- i18n：SPA 文字全部英文（AD_Message 機制未實作）
- 欄內排序：X_KanbanSeqNo 欄位已預留但未實作
- Server Push：ZK CE polling 延遲 1-15 秒
- Oracle：migration SQL 有 oracle 版本但未測試
