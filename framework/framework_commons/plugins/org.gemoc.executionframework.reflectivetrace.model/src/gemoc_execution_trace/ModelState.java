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
/**
 */
package gemoc_execution_trace;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Model State</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link gemoc_execution_trace.ModelState#getModel <em>Model</em>}</li>
 *   <li>{@link gemoc_execution_trace.ModelState#getContextState <em>Context State</em>}</li>
 * </ul>
 *
 * @see gemoc_execution_trace.Gemoc_execution_tracePackage#getModelState()
 * @model
 * @generated
 */
public interface ModelState extends EObject {
	/**
	 * Returns the value of the '<em><b>Model</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Model</em>' reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Model</em>' reference.
	 * @see #setModel(EObject)
	 * @see gemoc_execution_trace.Gemoc_execution_tracePackage#getModelState_Model()
	 * @model required="true"
	 * @generated
	 */
	EObject getModel();

	/**
	 * Sets the value of the '{@link gemoc_execution_trace.ModelState#getModel <em>Model</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Model</em>' reference.
	 * @see #getModel()
	 * @generated
	 */
	void setModel(EObject value);

	/**
	 * Returns the value of the '<em><b>Context State</b></em>' reference list.
	 * The list contents are of type {@link gemoc_execution_trace.ContextState}.
	 * It is bidirectional and its opposite is '{@link gemoc_execution_trace.ContextState#getModelState <em>Model State</em>}'.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Context State</em>' reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Context State</em>' reference list.
	 * @see gemoc_execution_trace.Gemoc_execution_tracePackage#getModelState_ContextState()
	 * @see gemoc_execution_trace.ContextState#getModelState
	 * @model opposite="modelState"
	 * @generated
	 */
	EList<ContextState> getContextState();

} // ModelState
