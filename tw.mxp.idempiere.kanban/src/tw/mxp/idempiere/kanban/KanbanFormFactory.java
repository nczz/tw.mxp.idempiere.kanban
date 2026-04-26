package tw.mxp.idempiere.kanban;

import org.adempiere.webui.factory.AnnotationBasedFormFactory;
import org.adempiere.webui.factory.IFormFactory;
import org.osgi.service.component.annotations.Component;

/**
 * Scans this package for @Form annotated classes (KanbanFormController).
 * Works on iDempiere v11-v14 (does not depend on IMappedFormFactory.scan).
 */
@Component(immediate = true, service = IFormFactory.class, property = "service.ranking:Integer=100")
public class KanbanFormFactory extends AnnotationBasedFormFactory {

	@Override
	protected String[] getPackages() {
		return new String[]{"tw.mxp.idempiere.kanban"};
	}
}
