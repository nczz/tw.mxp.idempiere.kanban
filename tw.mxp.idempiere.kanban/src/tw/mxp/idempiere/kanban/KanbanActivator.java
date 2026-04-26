package tw.mxp.idempiere.kanban;

import java.util.logging.Level;

import org.adempiere.plugin.utils.Incremental2PackActivator;
import org.adempiere.webui.factory.IMappedFormFactory;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.idempiere.model.IMappedModelFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Plugin activator. Extends Incremental2PackActivator for version tracking.
 * 2Pack_1.0.0.zip in META-INF/ is processed automatically on first install.
 * afterPackIn() creates tables, AD records, translations, and registers factories.
 */
public class KanbanActivator extends Incremental2PackActivator {

	private static final CLogger log = CLogger.getCLogger(KanbanActivator.class);
	private static final String FORM_UU = "tw-mxp-idempiere-kanban-form-001";
	private static final String MENU_UU = "tw-mxp-idempiere-kanban-menu-001";

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	@Override
	protected void afterPackIn() {
		BundleContext ctx = getContext();
		if (ctx == null) return;

		ensureTables();
		ensureForm();
		ensureMenu();
		registerFactories(ctx);
	}

	private void registerFactories(BundleContext ctx) {
		registerFactory(ctx, IMappedFormFactory.class, "forms");
		registerFactory(ctx, IMappedModelFactory.class, "models");
	}

	@SuppressWarnings("unchecked")
	private void registerFactory(BundleContext ctx, Class<?> serviceClass, String label) {
		try {
			ServiceReference<?> ref = ctx.getServiceReference(serviceClass.getName());
			if (ref == null) return;
			Object factory = ctx.getService(ref);
			if (factory == null) return;

			// Use direct cast instead of reflection for compatibility
			if (factory instanceof IMappedFormFactory) {
				((IMappedFormFactory) factory).scan(ctx, "tw.mxp.idempiere.kanban");
			} else if (factory instanceof IMappedModelFactory) {
				((IMappedModelFactory) factory).scan(ctx, "tw.mxp.idempiere.kanban");
			}
			log.info("Kanban " + label + " registered");
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to register kanban " + label, e);
		}
	}

	private void ensureTables() {
		if (!tableExists("RK_Card_Move_Log")) {
			DB.executeUpdate("CREATE TABLE RK_Card_Move_Log ("
				+ "RK_Card_Move_Log_ID NUMERIC(10) NOT NULL, "
				+ "RK_Card_Move_Log_UU VARCHAR(36) DEFAULT generate_uuid(), "
				+ "AD_Client_ID NUMERIC(10) NOT NULL, "
				+ "AD_Org_ID NUMERIC(10) NOT NULL DEFAULT 0, "
				+ "IsActive CHAR(1) NOT NULL DEFAULT 'Y', "
				+ "Created TIMESTAMP NOT NULL DEFAULT statement_timestamp(), "
				+ "CreatedBy NUMERIC(10) NOT NULL, "
				+ "Updated TIMESTAMP NOT NULL DEFAULT statement_timestamp(), "
				+ "UpdatedBy NUMERIC(10) NOT NULL, "
				+ "R_Request_ID NUMERIC(10) NOT NULL, "
				+ "R_Status_ID_From NUMERIC(10), "
				+ "R_Status_ID_To NUMERIC(10) NOT NULL, "
				+ "Note VARCHAR(2000), "
				+ "CONSTRAINT RK_Card_Move_Log_Key PRIMARY KEY (RK_Card_Move_Log_ID))", false, null);
			ensureSequence("RK_Card_Move_Log");
		}
		if (!tableExists("RK_Card_Member")) {
			DB.executeUpdate("CREATE TABLE RK_Card_Member ("
				+ "RK_Card_Member_ID NUMERIC(10) NOT NULL, "
				+ "RK_Card_Member_UU VARCHAR(36) DEFAULT generate_uuid(), "
				+ "AD_Client_ID NUMERIC(10) NOT NULL, "
				+ "AD_Org_ID NUMERIC(10) NOT NULL DEFAULT 0, "
				+ "IsActive CHAR(1) NOT NULL DEFAULT 'Y', "
				+ "Created TIMESTAMP NOT NULL DEFAULT statement_timestamp(), "
				+ "CreatedBy NUMERIC(10) NOT NULL, "
				+ "Updated TIMESTAMP NOT NULL DEFAULT statement_timestamp(), "
				+ "UpdatedBy NUMERIC(10) NOT NULL, "
				+ "R_Request_ID NUMERIC(10) NOT NULL, "
				+ "AD_User_ID NUMERIC(10) NOT NULL, "
				+ "MemberRole VARCHAR(20) DEFAULT 'Observer', "
				+ "CONSTRAINT RK_Card_Member_Key PRIMARY KEY (RK_Card_Member_ID))", false, null);
			ensureSequence("RK_Card_Member");
		}
		if (!tableExists("RK_Request_Type_Config")) {
			DB.executeUpdate("CREATE TABLE RK_Request_Type_Config ("
				+ "RK_Request_Type_Config_ID NUMERIC(10) NOT NULL, "
				+ "RK_Request_Type_Config_UU VARCHAR(36) DEFAULT generate_uuid(), "
				+ "AD_Client_ID NUMERIC(10) NOT NULL, "
				+ "AD_Org_ID NUMERIC(10) NOT NULL DEFAULT 0, "
				+ "IsActive CHAR(1) NOT NULL DEFAULT 'Y', "
				+ "Created TIMESTAMP NOT NULL DEFAULT statement_timestamp(), "
				+ "CreatedBy NUMERIC(10) NOT NULL, "
				+ "Updated TIMESTAMP NOT NULL DEFAULT statement_timestamp(), "
				+ "UpdatedBy NUMERIC(10) NOT NULL, "
				+ "R_RequestType_ID NUMERIC(10) NOT NULL, "
				+ "Default_AD_Role_ID NUMERIC(10), "
				+ "Default_SalesRep_ID NUMERIC(10), "
				+ "CONSTRAINT RK_Request_Type_Config_Key PRIMARY KEY (RK_Request_Type_Config_ID))", false, null);
			ensureSequence("RK_Request_Type_Config");
		}
	}

	private void ensureForm() {
		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_Form WHERE AD_Form_UU=?", FORM_UU) > 0)
			return;
		int formId = DB.getNextID(0, "AD_Form", null);
		DB.executeUpdateEx(
			"INSERT INTO AD_Form (AD_Form_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,"
			+ "Updated,UpdatedBy,Name,Description,Classname,AccessLevel,IsBetaFunctionality,EntityType,AD_Form_UU) "
			+ "VALUES (?,0,0,'Y',now(),100,now(),100,'Request Kanban','Kanban board for request management',"
			+ "'tw.mxp.idempiere.kanban.KanbanFormController','3','N','U',?)",
			new Object[]{formId, FORM_UU}, null);
		// Translations
		DB.executeUpdate(
			"INSERT INTO AD_Form_Trl (AD_Form_ID,AD_Language,AD_Client_ID,AD_Org_ID,IsActive,"
			+ "Created,CreatedBy,Updated,UpdatedBy,Name,Description,Help,IsTranslated,AD_Form_Trl_UU) "
			+ "SELECT " + formId + ",l.AD_Language,0,0,'Y',now(),100,now(),100,"
			+ "'Request Kanban','Kanban board for request management',NULL,'N',generate_uuid() "
			+ "FROM AD_Language l WHERE l.IsActive='Y' AND l.IsSystemLanguage='Y' AND l.IsBaseLanguage='N' "
			+ "AND NOT EXISTS (SELECT 1 FROM AD_Form_Trl t WHERE t.AD_Form_ID=" + formId + " AND t.AD_Language=l.AD_Language)",
			false, null);
		DB.executeUpdate("UPDATE AD_Form_Trl SET Name='需求看板',Description='iDempiere 需求工單看板管理',IsTranslated='Y' "
			+ "WHERE AD_Form_ID=" + formId + " AND AD_Language='zh_TW'", false, null);
	}

	private void ensureMenu() {
		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_Menu WHERE AD_Menu_UU=?", MENU_UU) > 0)
			return;
		int formId = DB.getSQLValueEx(null, "SELECT AD_Form_ID FROM AD_Form WHERE AD_Form_UU=?", FORM_UU);
		if (formId <= 0) return;

		int menuId = DB.getNextID(0, "AD_Menu", null);
		DB.executeUpdateEx(
			"INSERT INTO AD_Menu (AD_Menu_ID,AD_Client_ID,AD_Org_ID,IsActive,Created,CreatedBy,"
			+ "Updated,UpdatedBy,Name,Description,IsSummary,IsSOTrx,IsReadOnly,Action,AD_Form_ID,EntityType,AD_Menu_UU) "
			+ "VALUES (?,0,0,'Y',now(),100,now(),100,'Request Kanban','Kanban board for request management',"
			+ "'N','N','N','X',?,'U',?)",
			new Object[]{menuId, formId, MENU_UU}, null);
		// Translations
		DB.executeUpdate(
			"INSERT INTO AD_Menu_Trl (AD_Menu_ID,AD_Language,AD_Client_ID,AD_Org_ID,IsActive,"
			+ "Created,CreatedBy,Updated,UpdatedBy,Name,Description,IsTranslated,AD_Menu_Trl_UU) "
			+ "SELECT " + menuId + ",l.AD_Language,0,0,'Y',now(),100,now(),100,"
			+ "'Request Kanban','Kanban board for request management','N',generate_uuid() "
			+ "FROM AD_Language l WHERE l.IsActive='Y' AND l.IsSystemLanguage='Y' AND l.IsBaseLanguage='N' "
			+ "AND NOT EXISTS (SELECT 1 FROM AD_Menu_Trl t WHERE t.AD_Menu_ID=" + menuId + " AND t.AD_Language=l.AD_Language)",
			false, null);
		DB.executeUpdate("UPDATE AD_Menu_Trl SET Name='需求看板',Description='iDempiere 需求工單看板管理',IsTranslated='Y' "
			+ "WHERE AD_Menu_ID=" + menuId + " AND AD_Language='zh_TW'", false, null);
		// Tree
		int treeId = DB.getSQLValueEx(null, "SELECT AD_Tree_Menu_ID FROM AD_ClientInfo WHERE AD_Client_ID=0");
		if (treeId <= 0) treeId = 10;
		DB.executeUpdate(
			"INSERT INTO AD_TreeNodeMM (AD_Tree_ID,Node_ID,AD_Client_ID,AD_Org_ID,IsActive,"
			+ "Created,CreatedBy,Updated,UpdatedBy,Parent_ID,SeqNo,AD_TreeNodeMM_UU) "
			+ "SELECT " + treeId + "," + menuId + ",0,0,'Y',now(),100,now(),100,500,99,generate_uuid() "
			+ "WHERE NOT EXISTS (SELECT 1 FROM AD_TreeNodeMM WHERE AD_Tree_ID=" + treeId + " AND Node_ID=" + menuId + ")",
			false, null);
		// Access
		DB.executeUpdate(
			"INSERT INTO AD_Form_Access (AD_Form_ID,AD_Role_ID,AD_Client_ID,AD_Org_ID,IsActive,"
			+ "Created,CreatedBy,Updated,UpdatedBy,IsReadWrite,AD_Form_Access_UU) "
			+ "SELECT " + formId + ",r.AD_Role_ID,r.AD_Client_ID,0,'Y',now(),100,now(),100,'Y',generate_uuid() "
			+ "FROM AD_Role r WHERE r.IsActive='Y' "
			+ "AND NOT EXISTS (SELECT 1 FROM AD_Form_Access a WHERE a.AD_Form_ID=" + formId + " AND a.AD_Role_ID=r.AD_Role_ID)",
			false, null);
	}

	private boolean tableExists(String name) {
		return DB.getSQLValueEx(null,
			"SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name)=?",
			name.toLowerCase()) > 0;
	}

	private void ensureSequence(String tableName) {
		if (DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_Sequence WHERE Name=? AND IsTableID='Y'", tableName) > 0)
			return;
		int seqId = DB.getNextID(0, "AD_Sequence", null);
		DB.executeUpdateEx(
			"INSERT INTO AD_Sequence (AD_Sequence_ID,Name,CurrentNext,IsAudited,StartNewYear,"
			+ "Description,IsActive,IsTableID,AD_Client_ID,AD_Org_ID,Created,CreatedBy,Updated,UpdatedBy,"
			+ "IsAutoSequence,StartNo,IncrementNo,CurrentNextSys,AD_Sequence_UU) "
			+ "VALUES (?,'"+tableName+"',1000000,'N','N','Table "+tableName+"','Y','Y',0,0,now(),100,now(),100,"
			+ "'Y',1000000,1,200000,generate_uuid())",
			new Object[]{seqId}, null);
	}
}
