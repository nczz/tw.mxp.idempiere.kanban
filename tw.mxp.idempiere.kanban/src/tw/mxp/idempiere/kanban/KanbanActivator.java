package tw.mxp.idempiere.kanban;

import java.util.logging.Level;

import org.adempiere.base.Core;
import org.adempiere.plugin.utils.Incremental2PackActivator;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.osgi.framework.BundleContext;

/**
 * Plugin activator following the same pattern as the official REST API plugin:
 * - Factory scan in start() BEFORE super.start()
 * - 2Pack ZIPs in META-INF/ for version tracking
 * - afterPackIn() for AD record creation
 */
public class KanbanActivator extends Incremental2PackActivator {

	private static final CLogger log = CLogger.getCLogger(KanbanActivator.class);
	private static final String FORM_UU = "tw-mxp-idempiere-kanban-form-001";
	private static final String MENU_UU = "tw-mxp-idempiere-kanban-menu-001";

	@Override
	public void start(BundleContext context) throws Exception {
		// Model factory scan — use try/catch for v11/v12 compatibility
		try {
			Core.getMappedModelFactory().scan(context, "tw.mxp.idempiere.kanban");
		} catch (Exception e) {
			log.log(Level.FINE, "IMappedModelFactory.scan not available (pre-v14)", e);
		}
		// Form factory is handled by KanbanFormFactory (AnnotationBasedFormFactory)
		// — no scan() needed, works on v11-v14

		super.start(context); // processes 2Pack ZIPs + calls afterPackIn()
	}

	@Override
	protected void afterPackIn() {
		super.afterPackIn();
		ensureTables();
		ensureForm();
		ensureMenu();
		ensureMessages();
	}

	private void ensureMessages() {
		String[][] msgs = {
			// key, en_US, zh_TW
			{"KanbanPrivate", "Private", "個人"},
			{"KanbanSubordinates", "Subordinates", "部屬"},
			{"KanbanAll", "All", "全部"},
			{"KanbanAllTypes", "All Types", "所有類型"},
			{"KanbanSearch", "Search...", "搜尋..."},
			{"KanbanNew", "+ New", "+ 新增"},
			{"KanbanOpen", "📋 Open", "📋 進行中"},
			{"KanbanClosed", "📦 Closed", "📦 已結案"},
			{"KanbanNoCards", "No cards", "沒有卡片"},
			{"KanbanNoStatuses", "No statuses configured.", "尚未設定狀態。"},
			{"KanbanEdit", "Edit", "編輯"},
			{"KanbanSave", "Save", "儲存"},
			{"KanbanSaving", "Saving...", "儲存中..."},
			{"KanbanCancel", "Cancel", "取消"},
			{"KanbanNotesResult", "Notes / Result", "備註 / 結果"},
			{"KanbanNoNotes", "No notes", "無備註"},
			{"KanbanERPLinks", "ERP Links", "ERP 關聯"},
			{"KanbanNoLinks", "No linked records", "無關聯記錄"},
			{"KanbanMoveHistory", "Move History", "移動歷程"},
			{"KanbanNoMoves", "No moves recorded", "無移動記錄"},
			{"KanbanEscalated", "Escalated", "已升級"},
			{"KanbanNewRequest", "New Request", "新增需求"},
			{"KanbanSummary", "Summary", "摘要"},
			{"KanbanCreate", "Create", "建立"},
			{"KanbanCreating", "Creating...", "建立中..."},
			{"KanbanStatus", "Status", "狀態"},
			{"KanbanRequestType", "Request Type", "需求類型"},
			{"KanbanPriority", "Priority", "優先級"},
			{"KanbanSalesRep", "Sales Rep", "負責人"},
			{"KanbanRequester", "Requester", "請求者"},
			{"KanbanCreatedBy", "Created By", "建立者"},
			{"KanbanCreated", "Created", "建立時間"},
			{"KanbanNextAction", "Next Action", "下次動作"},
			{"KanbanStartDate", "Start Date", "開始日期"},
			{"KanbanCloseDate", "Close Date", "結案日期"},
			{"KanbanDateNextAction", "Date Next Action", "下次動作日"},
			{"KanbanBusinessPartner", "Business Partner", "業務夥伴"},
			{"KanbanProduct", "Product", "產品"},
			{"KanbanOrder", "Order", "訂單"},
			{"KanbanInvoice", "Invoice", "發票"},
			{"KanbanPayment", "Payment", "付款"},
			{"KanbanProject", "Project", "專案"},
			{"KanbanCampaign", "Campaign", "行銷活動"},
			{"KanbanAsset", "Asset", "資產"},
			{"KanbanActivity", "Activity", "活動"},
			{"KanbanLoading", "Loading...", "載入中..."},
			{"KanbanLoadingCards", "Loading cards...", "載入卡片中..."},
			{"KanbanFailedToLoad", "Failed to load", "載入失敗"},
			{"KanbanNoToken", "No authentication token", "無認證令牌"},
			{"KanbanNoTokenHint", "Please open this form from the iDempiere menu.", "請從 iDempiere 選單開啟此表單。"},
			{"KanbanMoveFailed", "Move failed", "移動失敗"},
			{"KanbanComments", "Comments", "留言"},
			{"KanbanNoComments", "No comments yet", "尚無留言"},
			{"KanbanAddComment", "Add comment...", "新增留言..."},
			{"KanbanPost", "Post", "發佈"},
			{"KanbanPosting", "Posting...", "發佈中..."},
			{"KanbanAttachments", "Attachments", "附件"},
			{"KanbanNoAttachments", "No attachments", "無附件"},
			{"KanbanUpload", "Upload", "上傳"},
			{"KanbanUploading", "Uploading...", "上傳中..."},
			{"KanbanDeleteConfirm", "Delete this file?", "確定刪除此檔案？"},
			{"KanbanViewBoard", "Board", "看板"},
			{"KanbanViewGantt", "Gantt", "甘特圖"},
			{"KanbanSettings", "Settings", "設定"},
			{"KanbanWipLimits", "WIP Limits (per column)", "WIP 限制（每欄）"},
			{"KanbanWipCards", "cards", "張卡片"},
			{"KanbanWipExceeded", "WIP limit reached for this column", "此欄已達 WIP 上限"},
			{"KanbanPriorityColors", "Priority Colors", "優先級顏色"},
		};
		for (String[] m : msgs) {
			if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_Message WHERE Value=?", m[0]) > 0) continue;
			int id = DB.getNextID(0, "AD_Message", null);
			DB.executeUpdateEx("INSERT INTO AD_Message (AD_Message_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,"
				+ "Updated,UpdatedBy,Value,MsgText,MsgType,EntityType,AD_Message_UU) "
				+ "VALUES (?,0,0,'Y',now(),100,now(),100,?,?,'I','U',generate_uuid())",
				new Object[]{id, m[0], m[1]}, null);
			// Translations for all system languages
			DB.executeUpdate("INSERT INTO AD_Message_Trl (AD_Message_ID,AD_Language,AD_Client_ID,AD_Org_ID,IsActive,"
				+ "Created,CreatedBy,Updated,UpdatedBy,MsgText,MsgTip,IsTranslated,AD_Message_Trl_UU) "
				+ "SELECT "+id+",l.AD_Language,0,0,'Y',now(),100,now(),100,'"+m[1]+"',NULL,'N',generate_uuid() "
				+ "FROM AD_Language l WHERE l.IsActive='Y' AND l.IsSystemLanguage='Y' AND l.IsBaseLanguage='N' "
				+ "AND NOT EXISTS (SELECT 1 FROM AD_Message_Trl t WHERE t.AD_Message_ID="+id+" AND t.AD_Language=l.AD_Language)",
				false, null);
			// zh_TW
			DB.executeUpdate("UPDATE AD_Message_Trl SET MsgText='"+m[2].replace("'","''")+"',IsTranslated='Y' "
				+ "WHERE AD_Message_ID="+id+" AND AD_Language='zh_TW'", false, null);
		}
	}

	private void ensureTables() {
		createTableIfNotExists("RK_Card_Move_Log",
			"RK_Card_Move_Log_ID NUMERIC(10) NOT NULL, "
			+ "RK_Card_Move_Log_UU VARCHAR(36) DEFAULT generate_uuid(), "
			+ "AD_Client_ID NUMERIC(10) NOT NULL, AD_Org_ID NUMERIC(10) NOT NULL DEFAULT 0, "
			+ "IsActive CHAR(1) NOT NULL DEFAULT 'Y', "
			+ "Created TIMESTAMP NOT NULL DEFAULT statement_timestamp(), CreatedBy NUMERIC(10) NOT NULL, "
			+ "Updated TIMESTAMP NOT NULL DEFAULT statement_timestamp(), UpdatedBy NUMERIC(10) NOT NULL, "
			+ "R_Request_ID NUMERIC(10) NOT NULL, R_Status_ID_From NUMERIC(10), "
			+ "R_Status_ID_To NUMERIC(10) NOT NULL, Note VARCHAR(2000), "
			+ "CONSTRAINT RK_Card_Move_Log_Key PRIMARY KEY (RK_Card_Move_Log_ID)");

		createTableIfNotExists("RK_Card_Member",
			"RK_Card_Member_ID NUMERIC(10) NOT NULL, "
			+ "RK_Card_Member_UU VARCHAR(36) DEFAULT generate_uuid(), "
			+ "AD_Client_ID NUMERIC(10) NOT NULL, AD_Org_ID NUMERIC(10) NOT NULL DEFAULT 0, "
			+ "IsActive CHAR(1) NOT NULL DEFAULT 'Y', "
			+ "Created TIMESTAMP NOT NULL DEFAULT statement_timestamp(), CreatedBy NUMERIC(10) NOT NULL, "
			+ "Updated TIMESTAMP NOT NULL DEFAULT statement_timestamp(), UpdatedBy NUMERIC(10) NOT NULL, "
			+ "R_Request_ID NUMERIC(10) NOT NULL, AD_User_ID NUMERIC(10) NOT NULL, "
			+ "MemberRole VARCHAR(20) DEFAULT 'Observer', "
			+ "CONSTRAINT RK_Card_Member_Key PRIMARY KEY (RK_Card_Member_ID)");

		createTableIfNotExists("RK_Request_Type_Config",
			"RK_Request_Type_Config_ID NUMERIC(10) NOT NULL, "
			+ "RK_Request_Type_Config_UU VARCHAR(36) DEFAULT generate_uuid(), "
			+ "AD_Client_ID NUMERIC(10) NOT NULL, AD_Org_ID NUMERIC(10) NOT NULL DEFAULT 0, "
			+ "IsActive CHAR(1) NOT NULL DEFAULT 'Y', "
			+ "Created TIMESTAMP NOT NULL DEFAULT statement_timestamp(), CreatedBy NUMERIC(10) NOT NULL, "
			+ "Updated TIMESTAMP NOT NULL DEFAULT statement_timestamp(), UpdatedBy NUMERIC(10) NOT NULL, "
			+ "R_RequestType_ID NUMERIC(10) NOT NULL, Default_AD_Role_ID NUMERIC(10), "
			+ "Default_SalesRep_ID NUMERIC(10), "
			+ "CONSTRAINT RK_Request_Type_Config_Key PRIMARY KEY (RK_Request_Type_Config_ID)");

		// X_KanbanSeqNo column on R_Request (for in-column card ordering)
		if (DB.getSQLValueEx(null,
				"SELECT COUNT(*) FROM information_schema.columns WHERE LOWER(table_name)='r_request' AND LOWER(column_name)='x_kanbanseqno'") == 0) {
			DB.executeUpdate("ALTER TABLE R_Request ADD COLUMN X_KanbanSeqNo NUMERIC(10) DEFAULT 0", false, null);
			log.info("Added X_KanbanSeqNo column to R_Request");
		}
	}

	private void createTableIfNotExists(String tableName, String columns) {
		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name)=?",
				tableName.toLowerCase()) > 0)
			return;
		DB.executeUpdate("CREATE TABLE " + tableName + " (" + columns + ")", false, null);
		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_Sequence WHERE Name=? AND IsTableID='Y'", tableName) == 0) {
			int seqId = DB.getNextID(0, "AD_Sequence", null);
			DB.executeUpdateEx("INSERT INTO AD_Sequence (AD_Sequence_ID,Name,CurrentNext,IsAudited,StartNewYear,"
				+ "Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,"
				+ "IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) "
				+ "VALUES (?,'"+tableName+"',1000000,'N','N','Table "+tableName+"','Y','Y',0,0,now(),100,now(),100,"
				+ "'Y',1000000,1,200000,generate_uuid())", new Object[]{seqId}, null);
		}
		log.info("Created table " + tableName);
	}

	private void ensureForm() {
		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_Form WHERE AD_Form_UU=?", FORM_UU) > 0) return;
		int id = DB.getNextID(0, "AD_Form", null);
		DB.executeUpdateEx("INSERT INTO AD_Form (AD_Form_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,"
			+ "Updated,UpdatedBy,Name,Description,Classname,AccessLevel,IsBetaFunctionality,EntityType,AD_Form_UU) "
			+ "VALUES (?,0,0,'Y',now(),100,now(),100,'Request Kanban','Kanban board for request management',"
			+ "'tw.mxp.idempiere.kanban.KanbanFormController','3','N','U',?)", new Object[]{id, FORM_UU}, null);
		// i18n: all system languages
		DB.executeUpdate("INSERT INTO AD_Form_Trl (AD_Form_ID,AD_Language,AD_Client_ID,AD_Org_ID,IsActive,"
			+ "Created,CreatedBy,Updated,UpdatedBy,Name,Description,Help,IsTranslated,AD_Form_Trl_UU) "
			+ "SELECT "+id+",l.AD_Language,0,0,'Y',now(),100,now(),100,'Request Kanban','Kanban board for request management',NULL,'N',generate_uuid() "
			+ "FROM AD_Language l WHERE l.IsActive='Y' AND l.IsSystemLanguage='Y' AND l.IsBaseLanguage='N' "
			+ "AND NOT EXISTS (SELECT 1 FROM AD_Form_Trl t WHERE t.AD_Form_ID="+id+" AND t.AD_Language=l.AD_Language)", false, null);
		DB.executeUpdate("UPDATE AD_Form_Trl SET Name='需求看板',Description='iDempiere 需求工單看板管理',IsTranslated='Y' WHERE AD_Form_ID="+id+" AND AD_Language='zh_TW'", false, null);
	}

	private void ensureMenu() {
		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_Menu WHERE AD_Menu_UU=?", MENU_UU) > 0) return;
		int formId = DB.getSQLValueEx(null, "SELECT AD_Form_ID FROM AD_Form WHERE AD_Form_UU=?", FORM_UU);
		if (formId <= 0) return;
		int id = DB.getNextID(0, "AD_Menu", null);
		DB.executeUpdateEx("INSERT INTO AD_Menu (AD_Menu_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,"
			+ "Updated,UpdatedBy,Name,Description,IsSummary,IsSOTrx,IsReadOnly,Action,AD_Form_ID,EntityType,AD_Menu_UU) "
			+ "VALUES (?,0,0,'Y',now(),100,now(),100,'Request Kanban','Kanban board for request management',"
			+ "'N','N','N','X',?,'U',?)", new Object[]{id, formId, MENU_UU}, null);
		// i18n
		DB.executeUpdate("INSERT INTO AD_Menu_Trl (AD_Menu_ID,AD_Language,AD_Client_ID,AD_Org_ID,IsActive,"
			+ "Created,CreatedBy,Updated,UpdatedBy,Name,Description,IsTranslated,AD_Menu_Trl_UU) "
			+ "SELECT "+id+",l.AD_Language,0,0,'Y',now(),100,now(),100,'Request Kanban','Kanban board for request management','N',generate_uuid() "
			+ "FROM AD_Language l WHERE l.IsActive='Y' AND l.IsSystemLanguage='Y' AND l.IsBaseLanguage='N' "
			+ "AND NOT EXISTS (SELECT 1 FROM AD_Menu_Trl t WHERE t.AD_Menu_ID="+id+" AND t.AD_Language=l.AD_Language)", false, null);
		DB.executeUpdate("UPDATE AD_Menu_Trl SET Name='需求看板',Description='iDempiere 需求工單看板管理',IsTranslated='Y' WHERE AD_Menu_ID="+id+" AND AD_Language='zh_TW'", false, null);
		// Tree
		int treeId = DB.getSQLValueEx(null, "SELECT AD_Tree_Menu_ID FROM AD_ClientInfo WHERE AD_Client_ID=0");
		if (treeId <= 0) treeId = 10;
		DB.executeUpdate("INSERT INTO AD_TreeNodeMM (AD_Tree_ID,Node_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,Parent_ID,SeqNo,AD_TreeNodeMM_UU) "
			+ "SELECT "+treeId+","+id+",0,0,'Y',now(),100,now(),100,500,99,generate_uuid() "
			+ "WHERE NOT EXISTS (SELECT 1 FROM AD_TreeNodeMM WHERE AD_Tree_ID="+treeId+" AND Node_ID="+id+")", false, null);
		// Access
		DB.executeUpdate("INSERT INTO AD_Form_Access (AD_Form_ID,AD_Role_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,Updated,UpdatedBy,IsReadWrite,AD_Form_Access_UU) "
			+ "SELECT "+formId+",r.AD_Role_ID,r.AD_Client_ID,0,'Y',now(),100,now(),100,'Y',generate_uuid() "
			+ "FROM AD_Role r WHERE r.IsActive='Y' AND NOT EXISTS (SELECT 1 FROM AD_Form_Access a WHERE a.AD_Form_ID="+formId+" AND a.AD_Role_ID=r.AD_Role_ID)", false, null);
		log.info("Menu created: Request Kanban (ID=" + id + ")");
	}
}
