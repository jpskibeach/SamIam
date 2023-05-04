package edu.ucla.belief;

import edu.ucla.util.EvidenceAssertedProperty;
import edu.ucla.util.InterventionAssertedProperty;
import edu.ucla.util.WeakLinkedList;

import java.util.*;

/** @author keith cascio
	@since  20020719 */
public class EvidenceController implements Cloneable
{
	public static boolean FLAG_DEBUG = Definitions.DEBUG;

	public EvidenceController( BeliefNetwork bn )
	{
		if( FLAG_DEBUG ) Definitions.STREAM_VERBOSE.println( "new EvidenceController( "+bn.getClass().getName()+" )" );
		this.myBeliefNetwork = bn;
	}

	/** Freeze evidence.
		@arg frozen If true, disallow evidence assertions/retractions.
		@since 20051006 */
	public void setFrozen( boolean frozen ){
		this.myFlagFrozen = frozen;
	}

	/** @since 20051006 */
	public boolean isFrozen(){
		return this.myFlagFrozen;
	}

	/** @since 20051006 */
	private void testFrozen(){
		if( myFlagFrozen ){ throw new IllegalStateException( STR_ERR_MSG_FROZEN ); }
	}

	/** Check if the creation/removal of edge or creation/removal of variables is valid action.
	 * @since 20230405
	 */
	private EvidenceController pre( FiniteVariable toObserve, FiniteVariable toIntervene, FiniteVariable toRetract, Collection observeTargets, Collection interveneTargets, Collection retractTargets ){
		if( myFlagSuppressPre ){ return this; }
		testFrozen();
		if( myBeliefNetwork != null ){
			if( toRetract != null ){ myBeliefNetwork.fireAudit( toRetract, null, null, BeliefNetwork.Auditor.Deed.RETRACT ); }
			if( retractTargets != null ){ myBeliefNetwork.fireAudit( null, null, retractTargets, BeliefNetwork.Auditor.Deed.RETRACT ); }
			if( toObserve != null ){ myBeliefNetwork.fireAudit( toObserve, null, null, BeliefNetwork.Auditor.Deed.OBSERVE ); }
			if( observeTargets != null ){ myBeliefNetwork.fireAudit( null, null, observeTargets, BeliefNetwork.Auditor.Deed.OBSERVE ); }
			if( toIntervene != null ){ myBeliefNetwork.fireAudit(toIntervene, null, null, BeliefNetwork.Auditor.Deed.INTERVENTION ); }
			if( interveneTargets != null ){ myBeliefNetwork.fireAudit(null, null, interveneTargets, BeliefNetwork.Auditor.Deed.INTERVENTION); } 
		}
		return this;
	}

	public static final String STR_ERR_MSG_FROZEN = "Cannot change evidence because it is frozen.";

	/**
		<p>
		Use this method to disable/re-enable evidence
		change events.
		<p>
		If you disable evidence change events,
		you must remember to re-enable them before
		other evidence change listeners make changes.

 		@since 20020805 */
	public void setNotifyEnabled( boolean enabled )
	{
		myFlagDoNotify = enabled;
	}

	/**
	 * Removes variable from relevant data structures if it exists
	 * @since 20230405
	 */
	public Object removeVariable( Variable var )
	{
		Object ret = null; 
		if( myMapObservations.containsKey( var ) ){
			ret = myMapObservations.remove( var );
			myRecentEvidenceChangeVariables.remove( var );
			EvidenceAssertedProperty.setValue( var, this );
		}
		else if( myMapInterventions.containsKey( var ) ){
			ret = myMapInterventions.remove( var ); 
			myRecentEvidenceChangeVariables.remove( var );
			InterventionAssertedProperty.setValue( var, this );
		}
		return ret;
	}

	/**
	 * Returns the value associated with the given variable. Check if it is an observed or 
	 * intervened variable. If it is, return the associated value, else return null. 
	 * @since 20230405
	 */
	public Object getValue( Variable var )
	{
		return myMapObservations.containsKey( var ) ? getObservedValue( var ) : getIntervenedValue( var ); 
	}

	/**
	 * Returns the value associated with the observed variable. If variable is not observed,
	 * return null. 
	 * @since 20230405
	 */
	public Object getObservedValue( Variable observed )
	{
		return myMapObservations.get( observed ); 
	}

	/**
	 * Returns the value associated with the intervened variable. If variable is not intervened,
	 * return null. 
	 * @since 20230405
	 */
	public Object getIntervenedValue( Variable intervened )
	{
		return myMapInterventions.get( intervened ); 
	}

	/** @since 20060320 */
	public List getPriorityEvidenceChangeListeners( List ret )
	{
		boolean flagValid = (myPriorityEvidenceChangeListeners != null);
		if( ret == null ){
			if( flagValid ) ret = Collections.unmodifiableList( myPriorityEvidenceChangeListeners );
			else ret = Collections.EMPTY_LIST;
		}
		else{
			ret.clear();
			if( flagValid ) ret.addAll( myPriorityEvidenceChangeListeners );
		}
		return ret;
	}

	public void addPriorityEvidenceChangeListener( EvidenceChangeListener ecl )
	{
		if( myPriorityEvidenceChangeListeners == null ) return;
		if( myPriorityEvidenceChangeListeners.contains( ecl ) ){ myPriorityEvidenceChangeListeners.remove( ecl ); }
		    myPriorityEvidenceChangeListeners.addFirst( ecl );
	}

	/** @since 20030117 */
	public boolean removePriorityEvidenceChangeListener( EvidenceChangeListener ecl ){
		return myPriorityEvidenceChangeListeners.remove( ecl );
	}

	public void addEvidenceChangeListener( EvidenceChangeListener ecl )
	{
		if( myEvidenceChangeListeners == null ) return;
		if( myEvidenceChangeListeners.contains( ecl ) ){ myEvidenceChangeListeners.remove( ecl ); }
		myEvidenceChangeListeners.addLast( ecl );
	}

	public boolean removeEvidenceChangeListener( EvidenceChangeListener ecl )
	{
		return myEvidenceChangeListeners.remove( ecl );
	}

	public void addInterventionChangeListener( EvidenceChangeListener ecl )
	{
		if( myInterventionChangeListeners == null ) return;
		if( myInterventionChangeListeners.contains( ecl ) ){ myInterventionChangeListeners.remove( ecl ); }
		myInterventionChangeListeners.addLast( ecl );
	}

	public boolean removeInterventionChangeListener( EvidenceChangeListener ecl )
	{
		return myInterventionChangeListeners.remove( ecl );
	}

	protected void addRecentEvidenceChange( FiniteVariable var )
	{
		myRecentEvidenceChangeVariables.add( var );
	}

	/** Fire evidenceChanged() event when evidence has not actually changed,
		usually when the computed query answer has changed for some other reason,
		and you want registered listeners to know about it.

		@return The number of non-priority listeners notified.
		@since 20080227 */
	public int notifyNonPriorityListeners(){
		testFrozen();
		myEvidenceChangeListeners.cleanClearedReferences();
		EvidenceChangeListener[] array = (EvidenceChangeListener[]) myEvidenceChangeListeners.toArray( new EvidenceChangeListener[ myEvidenceChangeListeners.size() ] );
		EvidenceChangeEvent ece = new EvidenceChangeEvent( Collections.EMPTY_SET );
		for( int i=0; i<array.length; i++ ){ array[i].evidenceChanged( ece ); }
		return array.length;
	}

	/**
	 * Notify all relevant listeners that evidence has changed. This includes variables being 
	 * observed/unobserved or intervened/unintervened. 
	 * TODO: double check that this description is correct
	 * @return number notified (expand on this, number of what?)
	 * @since 20230405
	 */
	protected int fireEvidenceChanged( boolean nonPriority )
	{
		int count = 0;
		if( ! myRecentEvidenceChangeVariables.isEmpty() ){
			count = fireEvidenceChanged( new EvidenceChangeEvent( myRecentEvidenceChangeVariables ), nonPriority );
		}
		myRecentEvidenceChangeVariables.clear();
		return count;
	}

	/** @return number notified */
	protected int fireEvidenceChanged( EvidenceChangeEvent ece, boolean nonPriority )
	{
		testFrozen();

		warnPriorityListeners( ece );

		EvidenceChangeListener[] array = null;
		EvidenceChangeListener[] intervenedArray = null;
		if( nonPriority ){
			myEvidenceChangeListeners.cleanClearedReferences();
			array = (EvidenceChangeListener[]) myEvidenceChangeListeners.toArray( new EvidenceChangeListener[myEvidenceChangeListeners.size()] );
			for( int i=0; i<array.length; i++ ){ array[i].warning( ece ); }
		}

		int count = notifyPriorityListeners( ece );

		if( nonPriority ){ 
			for( int i=0; i<array.length; i++ ){ array[i].evidenceChanged( ece ); }
		}

		intervenedArray = (EvidenceChangeListener[]) myInterventionChangeListeners.toArray( new EvidenceChangeListener[myInterventionChangeListeners.size()] );
		for( int i=0; i<intervenedArray.length; i++ ){ intervenedArray[i].evidenceChanged( ece ); }
		
		return count + (array == null ? 0 : array.length) + (intervenedArray == null ? 0 : intervenedArray.length);
	}

	/** @since 20020814
		@return number notified */
	protected int notifyPriorityListeners( EvidenceChangeEvent ece ){
		EvidenceChangeListener next;
		int count = 0;
		for( Iterator it = myPriorityEvidenceChangeListeners.iterator(); it.hasNext(); ){
			if( (next = (EvidenceChangeListener) it.next()) == null ){ it.remove(); }
			else{ next.evidenceChanged( ece ); ++count; }
		}
		return count;
	}

	/** @since 20030710
		@return number warned */
	protected int warnPriorityListeners( EvidenceChangeEvent ece ){
		EvidenceChangeListener next;
		int count = 0;
		for( Iterator it = myPriorityEvidenceChangeListeners.iterator(); it.hasNext(); ){
			if( (next = (EvidenceChangeListener) it.next()) == null ){ it.remove(); }
			else{ next.warning( ece ); ++count; }
		}
		return count;
	}

	/** Returns if an Object is an observation or not. */
	public boolean isObservation( Object obj ) {
		return myMapObservations.containsKey( obj );
	}

	/** Add var=value to the list of observations. Observations accumulate, but
		conflicting assignments are replaced. For example if the current evidence
		is {X=a,Y=b}, calling observe(Z,c) would produce {X=a,Y=b,Z=c}.
		Subsequently calling observe(Y,d) would produce {X=a,Y=d,Z=c}
		@return number of changes */
	public int observe( FiniteVariable var, Object value ) throws StateNotFoundException
	{
		return observe( var, value, myFlagDoNotify );
	}

	/** @since 20020814
		@return number of changes */
	public int observeNotifyOnlyPriorityListeners( FiniteVariable var, Object value ) throws StateNotFoundException
	{
		return observe( var, value, false );
	}

	/** @since 20020814
		@return number of changes */
	protected int observe( FiniteVariable var, Object value, boolean nonPriority ) throws StateNotFoundException
	{
		if( ! myBeliefNetwork.contains( var ) ){ throw new RuntimeException( "EvidenceController.observe( " + var + " ) not contained in this belief network." ); }

		if( Definitions.DEBUG ){ Definitions.STREAM_VERBOSE.println( "EvidenceController.observe( " +var+ ", " +value+ ", " +nonPriority+ " )" ); }

		int ret = observeSansCheckSansNotify( var, value );
		if( ret > 0 ){ fireEvidenceChanged( nonPriority ); }
		return ret;
	}

	/**
	 * @since 20230405
	 * @return number of changes
	 */
	protected int observeSansCheckSansNotify( FiniteVariable var, Object value ) throws StateNotFoundException{
		if( ! var.contains( value ) ){ throw new StateNotFoundException( var, value ); }
		Object old = myMapObservations.get( var );
		if( ! value.equals( old ) ){
			pre( var, null, old == null ? null : var, null, null, null );
			myMapInterventions.remove( var ); 
			myMapObservations.put( var, value );
			addRecentEvidenceChange( var );
			InterventionAssertedProperty.setValue( var, this );
			EvidenceAssertedProperty.setValue( var, this );
			return 1;
		}
		return 0;
	}

	/** Adds the observations listed to the set of current observations
		@param evidence A mapping from FiniteVariables to the value they take on.
		@return count modified observations
	*/
	public int observe( Map evidence ) throws StateNotFoundException
	{
		Set evidenceVariables = evidence.keySet();
		pre( null, null, null, evidenceVariables, null, null );

		int         count =    0;
		myFlagSuppressPre = true;
		try{
			checkValidVariables( evidenceVariables );
			FiniteVariable var   = null;
			Object         value = null;
			for( Iterator  iter  = evidenceVariables.iterator(); iter.hasNext(); ){
				if( (value = evidence.get( var = (FiniteVariable) iter.next() )) != null ){
					count += observeSansCheckSansNotify( var, value );
				}
			}
		}finally{
			myFlagSuppressPre = false;
		}
		fireEvidenceChanged( myFlagDoNotify );

		return count;
	}

	/**
	* This differs from <code>observe(Map evidence)</code> in that the
	* old observations are completely removed, and replaced by the current
	* evidence. For example, if the old evidence state was {X=a,Y=b}, calling
	* setObservations({Y=c,Z=d}) would produce the observation set {Y=c,Z=d}.

	  @return count modified observations */
	public int setObservations( Map evidence ) throws StateNotFoundException
	{
		Set keys = myMapObservations.keySet();
		Set nukeys = evidence.keySet();
		Collection drop = new ArrayList( keys.size() );
		Object key = null;
		for( Iterator it = keys.iterator(); it.hasNext(); ){
			if( ! nukeys.contains( key = it.next() ) ){ drop.add( key ); }
		}
		pre( null, null, null, nukeys, null, drop.isEmpty() ? null : drop );

		int count = 0;
		myFlagSuppressPre = true;
		try{
			if( evidence == null ){ evidence = Collections.EMPTY_MAP; }
			try{
				for( Iterator it = keys.iterator(); it.hasNext(); ){
					myRecentEvidenceChangeVariables.add( key = it.next() );
					if( (! evidence.containsKey( key ) ) ||
						( evidence.get( key ) != myMapObservations.get( key )) ){ ++count; }
				}

				for( Iterator it = nukeys.iterator(); it.hasNext(); ){
					if( ! myMapObservations.containsKey( it.next() ) ){ ++count; }
				}
			}catch( Exception exception ){
				System.err.println( "warning: EvidenceController.setObservations() caught " + exception );
				count = -1;
				myRecentEvidenceChangeVariables.addAll( keys );
			}
			myMapObservations.clear();
			observe( evidence );
		}finally{
			myFlagSuppressPre = false;
		}
		return count;
	}

	/** Returns if an object is an intervention or not. */
	public boolean isIntervention( Object obj ){
		return myMapInterventions.containsKey( obj ); 
	}

	/**
	 * Add var=value to list of interventions. Interventions accumulate, but
	 * conflicting assignments are replaced. Removing value from observation list and adding it
	 * to intervention list. 
	 * @since 20230405
	 */
	public int intervene( FiniteVariable var, Object value ) throws StateNotFoundException
	{
		return intervene(var, value, myFlagDoNotify ); 
	}

	/**
	 * TODO
	 * @since 20230405
	 */
	protected int intervene( FiniteVariable var, Object value, boolean nonPriority ) throws StateNotFoundException
	{
		if ( ! myBeliefNetwork.contains( var ) ){ throw new RuntimeException( "EvidenceController.intervene( " + var + " ) not contained in this belief network." ); } 

		int ret = interveneSansCheckSansNotify( var, value ); 
		if( ret > 0 ){ fireEvidenceChanged( nonPriority ); }
		return ret; 
	}

	/**
	 * Intervene upon a variable except if it is already intervened. Interveneing if variable was 
	 * observed or unobserved is allowed due to paste functionality of instantiation clipboard. 
	 * Alert all relevant listeners that an observed value has become an intervened value. 
	 * @return number of changes 
	 * @since 20230405
	 */
	protected int interveneSansCheckSansNotify( FiniteVariable var, Object value ) throws StateNotFoundException
	{
		if( ! var.contains( value ) ){ throw new StateNotFoundException( var, value ); }
		Object prev_obsv = myMapObservations.get( var ); 
		pre( null, var, prev_obsv == null ? null : var, null, null, null ); 
		if( ! myMapInterventions.containsKey(var) || value != prev_obsv) {
			myMapObservations.remove( var ); 
			myMapInterventions.put( var, value ); 
			addRecentEvidenceChange( var );
			EvidenceAssertedProperty.setValue( var, this );
			InterventionAssertedProperty.setValue( var, this);
			return 1; 
		}
		return 0;
	}

	public int intervene( Map evidence ) throws StateNotFoundException
	{
		Set evidenceVariables = evidence.keySet(); 
		pre( null, null, null, null, evidenceVariables, null); 
	
		int count = 0; 
		myFlagSuppressPre = true; 
		try {
			checkValidVariables( evidenceVariables );
			FiniteVariable var = null; 
			Object value = null; 
			for (Iterator iter = evidenceVariables.iterator(); iter.hasNext();) {
				if( (value = evidence.get( var = (FiniteVariable) iter.next() )) != null ){
					count += interveneSansCheckSansNotify( var, value );
				}
			}
		} finally {
			myFlagSuppressPre = false;
		}
		fireEvidenceChanged( myFlagDoNotify );

		return count;
	}

	/**
	 * The old interventions are completely removed and replaced by the current evidence. 
	 * @since 20230405
	 */
	public int setInterventions( Map evidence ) throws StateNotFoundException
	{
		Set keys = myMapInterventions.keySet();
		Set nukeys = evidence.keySet();
		Collection drop = new ArrayList( keys.size() );
		Object key = null;
		for( Iterator it = keys.iterator(); it.hasNext(); ){
			if( ! nukeys.contains( key = it.next() ) ){ drop.add( key ); }
		}
		pre( null, null, null, null, nukeys, drop.isEmpty() ? null : drop );

		int count = 0;
		myFlagSuppressPre = true;
		try{
			if( evidence == null ){ evidence = Collections.EMPTY_MAP; }
			try{
				for( Iterator it = keys.iterator(); it.hasNext(); ){
					myRecentEvidenceChangeVariables.add( key = it.next() );
					if( (! evidence.containsKey( key ) ) ||
						( evidence.get( key ) != myMapInterventions.get( key )) ){ ++count; }
				}

				for( Iterator it = nukeys.iterator(); it.hasNext(); ){
					if( ! myMapInterventions.containsKey( it.next() ) ){ ++count; }
				}
			}catch( Exception exception ){
				System.err.println( "warning: EvidenceController.setObservations() caught " + exception );
				count = -1;
				myRecentEvidenceChangeVariables.addAll( keys );
			}
			myMapInterventions.clear();
			intervene( evidence );
		}finally{
			myFlagSuppressPre = false;
		}
		return count;
	}

	/**
	 * TODO: maybe also add setEvidence function that calls both setObservation and setIntervention
	 * @since 20230405
	 * emilydebug may be an unnecessary function
	 */
	public int setEvidence( Map obsEvidence, Map intEvidence ) throws StateNotFoundException
	{
		return setObservations(obsEvidence) + setInterventions(intEvidence); 
	}

	/**
	 * Adds the evidence listed to the set of current evidence. If it is an intervened variable,
	 * intervene on the evidence, else observe. 
	 * @param evidence A mapping from FiniteVariables to the value they take on. 
	 * @return count modified evidence (observations and interventions)
	*/
	public int addEvidence( Map evidence, Set intervenedVars ) throws StateNotFoundException
	{
		Set evidenceVars = evidence.keySet();
		Set observeVars = new HashSet(evidenceVars);
		observeVars.removeAll(intervenedVars);
		pre( null, null, null, observeVars, intervenedVars, null);

		int count = 0; 
		myFlagSuppressPre = true; 
		try{
			checkValidVariables( evidenceVars );
			FiniteVariable var = null; 
			Object value = null; 
			for( Iterator iter = evidenceVars.iterator(); iter.hasNext(); ){
				var = (FiniteVariable) iter.next(); 
				value = evidence.get(var);
				if (value != null) {
					if( intervenedVars.contains( var ) ){
						count += interveneSansCheckSansNotify( var, value );
					} 
					else {
						count += observeSansCheckSansNotify( var, value );
					}
				}
			}
		} finally {
			myFlagSuppressPre = false; 
		}
		
		fireEvidenceChanged( myFlagDoNotify );
		
		return count;
	}


	/** @since 20020607 */
	protected void checkValidVariables( Set varsToCheck )
	{
		if( ! myBeliefNetwork.containsAll( varsToCheck ) ){
			throw new RuntimeException( "Variable not contained in this belief network." );
		}
	}

	/** @return number of changes */
	protected int unobserveSansCheckSansNotify( FiniteVariable var ){
		if( myMapObservations.containsKey( var ) || myMapInterventions.containsKey( var ) ){
			pre( null, null, var, null, null, null );
			myMapObservations.remove( var );
			myMapInterventions.remove( var );
			addRecentEvidenceChange( var );
			EvidenceAssertedProperty.setValue( var, this );
			InterventionAssertedProperty.setValue( var, this );
			return 1;
		}
		return 0;
	}

	/** Sets the state of the variable to unobserved. In otherwords, that
		variable is removed from the set of observations.
		@return number of changes */
	public int unobserve( FiniteVariable var )
	{
		return unobserve( var, myFlagDoNotify );
	}

	/** @since 20020814
		@return number of changes */
	public int unobserveNotifyOnlyPriorityListeners( FiniteVariable var )
	{
		return unobserve( var, false );
	}

	/** @since 20020814
		@return number of changes */
	protected int unobserve( FiniteVariable var, boolean nonPriority )
	{
		if( !myBeliefNetwork.contains( var ) ) throw new RuntimeException( "EvidenceController.unobserve( " + var + " ) not contained in this belief network." );

		int ret = unobserveSansCheckSansNotify( var );
		if( ret > 0 ){ fireEvidenceChanged( nonPriority ); }
		return ret;
	}

	/**
	 * Restores the evidence to the state of no observations.
	 * @return previous size of evidence
	 * @since 20230405
	 */
	public int resetEvidence()
	{
		Collection obsVars = new ArrayList( myMapObservations.keySet() );
		Collection interveneVars = new ArrayList( myMapInterventions.keySet() ); 
		Collection vars = new ArrayList(); 
		vars.addAll(obsVars);
		vars.addAll(interveneVars);
		pre( null, null, null, null, null, vars );
		// reset all observations
		myRecentEvidenceChangeVariables.addAll( vars );
		myMapObservations.clear();
		EvidenceAssertedProperty.setAllValues( obsVars, this );
		myMapInterventions.clear();
		InterventionAssertedProperty.setAllValues( interveneVars, this );
		fireEvidenceChanged( myFlagDoNotify );
		return vars.size();
	}

	/** Returns the set of variable to value instantiations. */
	public Map evidence() {
		HashMap evidence = new HashMap(); 
		evidence.putAll(myMapObservations);
		evidence.putAll(myMapInterventions);
		return Collections.unmodifiableMap(evidence);
	}

	/** @since 20070319 */
	public int size(){
		return myMapObservations.size() + myMapInterventions.size();
	}

	/** @since 20051006 */
	public Set evidenceVariables(){
		HashMap evidence = new HashMap(); 
		evidence.putAll(myMapObservations);
		evidence.putAll(myMapInterventions);
		return Collections.unmodifiableSet( evidence.keySet() );
	}

	/** @since 20230412 */
	public Set intervenedVariables(){
		return Collections.unmodifiableSet( myMapInterventions.keySet() );
	}

	/** @since 20021029 */
	public boolean isEmpty(){
		return myMapObservations.isEmpty() && myMapInterventions.isEmpty();
	}

	public Collection getRecentChanges(){
		return myRecentEvidenceChangeVariables;
	}

	/** 
	 * @since 20021004
		@return number replaced */
	public int replaceVariables( Map mapVariablesOldToNew )
	{
		if( FLAG_DEBUG ){ Definitions.STREAM_VERBOSE.println( "EvidenceController.replaceVariables()" ); }
		Set toReplace = new HashSet();
		Object oldVar = null;
		for( Iterator it = myRecentEvidenceChangeVariables.iterator(); it.hasNext(); ){
			if( mapVariablesOldToNew.containsKey( oldVar = it.next() ) ){ toReplace.add( oldVar ); }
		}
		for( Iterator it = toReplace.iterator(); it.hasNext(); ){
			myRecentEvidenceChangeVariables.remove( oldVar = it.next() );
			myRecentEvidenceChangeVariables.add( mapVariablesOldToNew.get( oldVar ) );
		}
		// toReplace.clear();
		Set toReplaceObs = new HashSet();
		Set toReplaceInt = new HashSet(); 
		for( Iterator it = myMapObservations.keySet().iterator(); it.hasNext(); ){
			if( mapVariablesOldToNew.containsKey( oldVar = it.next() ) ){ toReplaceObs.add( oldVar ); }
		}
		for( Iterator it = myMapInterventions.keySet().iterator(); it.hasNext(); ){
			if( mapVariablesOldToNew.containsKey( oldVar = it.next() ) ){ toReplaceInt.add( oldVar ); }
		}
		for( Iterator it = toReplaceObs.iterator(); it.hasNext(); ){
			oldVar = it.next();
			myMapObservations.put( mapVariablesOldToNew.get( oldVar ), myMapObservations.remove( oldVar ) );
		}
		for( Iterator it = toReplaceInt.iterator(); it.hasNext(); ){
			oldVar = it.next();
			myMapInterventions.put( mapVariablesOldToNew.get( oldVar ), myMapInterventions.remove( oldVar ) );
		}
		return toReplace.size();
	}

	/** @since 20021004 */
	public Object clone(){
		if( FLAG_DEBUG ){ Definitions.STREAM_VERBOSE.println( "EvidenceController.clone()" ); }
		EvidenceController ret = new EvidenceController( myBeliefNetwork );
		ret.myMapObservations.putAll( myMapObservations );
		ret.myEvidenceChangeListeners.addAll( myEvidenceChangeListeners );
		ret.myPriorityEvidenceChangeListeners.addAll( myPriorityEvidenceChangeListeners );
		ret.myRecentEvidenceChangeVariables.addAll( myRecentEvidenceChangeVariables );
		return ret;
	}

	/** @since 20021004 */
	public void setBeliefNetwork( BeliefNetwork bn ){
		myBeliefNetwork = bn;
	}

	/** @since 20081110 */
	public BeliefNetwork getBeliefNetwork(){
		return myBeliefNetwork;
	}

	protected BeliefNetwork myBeliefNetwork = null;
	protected Map myMapObservations = new HashMap();
	protected Map myMapInterventions = new HashMap(); 
	protected WeakLinkedList myEvidenceChangeListeners = new WeakLinkedList();
	protected WeakLinkedList myPriorityEvidenceChangeListeners = new WeakLinkedList();
	protected WeakLinkedList myInterventionChangeListeners = new WeakLinkedList();
	protected Set myRecentEvidenceChangeVariables = new HashSet();

	protected boolean myFlagDoNotify = true;
	private boolean myFlagFrozen = false; 
	private boolean myFlagSuppressPre = false;
}
