# 看板欄位對照表 — Kanban Field Mapping

看板卡片的每個欄位對應 iDempiere 原生資料表與欄位。

## 主表：R_Request（需求工單）

| 看板欄位 | Kanban Field | DB 欄位 | 資料型態 | 說明 |
|---------|-------------|---------|---------|------|
| 單據編號 | Document No | `R_Request.DocumentNo` | VARCHAR(30) | 自動編號，前綴 REQ |
| 摘要 | Summary | `R_Request.Summary` | VARCHAR(2000) | 卡片標題 |
| 備註/結果 | Notes / Result | `R_Request.Result` | TEXT | 卡片描述內容 |
| 狀態 | Status | `R_Request.R_Status_ID` → `R_Status.Name` | NUMERIC(10) FK | 看板欄位（Backlog/To Do/...） |
| 需求類型 | Request Type | `R_Request.R_RequestType_ID` → `R_RequestType.Name` | NUMERIC(10) FK | 看板來源分類 |
| 優先級 | Priority | `R_Request.Priority` | CHAR(1) | 1=緊急 3=高 5=中 7=低 9=次要，對應 AD_Ref_List(AD_Reference_ID=154) |
| 到期類型 | Due Type | `R_Request.DueType` | CHAR(1) | 3=逾期(紅) 5=即將到期(黃) 7=正常(綠) |
| 待決/阻塞 | Blocked | `R_Request.IsEscalated` | CHAR(1) Y/N | 卡片阻塞標記 🚫 |
| 負責人 | Sales Rep | `R_Request.SalesRep_ID` → `AD_User.Name` | NUMERIC(10) FK | 卡片負責人 |
| 請求者 | Requester | `R_Request.AD_User_ID` → `AD_User.Name` | NUMERIC(10) FK | 提出需求的人 |
| 建立者 | Created By | `R_Request.CreatedBy` → `AD_User.Name` | NUMERIC(10) FK | 系統自動記錄 |
| 建立時間 | Created | `R_Request.Created` | TIMESTAMP | 系統自動記錄 |
| 下次動作日 | Date Next Action | `R_Request.DateNextAction` | TIMESTAMP | 到期日/下次跟進日 |
| 開始日期 | Start Date | `R_Request.StartDate` | TIMESTAMP | 甘特圖起始 |
| 結束日期 | End Time | `R_Request.EndTime` | TIMESTAMP | 甘特圖結束 |
| 結案日期 | Close Date | `R_Request.CloseDate` | TIMESTAMP | 系統自動（移入 Closed 狀態時） |
| 卡片排序 | Card Order | `R_Request.X_KanbanSeqNo` | NUMERIC(10) | 自訂欄位，欄內拖曳排序 |

## ERP 關聯欄位（R_Request 上的 FK）

| 看板欄位 | Kanban Field | DB 欄位 | 關聯表 | 顯示欄位 | 搜尋端點 |
|---------|-------------|---------|--------|---------|---------|
| 業務夥伴 | Business Partner | `R_Request.C_BPartner_ID` | `C_BPartner` | Name | `/lookup?table=C_BPartner` |
| 產品 | Product | `R_Request.M_Product_ID` | `M_Product` | Name | `/lookup?table=M_Product` |
| 訂單 | Order | `R_Request.C_Order_ID` | `C_Order` | DocumentNo | `/lookup?table=C_Order` |
| 發票 | Invoice | `R_Request.C_Invoice_ID` | `C_Invoice` | DocumentNo | `/lookup?table=C_Invoice` |
| 付款 | Payment | `R_Request.C_Payment_ID` | `C_Payment` | DocumentNo | `/lookup?table=C_Payment` |
| 專案 | Project | `R_Request.C_Project_ID` | `C_Project` | Name | `/lookup?table=C_Project` |
| 行銷活動 | Campaign | `R_Request.C_Campaign_ID` | `C_Campaign` | Name | `/lookup?table=C_Campaign` |
| 資產 | Asset | `R_Request.A_Asset_ID` | `A_Asset` | Name | `/lookup?table=A_Asset` |
| 活動 | Activity | `R_Request.C_Activity_ID` | `C_Activity` | Name | `/lookup?table=C_Activity` |

## 狀態系統

| 看板概念 | Kanban Concept | DB 表 | 關鍵欄位 | 說明 |
|---------|---------------|-------|---------|------|
| 狀態類別 | Status Category | `R_StatusCategory` | Name | 一組狀態的容器，多個看板可共用 |
| 狀態 | Status | `R_Status` | Name, SeqNo, IsOpen, IsClosed, IsFinalClose | 看板的每一欄 |
| 需求類型 | Request Type | `R_RequestType` | Name, R_StatusCategory_ID | 看板來源，指向一個狀態類別 |

## 看板自建表格

| 表格 | Table | 用途 | 關鍵欄位 |
|------|-------|------|---------|
| 移動歷程 | `RK_Card_Move_Log` | 卡片狀態變更記錄 | R_Request_ID, R_Status_ID_From, R_Status_ID_To, CreatedBy, Created |
| 卡片成員 | `RK_Card_Member` | 卡片參與者（預留） | R_Request_ID, AD_User_ID |
| 類型設定 | `RK_Request_Type_Config` | 需求類型擴充設定（預留） | R_RequestType_ID |

## 留言與附件（iDempiere 原生）

| 看板功能 | Kanban Feature | DB 表 | 說明 |
|---------|---------------|-------|------|
| 留言 | Comments | `R_RequestUpdate` | Result 欄位存留言內容，CreatedBy + Created 記錄誰/何時 |
| 附件 | Attachments | `AD_Attachment` | AD_Table_ID=417(R_Request), Record_ID=卡片ID |
| 欄位變更 | Field Changes | `AD_ChangeLog` | AD_Table_ID=417, Record_ID=卡片ID（需啟用 IsChangeLog） |

## 設定儲存

| 設定 | Setting | 儲存位置 | Key 格式 | 範圍 |
|------|---------|---------|---------|------|
| 預設看板來源 | Active Board | `AD_Preference` | `KANBAN_ACTIVE_REQUEST_TYPE` | 個人（per-user） |
| WIP 限制 | WIP Limits | `AD_SysConfig` | `KANBAN_WIP_{R_Status_ID}` | 組織（per-client） |
| 優先級顏色 | Priority Colors | `AD_SysConfig` | `KANBAN_COLOR_P{value}` | 組織（per-client） |
| JWT 密鑰 | JWT Secret | `AD_SysConfig` | `KANBAN_TOKEN_SECRET` | 組織（per-client） |
