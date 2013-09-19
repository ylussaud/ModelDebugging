package org.gemoc.execution.engine.api_implementations.feedback;

import org.gemoc.gemoc_language_workbench.api.feedback.FeedbackData;


/**
 * A simple EObject.
 * 
 * @author flatombe
 * */
public class ObjectFeedbackData implements FeedbackData {
    Object o;

    public ObjectFeedbackData(Object feedback) {
        this.o = feedback;
    }

    public Object getObject() {
        return this.o;
    }

    public String toString() {
        return this.getClass().getName() + "@[" + this.o.toString() + "]";
    }
}
