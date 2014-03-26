package org.gemoc.gemoc_language_workbench.utils.ui.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Shell;
import org.gemoc.gemoc_language_workbench.utils.Activator;
import org.gemoc.gemoc_language_workbench.utils.resourcevisitors.AIRDFileFinderResourceVisitor;

public class SelectAIRDIFileDialog extends SelectAnyIFileDialog {

	public SelectAIRDIFileDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected boolean select(IResource resource) {
		boolean result = super.select(resource);
		if(resource instanceof IFile){
			AIRDFileFinderResourceVisitor eclFinderVisitor = new AIRDFileFinderResourceVisitor();
			try {
				resource.accept(eclFinderVisitor);
				result = eclFinderVisitor.eclFile != null;
				
			} catch (CoreException e) {
				Activator.error(e.getMessage(), e);
			}	
		} 
		return result;
	}
}
