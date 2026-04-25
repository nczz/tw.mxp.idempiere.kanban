package tw.mxp.idempiere.kanban;

import java.util.logging.Level;

import org.adempiere.plugin.utils.Incremental2PackActivator;
import org.adempiere.webui.factory.IMappedFormFactory;
import org.compiere.util.CLogger;
import org.idempiere.model.IMappedModelFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class KanbanActivator extends Incremental2PackActivator {

	private static final CLogger log = CLogger.getCLogger(KanbanActivator.class);

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	@Override
	protected void afterPackIn() {
		BundleContext ctx = getContext();
		if (ctx == null) return;

		// Register @Form annotated classes (SCR @Reference doesn't work in WAB)
		try {
			ServiceReference<?> ref = ctx.getServiceReference(IMappedFormFactory.class.getName());
			if (ref != null) {
				IMappedFormFactory factory = (IMappedFormFactory) ctx.getService(ref);
				if (factory != null) {
					factory.scan(ctx, "tw.mxp.idempiere.kanban");
					log.info("Kanban forms registered via IMappedFormFactory");
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to register kanban forms", e);
		}

		// Register @Model annotated classes (MRequestKanban)
		try {
			ServiceReference<?> ref = ctx.getServiceReference(IMappedModelFactory.class.getName());
			if (ref != null) {
				IMappedModelFactory factory = (IMappedModelFactory) ctx.getService(ref);
				if (factory != null) {
					factory.scan(ctx, "tw.mxp.idempiere.kanban");
					log.info("Kanban models registered via IMappedModelFactory");
				}
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Failed to register kanban models", e);
		}
	}
}
