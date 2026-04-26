package tw.mxp.idempiere.kanban;

import java.util.logging.Level;

import org.adempiere.base.event.EventManager;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.IFormController;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.ui.zk.annotation.Form;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.Clients;

/**
 * ZK Form controller for the Kanban Board SPA.
 * Generates JWT from ZK session context and loads the React SPA in an iframe.
 * Bridges postMessage between SPA and ZK (zoom, token refresh, real-time push).
 */
@Form
public class KanbanFormController implements IFormController {

	private static final CLogger log = CLogger.getCLogger(KanbanFormController.class);
	private KanbanForm form;
	private EventHandler pushHandler;

	public KanbanFormController() {
		form = new KanbanForm();

		// Guard: if no execution context (bookmark/serialize), skip initialization
		if (Executions.getCurrent() == null) return;

		// Generate JWT directly from ZK context (no HTTP round-trip)
		int clientId = Env.getContextAsInt(Env.getCtx(), "#AD_Client_ID");
		int orgId = Env.getContextAsInt(Env.getCtx(), "#AD_Org_ID");
		int userId = Env.getContextAsInt(Env.getCtx(), "#AD_User_ID");
		int roleId = Env.getContextAsInt(Env.getCtx(), "#AD_Role_ID");
		String userName = Env.getContext(Env.getCtx(), "#AD_User_Name");

		String token = JwtUtil.generate(clientId, orgId, userId, roleId, userName);

		// Build full URL — Executions.getCurrent() is only safe in constructor
		String base = Executions.getCurrent().getScheme() + "://"
				+ Executions.getCurrent().getServerName() + ":"
				+ Executions.getCurrent().getServerPort();
		form.loadSpa(base + "/kanban/web/index.html#token=" + token);

		setupPostMessageBridge(clientId, orgId, userId, roleId, userName);
		setupServerPush(clientId);
	}

	@Override
	public ADForm getForm() {
		return form;
	}

	private void setupPostMessageBridge(int clientId, int orgId, int userId, int roleId, String userName) {
		String iframeUuid = form.getIframe().getUuid();

		// Inject client-side JS listener for postMessage from SPA
		String script =
			"(function(){window.addEventListener('message',function(e){" +
			"if(!e.data||!e.data.type)return;" +
			"var w=zk.Widget.$('#" + iframeUuid + "');" +
			"if(e.data.type==='refresh-token'){" +
			"zAu.send(new zk.Event(w,'onTokenRefresh',null));" +
			"}else if(e.data.type==='zoom'){" +
			"zAu.send(new zk.Event(w,'onZoom',{data:[e.data.tableName+'_ID',String(e.data.recordId)]}));" +
			"}});})();";
		Clients.evalJavaScript(script);

		// Server-side listener for token refresh
		form.getIframe().addEventListener("onTokenRefresh", evt -> {
			String newToken = JwtUtil.generate(clientId, orgId, userId, roleId, userName);
			Clients.evalJavaScript(String.format(
				"var f=document.getElementById('%s');" +
				"if(f&&f.contentWindow)f.contentWindow.postMessage({type:'token-refreshed',token:'%s'},'*');",
				form.getIframe().getUuid(), newToken));
		});
	}

	private void setupServerPush(int clientId) {
		Desktop desktop;
		try {
			desktop = Executions.getCurrent().getDesktop();
		} catch (Exception e) {
			log.log(Level.FINE, "No desktop available, skipping server push", e);
			return;
		}
		if (desktop == null) return;

		// Enable server push for this desktop
		desktop.enableServerPush(true);

		// Register OSGi EventHandler for kanban/refresh topic
		pushHandler = new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				if (!desktop.isAlive()) return;
				Object evtClientId = event.getProperty("AD_Client_ID");
				if (evtClientId instanceof Integer && (Integer) evtClientId != clientId) return;

				try {
					Executions.schedule(desktop, e -> {
						String iframeUuid = form.getIframe().getUuid();
						Clients.evalJavaScript(
							"var f=document.getElementById('" + iframeUuid + "');" +
							"if(f&&f.contentWindow)f.contentWindow.postMessage({type:'kanban-refresh'},'*');");
					}, new org.zkoss.zk.ui.event.Event("onKanbanRefresh"));
				} catch (Exception ex) {
					log.log(Level.FINE, "Push failed", ex);
				}
			}
		};
		EventManager.getInstance().register("kanban/refresh", pushHandler);

		// Cleanup on form close
		form.addEventListener("onDetach", evt -> {
			if (pushHandler != null) {
				EventManager.getInstance().unregister(pushHandler);
				pushHandler = null;
			}
			try {
				desktop.enableServerPush(false);
			} catch (Exception ignored) {}
		});
	}
}
