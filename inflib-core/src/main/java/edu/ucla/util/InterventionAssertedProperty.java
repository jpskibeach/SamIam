package edu.ucla.util;

import edu.ucla.belief.*;

import java.util.*;

/**
 * InterventionAssertedProperty determines if the given variable(s) are intervened upon based
 * on the EvidenceController. 
 * 
 * @author emily kao 
 * @since 20230405
 */
public class InterventionAssertedProperty extends FlagProperty {
    public static final InterventionAssertedProperty PROPERTY = new InterventionAssertedProperty(); 
    
    private InterventionAssertedProperty() {}

    public String getName() {
        return "intervention"; 
    }

    public EnumValue getDefault() {
        return FALSE; 
    }

    public String getID() {
        return "isintervention"; 
    }

    public boolean isUserEditable() {
        return false; 
    }

    public boolean isTransient() {
        return true; 
    }

    public static FlagValue valueFor(Variable var, EvidenceController controller) {
        return (controller.getIntervenedValue(var) == null) ? PROPERTY.FALSE : PROPERTY.TRUE; 
    }

    public static void setValue(Variable var, EvidenceController controller) {
        var.setProperty(PROPERTY, valueFor(var, controller));
    }

    public static void setAllValues(Collection<Object> vars, EvidenceController controller) {
        for (Iterator<Object> it = vars.iterator(); it.hasNext();) {
            setValue((Variable)it.next(), controller);
        }
    }
}
