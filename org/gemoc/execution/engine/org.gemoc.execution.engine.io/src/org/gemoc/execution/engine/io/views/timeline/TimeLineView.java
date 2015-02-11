package org.gemoc.execution.engine.io.views.timeline;

import java.util.WeakHashMap;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.gemoc.commons.eclipse.ui.ViewHelper;
import org.gemoc.execution.engine.commons.trace.ModelExecutionTracingAddon;
import org.gemoc.execution.engine.commons.trace.ModelExecutionTracingException;
import org.gemoc.execution.engine.core.AbstractExecutionEngine;
import org.gemoc.execution.engine.io.views.AbstractUserDecider;
import org.gemoc.execution.engine.io.views.IMotorSelectionListener;
import org.gemoc.execution.engine.io.views.engine.EnginesStatusView;
import org.gemoc.execution.engine.trace.gemoc_execution_trace.Choice;
import org.gemoc.execution.engine.trace.gemoc_execution_trace.LogicalStep;
import org.gemoc.gemoc_language_workbench.api.core.EngineStatus.RunStatus;
import org.gemoc.gemoc_language_workbench.api.core.ExecutionMode;
import org.gemoc.gemoc_language_workbench.api.core.IExecutionEngine;

import fr.obeo.timeline.editpart.PossibleStepEditPart;
import fr.obeo.timeline.editpart.TimelineEditPartFactory;
import fr.obeo.timeline.view.AbstractTimelineView;

public class TimeLineView extends AbstractTimelineView implements IMotorSelectionListener
{

	public static final String ID = "org.gemoc.execution.engine.io.views.timeline.TimeLineView";

	public static final String FOLLOW_COMMAND_ID = "org.gemoc.execution.engine.io.views.timeline.Follow";

	/**
	 * The {@link AdapterFactory} created from the EMF registry.
	 */
	private final AdapterFactory adapterFactory = new ComposedAdapterFactory(
			ComposedAdapterFactory.Descriptor.Registry.INSTANCE);

	private IContentProvider _contentProvider;
	private ILabelProvider _labelProvider;
	
	private AbstractExecutionEngine _currentEngine;
	
	private WeakHashMap<IExecutionEngine, Integer> _positions = new WeakHashMap<IExecutionEngine, Integer>();
	
	public TimeLineView()
	{
		_contentProvider = new AdapterFactoryContentProvider(adapterFactory);
		_labelProvider = new AdapterFactoryLabelProvider(adapterFactory);
	}
	
	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		startListeningToMotorSelectionChange();
	}
	
	@Override
	public void dispose() {
		disposeTimeLineProvider();
		removeDoubleClickListener();
		stopListeningToMotorSelectionChange();
		super.dispose();
		_contentProvider.dispose();
		_labelProvider.dispose();
	}

	@Override
	public void createPartControl(Composite parent) 
	{
		super.createPartControl(parent);
		setDetailViewerContentProvider(_contentProvider);
		setDetailViewerLabelProvider(_labelProvider);
		_mouseListener = new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent e) 
			{
			}
			
			@Override
			public void mouseDown(MouseEvent e) 
			{
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent event) 
			{
				handleDoubleCick();
			}

		};
		
		getTimelineViewer()
			.getControl()
			.addMouseListener(_mouseListener);
	}
	
	private EnginesStatusView _enginesStatusView;

	private void startListeningToMotorSelectionChange() {
		_enginesStatusView = ViewHelper.retrieveView(EnginesStatusView.ID);
		if (_enginesStatusView != null) {
			_enginesStatusView.addMotorSelectionListener(this);
		}
	}

	private void stopListeningToMotorSelectionChange() {
		if (_enginesStatusView != null) {
			_enginesStatusView.removeMotorSelectionListener(this);
		}
	}
	
	private TimelineProvider _timelineProvider;
	private MouseListener _mouseListener = null;
	
	public void configure(AbstractExecutionEngine engine)
	{
		if (_currentEngine != engine
			|| _timelineProvider == null)
		{
			saveStartIndex();
			_currentEngine = engine;
			disposeTimeLineProvider();
			if (engine != null)
			{
				int start = getStartIndex(engine);
				_timelineProvider = new TimelineProvider(engine);
				setTimelineProvider(_timelineProvider, start);			
			}			
		}
	}

	private int getStartIndex(AbstractExecutionEngine engine) {
		int start = 0;
		if (_positions.containsKey(engine))
		{
			start = _positions.get(engine);
		}
		return start;
	}

	private void saveStartIndex() {
		if (_currentEngine != null)
		{
			_positions.put(_currentEngine, getStart());
		}
	}

	private void removeDoubleClickListener() {
		if (_mouseListener != null
			&& getTimelineViewer() != null
			&& getTimelineViewer().getControl() != null) 
		{
			getTimelineViewer()
				.getControl()
				.removeMouseListener(_mouseListener);			
		}
	}

	private void disposeTimeLineProvider() {
		if (_timelineProvider != null)
		{
			_timelineProvider.dispose();
			_timelineProvider = null;
			setTimelineProvider(_timelineProvider, 0);
		}
	}

	@Override
	public void motorSelectionChanged(IExecutionEngine engine) {
		update(engine);
	}

	private boolean canDisplayTimeline(IExecutionEngine engine)
	{
		if (engine.getExecutionContext().getExecutionMode().equals(ExecutionMode.Run)
			&& engine.getRunningStatus().equals(RunStatus.Stopped))
		{
			return true;
		}
		if (engine.getExecutionContext().getExecutionMode().equals(ExecutionMode.Animation))
		{
			return true;
		}	
		return false;
	}
	
	@Override
	public boolean hasDetailViewer() {
		return false;
	}

	@Override
	public String getFollowCommandID() {
		return FOLLOW_COMMAND_ID;
	}

	private void handleDoubleCick() {
		final ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection) {
			final Object selected = ((IStructuredSelection) selection).getFirstElement();
			if (selected instanceof PossibleStepEditPart) 
			{
				Object o1 = ((PossibleStepEditPart) selected).getModel().getChoice2();
				Object o2 = ((PossibleStepEditPart) selected).getModel().getPossibleStep();
				if (o1 instanceof Choice
					&& o2 instanceof LogicalStep)
				{								
					Choice choice = (Choice)o1;
					LogicalStep logicalStep = (LogicalStep)o2;
					if (_currentEngine.getRunningStatus().equals(RunStatus.WaitingLogicalStepSelection))
					{
						if (choice.getSelectedNextChoice() == null)
						{
							performExecutionStep(logicalStep);
						}
						else
						{
							Choice choiceToRestore = choice.getSelectedNextChoice();
							backToPastIfPossible(choiceToRestore);							
						}
					}
				}
			}
		}
	}

	private void performExecutionStep(LogicalStep logicalStep) 
	{
		if (_currentEngine.getExecutionContext().getLogicalStepDecider() instanceof AbstractUserDecider)
		{
			AbstractUserDecider decider = (AbstractUserDecider)_currentEngine.getExecutionContext().getLogicalStepDecider();
			decider.decideFromTimeLine(logicalStep);
		}
		return;
	}

	private void backToPastIfPossible(Choice choice) {
		if (_currentEngine.hasCapability(ModelExecutionTracingAddon.class)) 
		{
			try 
			{
				_currentEngine.getCapability(ModelExecutionTracingAddon.class).backToPast(choice);
			} catch (ModelExecutionTracingException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected TimelineEditPartFactory getTimelineEditPartFactory() {
		return new TimelineEditPartFactory(false);
	}
	
	public void update(IExecutionEngine engine)
	{
		if (engine != null)
		{
			if (canDisplayTimeline(engine))
			{
				configure((AbstractExecutionEngine)engine);				
			}
			else
			{
				disposeTimeLineProvider();
			}
		}		
	}
}
