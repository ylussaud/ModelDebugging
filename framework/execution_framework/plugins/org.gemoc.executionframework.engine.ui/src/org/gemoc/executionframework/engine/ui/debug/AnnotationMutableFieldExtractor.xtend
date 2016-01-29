package org.gemoc.executionframework.engine.ui.debug

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Map
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.transaction.RecordingCommand
import org.eclipse.emf.transaction.util.TransactionUtil
import org.gemoc.executionframework.engine.core.CommandExecution
import org.gemoc.executionframework.engine.ui.debug.IMutableFieldExtractor
import org.gemoc.executionframework.engine.ui.debug.MutableField
import org.gemoc.xdsmlframework.commons.DynamicAnnotationHelper
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider

class AnnotationMutableFieldExtractor implements IMutableFieldExtractor {

	val Map<EClass, Integer> counters = new HashMap

	override extractMutableField(EObject eObject) {

		val List<MutableField> result = new ArrayList<MutableField>()

		val idProp = eObject.eClass.EIDAttribute
		val String objectName = if (idProp != null) {
				val id = eObject.eGet(idProp);
				if (id != null) {
					val NumberFormat formatter = new DecimalFormat("00");
					val String idString = if(id instanceof Integer) formatter.format((id as Integer)) else id.toString;
					eObject.eClass.name + "_" + idString // "returned" value 
				} else {
					if (!counters.containsKey(eObject.eClass)) {
						counters.put(eObject.eClass, 0)
					}
					val Integer counter = counters.get(eObject.eClass)
					counters.put(eObject.eClass, counter + 1)
					eObject.eClass.name + "_" + counter
				}

			} else {
				val org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider nameprovider = new DefaultDeclarativeQualifiedNameProvider()
				val qname = nameprovider.getFullyQualifiedName(eObject)
				if(qname != null) 
					eObject.toString
				else 
					qname.toString
			}
		
		for (prop : eObject.eClass.EAllStructuralFeatures) {
			if (DynamicAnnotationHelper.isDynamic(prop)) {			
				val mut = new MutableField(
					/* name    */ prop.name+" (in "+eObject.eClass.getName + " : "+objectName+")",
					/* eObject */ eObject,
					/* mutProp */ prop,
					/* getter  */ [eObject.eGet(prop)],
					/* setter  */ [ o |

						val ed = TransactionUtil.getEditingDomain(eObject.eResource);
						var RecordingCommand command = new RecordingCommand(ed,
							"Setting value " + o + " in " + objectName +"."+prop.name+ " from the debugger") {
							protected override void doExecute() {
								eObject.eSet(prop, o)
							}
						};
						CommandExecution.execute(ed, command);

					]
				)
				result.add(mut)

			}
		}
		return result
	}

}