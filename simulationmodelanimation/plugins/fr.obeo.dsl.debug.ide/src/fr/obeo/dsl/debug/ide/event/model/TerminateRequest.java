/*******************************************************************************
 * Copyright (c) 2015 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package fr.obeo.dsl.debug.ide.event.model;

/**
 * Request sent to terminate the execution of a thread or the debugger.
 * 
 * @author <a href="mailto:yvan.lussaud@obeo.fr">Yvan Lussaud</a>
 */
public class TerminateRequest extends AbstractThreadRequest {

	/**
	 * Constructor for debugger.
	 */
	public TerminateRequest() {
		super(null);
	}

	/**
	 * Constructor for thread.
	 * 
	 * @param threadName
	 *            the {@link fr.obeo.dsl.debug.Thread#getName() thread name}
	 */
	public TerminateRequest(String threadName) {
		super(threadName);
	}

}
