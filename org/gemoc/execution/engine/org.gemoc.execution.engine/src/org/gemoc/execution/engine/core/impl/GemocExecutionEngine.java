package org.gemoc.execution.engine.core.impl;

import fr.inria.aoste.trace.EventOccurrence;
import fr.inria.aoste.trace.FiredStateKind;
import fr.inria.aoste.trace.LogicalStep;
import fr.inria.aoste.trace.ModelElementReference;
import fr.inria.aoste.trace.NamedReference;
import fr.inria.aoste.trace.Reference;
import glml.DomainSpecificEvent;
import glml.DomainSpecificEventFile;
import glml.ModelSpecificEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.gemoc.execution.engine.Activator;
import org.gemoc.execution.engine.core.ObservableBasicExecutionEngine;
import org.gemoc.gemoc_language_workbench.api.core.ExecutionEngine;
import org.gemoc.gemoc_language_workbench.api.dsa.EventExecutor;
import org.gemoc.gemoc_language_workbench.api.exceptions.EventInjectionException;
import org.gemoc.gemoc_language_workbench.api.feedback.FeedbackPolicy;
import org.gemoc.gemoc_language_workbench.api.moc.Solver;
import org.gemoc.gemoc_language_workbench.api.utils.ModelLoader;

/**
 * Implementation of the GEMOC Execution Engine. In particular, it is more
 * concerned about the model-specific elements such as the modelLoader and the
 * modelResource used for an execution.
 * 
 * 
 * @see ExecutionEngine
 * @see ObservableBasicExecutionEngine
 * 
 * @author flatombe
 * 
 */
public class GemocExecutionEngine extends ObservableBasicExecutionEngine {

	/**
	 * Derived from the language-specific elements using the loaded model.
	 */
	private Resource modelOfExecution = null;
	private Map<String, ModelSpecificEvent> modelSpecificEventsRegistry = null;
	private Resource solverInput = null;

	/**
	 * Delegated to the abstract class, for the language-level instantiation of
	 * the engine.
	 * 
	 * @see ObservableBasicExecutionEngine
	 * @param domainSpecificEventsResource
	 *            the resource containing the DomainSpecificEvents of the
	 *            language.
	 * @param solver
	 *            the solver corresponding to the MoC used by the xDSML.
	 * @param executor
	 *            the executor, able to execute code compiled from the language
	 *            used for the RTD/DSAs.
	 * @param feedbackPolicy
	 *            interprets the results from the DSAs to influence the
	 *            Solver/MoC.
	 */
	public GemocExecutionEngine(Resource domainSpecificEventsResource,
			Solver solver, EventExecutor executor, FeedbackPolicy feedbackPolicy) {
		super(domainSpecificEventsResource, solver, executor, feedbackPolicy);
		Activator.getMessagingSystem().info(
				"*** Engine construction done. ***", Activator.PLUGIN_ID);
	}

	@Override
	public void initialize(String modelURI, ModelLoader modelLoader) {
		Activator.getMessagingSystem().info(
				"Verifying input before instanciating GemocExecutionEngine...",
				Activator.PLUGIN_ID);

		// modelURI cannot be null or "", modelLoader cannot be null.
		if (modelURI == null | modelLoader == null | modelURI.isEmpty()) {
			String exceptionMessage = "";
			if (modelURI == null) {
				exceptionMessage += "modelURI is null, ";
			}
			if (modelLoader == null) {
				exceptionMessage += "modelLoader is null, ";
			}
			if (modelURI.isEmpty()) {
				exceptionMessage += "modelURI is empty, ";
			}
			Activator.getMessagingSystem().info(
					"...NOK. Throwing NullPointerException.",
					Activator.PLUGIN_ID);
			throw new NullPointerException(exceptionMessage);
		} else {

			Activator.getMessagingSystem().info(
					"...OK. Initializing GemocExecutionEngine with...",
					Activator.PLUGIN_ID);
			Activator.getMessagingSystem().info("\tmodelURI: " + modelURI,
					Activator.PLUGIN_ID);
			Activator.getMessagingSystem().info(
					"\tmodelLoader: " + modelLoader, Activator.PLUGIN_ID);

			this.modelStringURI = modelURI;
			this.modelLoader = modelLoader;

			// Create the modelResource from the modelLoader and the modelURI.
			this.modelResource = this.modelLoader.loadModel(modelURI);

			Activator.getMessagingSystem().info(
					"Model was successfully loaded: "
							+ modelResource.toString(), Activator.PLUGIN_ID);

			// TODO: Invoke the transformation that creates the model of
			// execution
			// from the DSEs.
			this.modelOfExecution = new ResourceSetImpl()
					.getResource(
							URI.createPlatformResourceURI(
									"/org.gemoc.sample.tfsm.instances/TrafficControl/test/MyModelSpecificEvents.glml",
									true), true);

			// TODO : remove when EclToCCslTranslator gets implemented.
			try {
				Resource solverInput = this.solver.getSolverInputBuilder()
						.build(this.modelOfExecution);

				this.solverInput = solverInput;

			} catch (UnsupportedOperationException e) {
				String solverInputFilePath = "/org.gemoc.sample.tfsm.instances/TrafficControl/test/test_executionModel.extendedCCSL";
				// String solverInputFilePath =
				// "/org.gemoc.sample.tfsm.instances/TrafficControl/test/MySolverInput.javasolverinput";
				this.solverInput = new ResourceSetImpl().getResource(URI
						.createPlatformResourceURI(solverInputFilePath, true),
						true);
				// this.solver.input.load(null) ???
			}
			this.solver.setSolverInputFile(this.solverInput.getURI());
			this.setCurrentStepAndUpdateTraces(this.getScheduledOrSolverStep());
			this.modelSpecificEventsRegistry = this
					.buildModelSpecificEventsRegistry(this.modelOfExecution);

			Activator.getMessagingSystem().info(
					"*** Engine initialization done. ***", Activator.PLUGIN_ID);
		}
	}

	/**
	 * Builds a map with, as keys, the names of the ModelSpecificEvents, as
	 * values, the ModelSpecificEvents.
	 * 
	 * @param modelOfExecution
	 * @return the registry of ModelSpecificEvents based on their names.
	 */
	private Map<String, ModelSpecificEvent> buildModelSpecificEventsRegistry(
			Resource modelOfExecution) {
		Map<String, ModelSpecificEvent> res = new HashMap<String, ModelSpecificEvent>();
		DomainSpecificEventFile dseFile = (DomainSpecificEventFile) modelOfExecution
				.getContents().get(0);
		for (ModelSpecificEvent mse : dseFile.getModelEvents()) {
			res.put(mse.getName(), mse);
		}
		return res;
	}

	@Override
	public void reset() {
		this.solver.setSolverInputFile(this.solverInput.getURI());
		this.scheduledEventsMap.clear();
		this.scheduledSteps.clear();
		this.schedulingTrace.clear();
		this.executionTrace.clear();
		this.setCurrentStepAndUpdateTraces(this.getScheduledOrSolverStep());
		this.setChanged();
		this.notifyObservers(">Reset!");
	}

	// TODO : this method will change when we change the DSE language.
	@Override
	protected Collection<ModelSpecificEvent> match(LogicalStep step) {
		Activator.getMessagingSystem().debug(
				"Matching the given step : " + step.toString()
						+ " containing: \n"
						+ step.getEventOccurrences().toString(),
				Activator.PLUGIN_ID);
		Collection<ModelSpecificEvent> res = new ArrayList<ModelSpecificEvent>();
		for (EventOccurrence eventOccurrence : step.getEventOccurrences()) {
			if (eventOccurrence.getFState() == FiredStateKind.TICK) {
				Activator.getMessagingSystem().debug(
						"FState is TICK for eventOccurrence: "
								+ eventOccurrence, Activator.PLUGIN_ID);

				/*
				 * if (eventOccurrence.getContext() != null &
				 * eventOccurrence.getReferedElement() != null) { // Case of the
				 * CCSL Solver EObject target = this
				 * .getEObjectFromReference(eventOccurrence .getContext());
				 * Activator.getMessagingSystem().debug(
				 * "Linked to the EObject: " + target, Activator.PLUGIN_ID); try
				 * { EOperation operation = (EOperation) this
				 * .getEObjectFromReference(eventOccurrence
				 * .getReferedElement()); Activator.getMessagingSystem().debug(
				 * "Linked to the EOperation: " + operation,
				 * Activator.PLUGIN_ID); ModelSpecificEvent mse = null;// = new
				 * ModelEvent( // "dseNamesNotUsedYet", new ModelAction(target,
				 * // operation, null), null); res.add(mse); } catch
				 * (ClassCastException e) { Activator.getMessagingSystem().warn(
				 * "... but not linked to an EOperation.", Activator.PLUGIN_ID);
				 * }
				 * 
				 * } else {
				 */
				// Case of the JavaSolver
				if (eventOccurrence.getReferedElement() != null) {
					if (eventOccurrence.getReferedElement() instanceof NamedReference) {
						NamedReference namedReference = (NamedReference) eventOccurrence
								.getReferedElement();
						res.add(this.modelSpecificEventsRegistry
								.get(namedReference.getValue()));
					}
				}
				/* } */
			}
		}
		return res;
	}

	@Override
	public void injectEvent(DomainSpecificEvent dse, EObject target)
			throws EventInjectionException {
		try {
			Activator.getMessagingSystem().info(
					"Trying injection of the following event : "
							+ dse.getName() + " on target: "
							+ target.toString(), Activator.PLUGIN_ID);

			// First we verify that the DSE is valid (with respect to the file
			// describing the DSEs) and that the target is indeed in the model
			// and that the metaclass of the target is the metaclass targetted
			// by the action of the DSE.

			EcoreUtil.EqualityHelper equalityHelper = new EcoreUtil.EqualityHelper();
			if (this.domainSpecificEventsRegistry.containsValue(dse)
					&& this.resourceContainsEObject(this.modelResource, target)
					&& equalityHelper.equals(dse.getDomainSpecificActions()
							.get(0).getTargetClass(), target.eClass())) {

				Activator
						.getMessagingSystem()
						.debug("DSE Registry and model resource contain required elements. Target is of valid type. Proceeding with the injection.",
								Activator.PLUGIN_ID);

				// If it is the case, then we retrieve the equivalent MSE
				ModelSpecificEvent mse = this
						.findCorrespondingModelSpecificEvent(dse, target,
								this.modelSpecificEventsRegistry);
				Activator.getMessagingSystem().debug(
						"The equivalent MSE is: " + mse.toString(),
						Activator.PLUGIN_ID);

				// We get the events allowed by the MoC for the current step.
				Collection<ModelSpecificEvent> allowedEvents = this
						.getPossibleEvents();
				Activator.getMessagingSystem().debug(
						"Allowed events are: " + allowedEvents.toString(),
						Activator.PLUGIN_ID);

				if (allowedEvents.contains(mse)) {
					// If the injected MSE is legal, then it is added to the
					// scheduled events of the current step.
					List<ModelSpecificEvent> scheduledEventsForCurrentStep = this.scheduledEventsMap
							.get(this.currentStep);
					Activator.getMessagingSystem().debug(
							scheduledEventsForCurrentStep.toString(),
							Activator.PLUGIN_ID);
					scheduledEventsForCurrentStep.add(mse);
					Activator.getMessagingSystem().info(
							"MSE added to the scheduled events of the step.",
							Activator.PLUGIN_ID);
				} else {
					// If the MSE is not authorized by the MoC, we throw an
					// exception and leave the GUI to deal with it (reinject
					// later,
					// send feedback to the user, etc...)
					Activator.getMessagingSystem().warn(
							"MSE was not allowed by the MoC.",
							Activator.PLUGIN_ID);
					throw new EventInjectionException(
							"Injecting this event is not allowed by the Model of Computation at that time.");
				}
			}

			Activator.getMessagingSystem().info(
					"Scheduling Trace: \n" + mapToString(this.schedulingTrace)
							+ "\n Scheduled Events: \n"
							+ mapToString(this.scheduledEventsMap),
					Activator.PLUGIN_ID);

		} catch (RuntimeException e) {
			Activator.error("RuntimeException during injection of event", e);
			throw e;
		}
	}

	

	@Override
	public Resource getModelResource() {
		return this.modelResource;
	}

	/**
	 * Searches through the given map for a Model-Specific Event which the
	 * following characteristics:
	 * <ul>
	 * <li>Its reification is "equals" to the DSE given in arguments. Because
	 * the DSE resource is loaded at two different places (once in the
	 * ModelOfExecution file, once by this Engine), the instances are not the
	 * same though the objects have the same features values. Which is why we
	 * use EcoreUtil.EqualityHelper.
	 * </ul>
	 * Its first (and only... ?) MSA's target is equals to the target given in
	 * the parameters (same as above).
	 * 
	 * @param dse
	 *            A Domain-Specific Event
	 * @param target
	 *            An EObject of the model
	 * @param map
	 *            The ModelSpecificEvents registry
	 * @return the MSE corresponding to the DSE on the given target.
	 */
	private ModelSpecificEvent findCorrespondingModelSpecificEvent(
			DomainSpecificEvent dse, EObject target,
			Map<String, ModelSpecificEvent> map) {
		Activator.getMessagingSystem().debug(
				"Trying to find the MSE with the following characteristics:"
						+ "\n - DSE: " + dse.getName() + "\n - Target: "
						+ target.toString() + "\n Among the following MSE map:"
						+ "\n" + this.mapToString(map), Activator.PLUGIN_ID);
		EcoreUtil.EqualityHelper equalityHelper = new EcoreUtil.EqualityHelper();
		for (ModelSpecificEvent mse : map.values()) {
			if (equalityHelper.equals(mse.getReification(), dse)
					&& equalityHelper.equals(
							mse.getModelSpecificActions().get(0).getTarget(),
							target)) {
				Activator.getMessagingSystem().debug(
						"Found corresponding MSE: " + mse.getName(),
						Activator.PLUGIN_ID);
				return mse;
			}
		}
		Activator
				.getMessagingSystem()
				.debug("Could not find corresponding MSE. Throwing NoSuchElementException.",
						Activator.PLUGIN_ID);
		throw new NoSuchElementException();
	}

	private boolean resourceContainsEObject(Resource resource, EObject eo) {
		Iterator<EObject> it = resource.getAllContents();
		while (it.hasNext()) {
			EObject content = it.next();
			if (content.equals(eo)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * All 5 methods below are a hack needed at some point to retrieve the
	 * EObject from a qualified name.
	 */

	/**
	 * Hack to retrieve an EObject from its qualified name...
	 * 
	 * @param modelResource
	 * @param qualifiedName
	 * @return
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public EObject getEObjectFromQualifiedName(Resource modelResource,
			String qualifiedName) throws SecurityException,
			IllegalArgumentException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {

		Iterator<EObject> iterator = modelResource.getAllContents();// this.modelResource.getContents().get(0).eAllContents();
		EObject res = null;
		while (iterator.hasNext() & res == null) {
			EObject modelElement = iterator.next();
			if (this.getQualifiedName(modelElement).equals(qualifiedName)) {
				res = modelElement;
			}
		}
		return res;
	}

	/**
	 * Part of the hack to get an EObject from its qualified name.
	 * 
	 * @param reference
	 * @return
	 */
	private EObject getEObjectFromReference(Reference reference) {

		EList<EObject> elements = ((ModelElementReference) reference)
				.getElementRef();
		if (reference instanceof ModelElementReference) {
			// Returns EObject thanks to the list of EObjects
			return elements.get(elements.size() - 1);
		} else if (reference instanceof NamedReference) {
			// Returns EObject thanks to its qualified name
			try {
				EObject res = getEObjectFromQualifiedName(this.modelResource,
						(((NamedReference) reference).getValue()));
				Activator.getMessagingSystem().debug("Returning :" + res,
						Activator.PLUGIN_ID);
				return res;
			} catch (SecurityException e) {
				String errorMessage = e.getClass().getSimpleName()
						+ " when trying to retrieve an EObject from the model from a NamedReference";
				Activator.getMessagingSystem().error(errorMessage,
						Activator.PLUGIN_ID);
				Activator.error(errorMessage, e);
				return null;
			} catch (IllegalArgumentException e) {
				String errorMessage = e.getClass().getSimpleName()
						+ " when trying to retrieve an EObject from the model from a NamedReference";
				Activator.getMessagingSystem().error(errorMessage,
						Activator.PLUGIN_ID);
				Activator.error(errorMessage, e);
				return null;
			} catch (NoSuchMethodException e) {
				String errorMessage = e.getClass().getSimpleName()
						+ " when trying to retrieve an EObject from the model from a NamedReference";
				Activator.getMessagingSystem().error(errorMessage,
						Activator.PLUGIN_ID);
				Activator.error(errorMessage, e);
				return null;
			} catch (IllegalAccessException e) {
				String errorMessage = e.getClass().getSimpleName()
						+ " when trying to retrieve an EObject from the model from a NamedReference";
				Activator.getMessagingSystem().error(errorMessage,
						Activator.PLUGIN_ID);
				Activator.error(errorMessage, e);
				return null;
			} catch (InvocationTargetException e) {
				String errorMessage = e.getClass().getSimpleName()
						+ " when trying to retrieve an EObject from the model from a NamedReference";
				Activator.getMessagingSystem().error(errorMessage,
						Activator.PLUGIN_ID);
				Activator.error(errorMessage, e);
				return null;
			}

			// Iterator<EObject> modelIterator =
			// this.modelResource.getContents().get(0).eAllContents();
			// while (modelIterator.hasNext()) {
			// EObject eo = modelIterator.next();
			// if (this.getNameOfEObject(eo).equals(elements.get(elements.size()
			// - 1))) {
			// return eo;
			// }
			// }
		} else {
			throw new RuntimeException(
					"Context reference is neither a ModelElementReference nor a NamedElementReference");
		}
	}

	/**
	 * Part of the hack to get an EObject from its qualified name.
	 * 
	 * @param eo
	 * @return
	 */
	private String getQualifiedName(EObject eo) {
		String res = getSimpleName(eo);
		EObject tmp = eo.eContainer();
		while (tmp != null) {
			res = getSimpleName(tmp) + "::" + res;
			tmp = tmp.eContainer();
		}
		return res;
	}

	/**
	 * Part of the hack to get an EObject from its qualified name.
	 * 
	 * @param eo
	 * @return
	 */
	private String getSimpleName(EObject eo) {
		return this.invokeGetNameOnEObject(eo);
	}

	/**
	 * Part of the hack to get an EObject from its qualified name.
	 * 
	 * @param eObjectMethod
	 * @return
	 */
	private String invokeGetNameOnEObject(EObject eObjectMethod) {
		Method method;
		try {
			method = eObjectMethod.getClass().getMethod("getName");
			Object res = method.invoke(eObjectMethod);
			if (res instanceof String) {
				return (String) res;
			} else {
				return null;
			}
		} catch (SecurityException e) {
			String errorMessage = "SecurityException when trying to get the qualified name of an object";
			Activator.getMessagingSystem().error(errorMessage,
					Activator.PLUGIN_ID);
			Activator.error(errorMessage, e);
			return null;
		} catch (IllegalArgumentException e) {
			String errorMessage = "IllegalArgumentException when trying to get the qualified name of an object";
			Activator.getMessagingSystem().error(errorMessage,
					Activator.PLUGIN_ID);
			Activator.error(errorMessage, e);
			return null;
		} catch (NoSuchMethodException e) {
			String errorMessage = "NoSuchMethodException when trying to get the qualified name of an object";
			Activator.getMessagingSystem().error(errorMessage,
					Activator.PLUGIN_ID);
			Activator.error(errorMessage, e);
			return null;
		} catch (IllegalAccessException e) {
			String errorMessage = "IllegalAccessException when trying to get the qualified name of an object";
			Activator.getMessagingSystem().error(errorMessage,
					Activator.PLUGIN_ID);
			Activator.error(errorMessage, e);
			return null;
		} catch (InvocationTargetException e) {
			String errorMessage = "InvocationTargetException when trying to get the qualified name of an object";
			Activator.getMessagingSystem().error(errorMessage,
					Activator.PLUGIN_ID);
			Activator.error(errorMessage, e);
			return null;
		}
	}

}
