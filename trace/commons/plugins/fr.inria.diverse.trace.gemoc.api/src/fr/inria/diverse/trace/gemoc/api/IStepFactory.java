package fr.inria.diverse.trace.gemoc.api;

import java.util.List;

public interface IStepFactory {

	public org.gemoc.executionframework.engine.mse.SequentialLogicalStep createSequentialLogicalStep(
			org.gemoc.executionframework.engine.mse.MSE mse, List<Object> parameters, List<Object> result);
	
}
