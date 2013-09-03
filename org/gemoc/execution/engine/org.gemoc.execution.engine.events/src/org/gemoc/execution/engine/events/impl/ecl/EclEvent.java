package org.gemoc.execution.engine.events.impl.ecl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.gemoc.execution.engine.actions.DomainSpecificAction;
import org.gemoc.execution.engine.actions.impl.methodref.MethodReferenceAction;
import org.gemoc.execution.engine.events.DomainSpecificEvent;

import fr.inria.aoste.trace.EventOccurrence;

/**
 * An implementation of Domain-Specific Events based on ECL. An EclEvent is
 * defined by a combination several CCSL clocks (?) and contains a reference to
 * a Domain-Specific Action that will be executed by the Executor.
 * 
 * @author flatombe
 */
public class EclEvent implements DomainSpecificEvent {
    private DomainSpecificAction action;
    private Boolean isEntryPoint;

    // TODO : not sure we need to use EventOccurrence here, maybe SolverClock ?
    // Anyway it should be the CCSL clocks generated by the transformation based
    // on the ECL information. // Outdated?!
    // private List<EventOccurrence> pattern;

    public EclEvent(DomainSpecificAction action, Boolean isEntryPoint) {
        this.action = action;
        this.isEntryPoint = isEntryPoint;
    }

    // TODO : L'event occurrence est renvoyée par le solveur et devrait
    // hopefully contenir une référence vers la DSA etc.
    public EclEvent(EventOccurrence eventOccurrence) {
        // TODO : ici remplir correctement l'objet.
        // Tout est dans le context ? Avec 1er EObject l'instance du contexte et
        // 2ème EObject la méthode
        // this.action = new MethodReferenceAction(eventOccurrence.getContext(),
        // eventOccurrence.getReferedElement());
    }

    private String getSimpleName(EObject eo) {
        Object res = null;
        res = this.invokeMethod(eo, "getName");
        return (String) res;
    }

    private Object invokeMethod(EObject eo, String methodName) {
        Method m = null;
        Object res = null;
        try {
            m = eo.getClass().getMethod(methodName);
            res = m.invoke(eo);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            try {
                m = eo.getClass().getMethod("EMFRENAME" + methodName); // dirty
                                                                       // fix
                                                                       // due to
                                                                       // kermeta
                                                                       // dirty
                                                                       // fix
                                                                       // :-/
                                                                       // ask
                                                                       // Didier
                res = m.invoke(eo);
            } catch (SecurityException e1) {
                e1.printStackTrace();
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
            } catch (IllegalArgumentException e1) {
                e1.printStackTrace();
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }

        return res;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gemoc.execution.engine.events.DomainSpecificEvent#getAction()
     */
    @Override
    public DomainSpecificAction getAction() {
        return this.action;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gemoc.execution.engine.events.DomainSpecificEvent#getTarget()
     */
    @Override
    public EObject getTarget() {
        return this.action.getTarget();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gemoc.execution.engine.events.DomainSpecificEvent#isFirst()
     */
    @Override
    public Boolean isEntryPoint() {
        return this.isEntryPoint;
    }

    public String toString() {
        return "EclEvent@[" + this.action.toString() + " ; isEntryPoint:" + this.isEntryPoint + "]";
    }

}
