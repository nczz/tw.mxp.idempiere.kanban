package tw.mxp.idempiere.kanban;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.base.Model;
import org.compiere.model.MRequest;
import org.compiere.util.DB;

/**
 * Extended MRequest that protects EndTime from being cleared by core's
 * RequestEventHandler during save. Uses the "stash and restore" pattern:
 * beforeSave stashes EndTime → core runs → afterSave restores via direct SQL.
 */
@Model(table = "R_Request")
public class MRequestKanban extends MRequest {

	private static final long serialVersionUID = 1L;
	private static final String ENDTIME_ATTR = "RK_OriginalEndTime";

	public MRequestKanban(Properties ctx, int R_Request_ID, String trxName) {
		super(ctx, R_Request_ID, trxName);
	}

	public MRequestKanban(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// Stash EndTime before core's beforeSave potentially clears it
		Object endTime = get_Value("EndTime");
		if (endTime != null) {
			set_Attribute(ENDTIME_ATTR, endTime);
		}
		return super.beforeSave(newRecord);
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		boolean ok = super.afterSave(newRecord, success);
		if (success) {
			Object stashed = get_Attribute(ENDTIME_ATTR);
			if (stashed != null) {
				// Restore via direct SQL to avoid re-entering PO save lifecycle
				DB.executeUpdateEx(
					"UPDATE R_Request SET EndTime=? WHERE R_Request_ID=?",
					new Object[]{stashed, getR_Request_ID()},
					get_TrxName());
				set_ValueNoCheck("EndTime", stashed);
			}
		}
		return ok;
	}
}
