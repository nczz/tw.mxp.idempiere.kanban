-- tw.mxp.idempiere.kanban — zh_TW 翻譯匯入
-- 使用方式：啟用 zh_TW 語系後，執行此 SQL 一次即可
-- psql -U adempiere -d idempiere -f i18n/zh_TW.sql

-- AD_Message 翻譯
UPDATE AD_Message_Trl SET MsgText='個人', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanPrivate');
UPDATE AD_Message_Trl SET MsgText='部屬', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanSubordinates');
UPDATE AD_Message_Trl SET MsgText='全部', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanAll');
UPDATE AD_Message_Trl SET MsgText='所有類型', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanAllTypes');
UPDATE AD_Message_Trl SET MsgText='搜尋...', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanSearch');
UPDATE AD_Message_Trl SET MsgText='+ 新增', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNew');
UPDATE AD_Message_Trl SET MsgText='📋 進行中', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanOpen');
UPDATE AD_Message_Trl SET MsgText='📦 已結案', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanClosed');
UPDATE AD_Message_Trl SET MsgText='沒有卡片', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNoCards');
UPDATE AD_Message_Trl SET MsgText='尚未設定狀態。', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNoStatuses');
UPDATE AD_Message_Trl SET MsgText='編輯', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanEdit');
UPDATE AD_Message_Trl SET MsgText='儲存', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanSave');
UPDATE AD_Message_Trl SET MsgText='儲存中...', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanSaving');
UPDATE AD_Message_Trl SET MsgText='取消', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCancel');
UPDATE AD_Message_Trl SET MsgText='備註 / 結果', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNotesResult');
UPDATE AD_Message_Trl SET MsgText='無備註', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNoNotes');
UPDATE AD_Message_Trl SET MsgText='ERP 關聯', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanERPLinks');
UPDATE AD_Message_Trl SET MsgText='無關聯記錄', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNoLinks');
UPDATE AD_Message_Trl SET MsgText='移動歷程', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanMoveHistory');
UPDATE AD_Message_Trl SET MsgText='無移動記錄', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNoMoves');
UPDATE AD_Message_Trl SET MsgText='待決', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanEscalated');
UPDATE AD_Message_Trl SET MsgText='新增需求', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNewRequest');
UPDATE AD_Message_Trl SET MsgText='摘要', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanSummary');
UPDATE AD_Message_Trl SET MsgText='建立', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCreate');
UPDATE AD_Message_Trl SET MsgText='建立中...', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCreating');
UPDATE AD_Message_Trl SET MsgText='狀態', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanStatus');
UPDATE AD_Message_Trl SET MsgText='需求類型', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanRequestType');
UPDATE AD_Message_Trl SET MsgText='優先級', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanPriority');
UPDATE AD_Message_Trl SET MsgText='負責人', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanSalesRep');
UPDATE AD_Message_Trl SET MsgText='請求者', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanRequester');
UPDATE AD_Message_Trl SET MsgText='建立者', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCreatedBy');
UPDATE AD_Message_Trl SET MsgText='建立時間', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCreated');
UPDATE AD_Message_Trl SET MsgText='下次動作', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNextAction');
UPDATE AD_Message_Trl SET MsgText='開始日期', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanStartDate');
UPDATE AD_Message_Trl SET MsgText='結束日期', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanEndTime');
UPDATE AD_Message_Trl SET MsgText='結案日期', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCloseDate');
UPDATE AD_Message_Trl SET MsgText='下次動作日', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanDateNextAction');
UPDATE AD_Message_Trl SET MsgText='業務夥伴', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanBusinessPartner');
UPDATE AD_Message_Trl SET MsgText='產品', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanProduct');
UPDATE AD_Message_Trl SET MsgText='訂單', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanOrder');
UPDATE AD_Message_Trl SET MsgText='發票', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanInvoice');
UPDATE AD_Message_Trl SET MsgText='付款', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanPayment');
UPDATE AD_Message_Trl SET MsgText='專案', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanProject');
UPDATE AD_Message_Trl SET MsgText='行銷活動', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCampaign');
UPDATE AD_Message_Trl SET MsgText='資產', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanAsset');
UPDATE AD_Message_Trl SET MsgText='活動', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanActivity');
UPDATE AD_Message_Trl SET MsgText='載入中...', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanLoading');
UPDATE AD_Message_Trl SET MsgText='載入卡片中...', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanLoadingCards');
UPDATE AD_Message_Trl SET MsgText='載入失敗', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanFailedToLoad');
UPDATE AD_Message_Trl SET MsgText='無認證令牌', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNoToken');
UPDATE AD_Message_Trl SET MsgText='請從 iDempiere 選單開啟此表單。', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNoTokenHint');
UPDATE AD_Message_Trl SET MsgText='移動失敗', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanMoveFailed');
UPDATE AD_Message_Trl SET MsgText='留言', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanComments');
UPDATE AD_Message_Trl SET MsgText='尚無留言', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNoComments');
UPDATE AD_Message_Trl SET MsgText='新增留言...', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanAddComment');
UPDATE AD_Message_Trl SET MsgText='發佈', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanPost');
UPDATE AD_Message_Trl SET MsgText='發佈中...', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanPosting');
UPDATE AD_Message_Trl SET MsgText='附件', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanAttachments');
UPDATE AD_Message_Trl SET MsgText='無附件', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNoAttachments');
UPDATE AD_Message_Trl SET MsgText='上傳', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanUpload');
UPDATE AD_Message_Trl SET MsgText='上傳中...', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanUploading');
UPDATE AD_Message_Trl SET MsgText='確定刪除此檔案？', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanDeleteConfirm');
UPDATE AD_Message_Trl SET MsgText='看板', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanViewBoard');
UPDATE AD_Message_Trl SET MsgText='甘特圖', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanViewGantt');
UPDATE AD_Message_Trl SET MsgText='度量', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanViewMetrics');
UPDATE AD_Message_Trl SET MsgText='設定', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanSettings');
UPDATE AD_Message_Trl SET MsgText='WIP 限制（每欄）', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanWipLimits');
UPDATE AD_Message_Trl SET MsgText='張卡片', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanWipCards');
UPDATE AD_Message_Trl SET MsgText='此欄已達 WIP 上限', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanWipExceeded');
UPDATE AD_Message_Trl SET MsgText='優先級顏色', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanPriorityColors');
UPDATE AD_Message_Trl SET MsgText='週期時間（每狀態平均天數）', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCycleTime');
UPDATE AD_Message_Trl SET MsgText='吞吐量（每週完成數）', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanThroughput');
UPDATE AD_Message_Trl SET MsgText='無資料', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNoData');
UPDATE AD_Message_Trl SET MsgText='待決', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanBlock');
UPDATE AD_Message_Trl SET MsgText='解除待決', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanUnblock');
UPDATE AD_Message_Trl SET MsgText='看板來源', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanBoardSource');
UPDATE AD_Message_Trl SET MsgText='狀態管理', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanStatusManagement');
UPDATE AD_Message_Trl SET MsgText='新增狀態', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanAddStatus');
UPDATE AD_Message_Trl SET MsgText='狀態名稱', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanStatusName');
UPDATE AD_Message_Trl SET MsgText='類型', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanStatusType');
UPDATE AD_Message_Trl SET MsgText='進行中', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanStatusOpen');
UPDATE AD_Message_Trl SET MsgText='已結案', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanStatusClosed');
UPDATE AD_Message_Trl SET MsgText='最終結案', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanStatusFinalClose');
UPDATE AD_Message_Trl SET MsgText='刪除', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanDeleteStatus');
UPDATE AD_Message_Trl SET MsgText='此操作將永久結案卡片，無法重新開啟。確定繼續？', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanFinalCloseWarning');
UPDATE AD_Message_Trl SET MsgText='卡片已移至結案狀態', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCardClosed');
UPDATE AD_Message_Trl SET MsgText='描述需求...', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanDescribeRequest');
UPDATE AD_Message_Trl SET MsgText='補充說明...', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanAdditionalDetails');
UPDATE AD_Message_Trl SET MsgText='— 選擇 —', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanSelectNone');
UPDATE AD_Message_Trl SET MsgText='— 無 —', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanNone');

-- AD_Menu 翻譯
UPDATE AD_Menu_Trl SET Name='需求看板', Description='iDempiere 需求工單看板管理', IsTranslated='Y'
WHERE AD_Language='zh_TW' AND AD_Menu_ID=(SELECT AD_Menu_ID FROM AD_Menu WHERE AD_Menu_UU='tw-mxp-idempiere-kanban-menu-001');

-- AD_Form 翻譯
UPDATE AD_Form_Trl SET Name='需求看板', Description='iDempiere 需求工單看板管理', IsTranslated='Y'
WHERE AD_Language='zh_TW' AND AD_Form_ID=(SELECT AD_Form_ID FROM AD_Form WHERE AD_Form_UU='tw-mxp-idempiere-kanban-form-001');

-- Swimlanes (Phase 4)
UPDATE AD_Message_Trl SET MsgText='分組', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanGroupBy');
UPDATE AD_Message_Trl SET MsgText='無', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanGroupNone');
UPDATE AD_Message_Trl SET MsgText='專案', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanGroupProject');
UPDATE AD_Message_Trl SET MsgText='負責人', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanGroupSalesRep');
UPDATE AD_Message_Trl SET MsgText='業務夥伴', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanGroupBPartner');
UPDATE AD_Message_Trl SET MsgText='優先級', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanGroupPriority');
UPDATE AD_Message_Trl SET MsgText='未分組', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanUngrouped');

-- Misc (Phase 4)
UPDATE AD_Message_Trl SET MsgText='找不到卡片', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCardNotFound');
UPDATE AD_Message_Trl SET MsgText='在 iDempiere 中開啟', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanOpenInERP');
UPDATE AD_Message_Trl SET MsgText='WIP 限制（0=無限）', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanWipTooltip');
UPDATE AD_Message_Trl SET MsgText='上次移動', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanLastMoved');
UPDATE AD_Message_Trl SET MsgText='預設', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanDefault');
UPDATE AD_Message_Trl SET MsgText='狀態與以下看板共用', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanSharedStatuses');
UPDATE AD_Message_Trl SET MsgText='建立獨立狀態', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanMakeIndependent');
UPDATE AD_Message_Trl SET MsgText='活動歷程', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanActivityLog');
UPDATE AD_Message_Trl SET MsgText='無法刪除：有卡片使用此狀態', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanCannotDelete');
UPDATE AD_Message_Trl SET MsgText='天', IsTranslated='Y' WHERE AD_Language='zh_TW' AND AD_Message_ID=(SELECT AD_Message_ID FROM AD_Message WHERE Value='KanbanDaysAgo');
