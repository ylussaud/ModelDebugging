/*******************************************************************************
 * Copyright (c) 2016 Inria and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Inria - initial API and implementation
 *******************************************************************************/
package org.gemoc.execution.sequential.javaengine.ui.launcher;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.ui.business.api.dialect.DialectEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.gemoc.executionframework.ui.IMSEPresenter;

import fr.inria.diverse.trace.commons.model.trace.MSE;
import fr.inria.diverse.trace.commons.model.trace.Step;
import fr.obeo.dsl.debug.ide.adapter.DSLStackFrameAdapter;
import fr.obeo.dsl.debug.ide.sirius.ui.DSLDebugModelPresentation;
import fr.obeo.dsl.debug.ide.sirius.ui.SiriusEditorUtils;

public class GemocDebugModelPresentation extends DSLDebugModelPresentation {

	@Override
	public IEditorInput getEditorInput(Object element) {
		final IEditorInput res;

		if (element instanceof MSE && ((MSE) element).getCaller() != null) {
			res = super.getEditorInput(((MSE) element).getCaller());
		} else {
			res = super.getEditorInput(element);
		}

		return res;
	}

	@Override
	public String getEditorId(IEditorInput input, Object element) {
		final String res;

		if (element instanceof MSE && ((MSE) element).getCaller() != null) {
			res = super.getEditorId(input, ((MSE) element).getCaller());
		} else {
			res = super.getEditorId(input, element);
		}

		return res;
	}

	@Override
	public boolean addAnnotations(IEditorPart editorPart, IStackFrame frame) {
		if (frame instanceof DSLStackFrameAdapter) {
			changeCurrentStackFrame(frame);
			if (editorPart instanceof DialectEditor) {
				EObject instruction = ((DSLStackFrameAdapter) frame).getCurrentInstruction();
				if (instruction instanceof Step) {
					final List<MSE> tickedEvents = new ArrayList<>();
					tickedEvents.add(((Step) instruction).getMseoccurrence().getMse());
					showEvents(tickedEvents);
					final Set<EObject> callers = new LinkedHashSet<EObject>();
					for (MSE event : tickedEvents) {
						if (event.getCaller() != null) {
							callers.add(event.getCaller());
						}
					}
					try {
						SiriusEditorUtils.showInstructions((DialectEditor) editorPart, new ArrayList<EObject>(callers));
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						SiriusEditorUtils.showInstruction((DialectEditor) editorPart, instruction);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				super.addAnnotations(editorPart, frame);
			}
		}

		return false;
	}

	private void showEvents(List<MSE> events) {
		final List<URI> uris = new ArrayList<URI>();
		for (MSE event : events) {
			uris.add(EcoreUtil.getURI(event));
		}
		for (IMSEPresenter presenter : org.gemoc.executionframework.ui.Activator.getDefault().getEventPresenters()) {
			presenter.present(uris);
		}
	}

}
