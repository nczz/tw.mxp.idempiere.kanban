package tw.mxp.idempiere.kanban;

import org.adempiere.webui.panel.CustomForm;
import org.zkoss.zul.Iframe;

/**
 * ZK Form UI — contains a full-screen iframe for the React SPA.
 * Does NOT call Executions.getCurrent() (would NPE on bookmark/serialize).
 */
public class KanbanForm extends CustomForm {

	private static final long serialVersionUID = 1L;
	private Iframe iframe;

	public KanbanForm() {
		iframe = new Iframe();
		iframe.setWidth("100%");
		iframe.setHeight("100%");
		iframe.setStyle("border:none;");
		appendChild(iframe);
		setWidth("100%");
		setHeight("100%");
		setStyle("position:absolute;top:0;left:0;right:0;bottom:0;");
	}

	/** Called by FormController with the full URL (including token). */
	public void loadSpa(String fullUrl) {
		iframe.setSrc(fullUrl);
	}

	/** Exposed for postMessage bridge — FormController needs iframe UUID. */
	public Iframe getIframe() {
		return iframe;
	}
}
