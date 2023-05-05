package edu.ucla.belief;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.*;

import edu.ucla.belief.EvidenceController;
import edu.ucla.belief.RandomNetworks;

import java.util.*;
import java.io.*;

public class EvidenceControllerTest {

    // create a random network with 5 boolean vertices
    private Integer numNodes = 5;
    private BeliefNetwork bn = RandomNetworks.randomNetwork(numNodes, 0.5);
    private List vertices = bn.topologicalOrder();
    private EvidenceController ec = new EvidenceController(bn);

    /*
     * Before each test, reset evidence and verify that all evidence has been reset. 
     */
    @BeforeEach
    public void reset() {
        ec.resetEvidence();
        for (int i = 0; i < numNodes; ++i ) {
            assertEquals(false, ec.isObservation((FiniteVariable) vertices.get(i)));
            assertEquals(false, ec.isIntervention((FiniteVariable) vertices.get(i)));
        }
    }

    /*
     * Test observe evidence functionality. Verify that correct variables are observed. 
     * With randomness and repeating the test, it tests the following scenarios: observing two 
     * different variable/value pairs and observing the same variable/value pair. 
     */
    @DisplayName("Test observations")
    @RepeatedTest(5)
    void observeTest() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index1 == index2) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs1Val2 = obs1.instance(1);
        Object obs2Val = obs2.instance(1);

        try {
            // verify observe on variable/value pair
            ec.observe(obs1, obs1Val1); 
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(false, ec.isIntervention(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            // verify observe on same variable, different value
            ec.observe(obs1, obs1Val2); 
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(false, ec.isIntervention(obs1));
            assertEquals(obs1Val2, ec.getValue(obs1));
            // verify observe on different variable/value pair
            ec.observe(obs2, obs2Val);
            assertEquals(true, ec.isObservation(obs2));
            assertEquals(false, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.observe() " + e);
        }

        for (int i = 0; i < numNodes; ++i) {
            if (i != index1 && i != index2) {
                assertEquals(false, ec.isObservation((FiniteVariable) vertices.get(i)));
                assertEquals(false, ec.isIntervention((FiniteVariable) vertices.get(i)));
            }
        }
    }
    
    /*
     * Test setObservations()
     */
    @DisplayName("Test set observations on no previously asserted evidence")
    @RepeatedTest(5)
    void setObsTest_noEvidence() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index2 == index1) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);

        try {
            // test no asserted evidence 
            ec.setObservations(evidence);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isObservation(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.setObservations() " + e);
        }
    }

    @DisplayName("Test set observations on observed diff var/val")
    @RepeatedTest(5)
    void setObsTest_diffObsVarVal() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index2 == index1) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs1Val2 = obs1.instance(1);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);

        try {
            // test on asserting on different observed var/val pair
            ec.observe(obs1, obs1Val2);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(obs1Val2, ec.getValue(obs1));
            ec.setObservations(evidence);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isObservation(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.setObservations() " + e);
        }
    }

    @DisplayName("Test set observations on intervened diff var/val")
    @RepeatedTest(5)
    void setObsTest_diffIntVarVal() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index2 == index1) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs1Val2 = obs1.instance(1);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);

        try {
            // test on asserting on different intervened var/val pair
            ec.intervene(obs1, obs1Val2);
            assertEquals(false, ec.isObservation(obs1));
            assertEquals(true, ec.isIntervention(obs1));
            ec.setObservations(evidence);
            assertEquals(false, ec.isIntervention(obs1));
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isObservation(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.setObservations() " + e);
        }
    }

    /*
    * Test intervene evidence functionality. Verify that the correct variables are intervened.
    * Assumes observation functionality works.
    */
    @DisplayName("Test interventions")
    @RepeatedTest(5)
    void interveneTest() {
        Random random = new Random(); 
        Integer index1 = random.nextInt(numNodes); 
        Integer index2 = random.nextInt(numNodes);
        while (index1 == index2) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs1Val2 = obs1.instance(1);
        Object obs2Val = obs2.instance(1);

        try {
            // verify intervening on previously observed value
            ec.observe(obs1, obs1Val1);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(false, ec.isIntervention(obs1));
            ec.intervene(obs1, obs1Val1); 
            assertEquals(false, ec.isObservation(obs1));
            assertEquals(true, ec.isIntervention(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            // verify intervening on different value of same variable
            // replicates instantiation clipboard pasting functionality
            // so it doesn't require value to be observed first
            ec.intervene(obs1, obs1Val2); 
            assertEquals(false, ec.isObservation(obs1));
            assertEquals(true, ec.isIntervention(obs1));
            assertEquals(obs1Val2, ec.getValue(obs1));
            // verify intervening on a different variable/value pair
            ec.intervene(obs2, obs2Val);
            assertEquals(false, ec.isObservation(obs2));
            assertEquals(true, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.intervene() " + e);
        }

        for (int i = 0; i < numNodes; ++i) {
            if (i != index1 && i != index2) {
                assertEquals(false, ec.isObservation((FiniteVariable) vertices.get(i)));
                assertEquals(false, ec.isIntervention((FiniteVariable) vertices.get(i)));
            }
        }
    }

    /*
     * Test setInterventions()
     */
    @DisplayName("Test set interventions on no previously asserted evidence")
    @RepeatedTest(5)
    void setIntTest_noEvidence() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index2 == index1) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);

        try {
            // test no asserted evidence 
            ec.setInterventions(evidence);
            assertEquals(true, ec.isIntervention(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.setInterventions() " + e);
        }
    }

    @DisplayName("Test set interventions on intervened diff var/val")
    @RepeatedTest(5)
    void setIntTest_diffIntVarVal() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index2 == index1) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs1Val2 = obs1.instance(1);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);

        try {
            // test on asserting on different intervened var/val pair
            ec.intervene(obs1, obs1Val2);
            assertEquals(true, ec.isIntervention(obs1));
            assertEquals(obs1Val2, ec.getValue(obs1));
            ec.setInterventions(evidence);
            assertEquals(true, ec.isIntervention(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.setInterventions() " + e);
        }
    }

    @DisplayName("Test set interventions on observed diff var/val")
    @RepeatedTest(5)
    void setIntTest() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index2 == index1) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs1Val2 = obs1.instance(1);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);

        try {
            // test on asserting on different observed var/val pair
            ec.observe(obs1, obs1Val2);
            assertEquals(false, ec.isIntervention(obs1));
            assertEquals(true, ec.isObservation(obs1));
            ec.setInterventions(evidence);
            assertEquals(false, ec.isObservation(obs1));
            assertEquals(true, ec.isIntervention(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.setInterventions() " + e);
        }
    }

    /*
     * Test addEvidence() used by paste functionality of Instantiation Clipboard
     */
    @DisplayName("Test add observed evidence with no previously asserted evidence")
    @RepeatedTest(5)
    void addEvidenceTest_ObsNoEvidence() {
        Random random = new Random(); 
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index1 == index2) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);
        Set intervenedVars = new HashSet<>();

        try {
            // add observed evidence with no previously asserted evidence
            ec.addEvidence(evidence, intervenedVars);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isObservation(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.addEvidence() " + e);
        }
    }

    @DisplayName("Test add observed and intervened evidence with no previously asserted evidence")
    @RepeatedTest(5)
    void addEvidenceTest_ObsIntNoEvidence() {
        Random random = new Random(); 
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index1 == index2) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);
        Set intervenedVars = new HashSet<>();

        try {
            // add observed and intervened evidence with no previously asserted evidence
            intervenedVars.add(obs1);
            ec.addEvidence(evidence, intervenedVars);
            assertEquals(true, ec.isIntervention(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isObservation(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.addEvidence() " + e);
        }
    }

    @DisplayName("Test add intervened evidence with no previously asserted evidence")
    @RepeatedTest(5)
    void addEvidenceTest_IntNoEvidence() {
        Random random = new Random(); 
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index1 == index2) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);
        Set intervenedVars = new HashSet<>();

        try {
            // add intervened evidence with no previously asserted evidence
            intervenedVars.add(obs1);
            intervenedVars.add(obs2);
            ec.addEvidence(evidence, intervenedVars);
            assertEquals(true, ec.isIntervention(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.addEvidence() " + e);
        }
    }

    @DisplayName("Test add observed vidence with previously intervened evidence")
    @RepeatedTest(5)
    void addEvidenceTest_intEvidence() {
        Random random = new Random(); 
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index1 == index2) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs1Val2 = obs1.instance(1);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);
        Set intervenedVars = new HashSet<>();

        try {
            // add observed evidence on previously intervened evidence
            ec.intervene(obs1, obs1Val2);
            ec.intervene(obs2, obs2Val);
            assertEquals(false, ec.isObservation(obs1));
            assertEquals(obs1Val2, ec.getValue(obs1));
            assertEquals(false, ec.isObservation(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
            ec.addEvidence(evidence, intervenedVars);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isObservation(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.addEvidence() " + e);
        }
    }

    @DisplayName("Test add intervened vidence with previously observed evidence")
    @RepeatedTest(5)
    void addEvidenceTest_obsEvidence() {
        Random random = new Random(); 
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index1 == index2) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs1Val2 = obs1.instance(1);
        Object obs2Val = obs2.instance(1);

        Map evidence = new HashMap<>();
        evidence.put(obs1, obs1Val1);
        evidence.put(obs2, obs2Val);
        Set intervenedVars = new HashSet<>();

        try {
            // add intervened evidence on previously observed evidence
            intervenedVars.add(obs1);
            intervenedVars.add(obs2);
            ec.observe(obs1, obs1Val2);
            ec.observe(obs2, obs2Val);
            assertEquals(false, ec.isIntervention(obs1));
            assertEquals(obs1Val2, ec.getValue(obs1));
            assertEquals(false, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
            ec.addEvidence(evidence, intervenedVars);
            assertEquals(true, ec.isIntervention(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.addEvidence() " + e);
        }
    }

    /*
     * Test unobserve functionality. 
     */
    @DisplayName("Test unobserve on observed var")
    @RepeatedTest(5)
    void unobserveObserveTest() {
        Random random = new Random(); 
        Integer index1 = random.nextInt(numNodes);
        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        Object obs1Val = obs1.instance(0);

        try {
            // test unobserve on observed variable 
            ec.observe(obs1, obs1Val);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(obs1Val, ec.getValue(obs1)); 
            ec.unobserve(obs1); 
            assertEquals(false, ec.isObservation(obs1)); 
            assertEquals(false, ec.isIntervention(obs1));
            assertEquals(null, ec.getValue(obs1));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.unobserve() " + e);
        }
    }

    @DisplayName("Test unobserve on intervened var")
    @RepeatedTest(5)
    void unobserveInterveneTest() {
        Random random = new Random(); 
        Integer index1 = random.nextInt(numNodes);
        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        Object obs1Val = obs1.instance(1);

        try {
            // test unobserve on intervened variable
            ec.intervene(obs1, obs1Val);
            assertEquals(true, ec.isIntervention(obs1));
            assertEquals(obs1Val, ec.getValue(obs1));
            ec.unobserve(obs1);
            assertEquals(false, ec.isObservation(obs1)); 
            assertEquals(false, ec.isIntervention(obs1));
            assertEquals(null, ec.getValue(obs1));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.unobserve() " + e);
        }
    }

    /*
     * Verify that evidence can not be changed when frozen.
     */
    @DisplayName("Test asserting evidence when frozen")
    @Test
    void frozenTestAssertEvidence() {
        Random random = new Random(); 
        Integer index1 = random.nextInt(numNodes);
        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        Object obs1Val1 = obs1.instance(0);

        // attempt to assert evidence when frozen
        ec.setFrozen(true);
        assertThrows(IllegalStateException.class, () -> {
            ec.observe(obs1, obs1Val1);
        });
        assertThrows(IllegalStateException.class, () -> {
            ec.intervene(obs1, obs1Val1);
        });
    }

    @DisplayName("Test unasserting evidence when frozen")
    @Test
    void frozenTestUnassertEvidence() {
        Random random = new Random(); 
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index1 == index2) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs1Val2 = obs1.instance(1);
        Object obs2Val = obs2.instance(1);

        try {
            // attempt to assert and unassert evidence when frozen
            ec.observe(obs1, obs1Val1);
            ec.intervene(obs2, obs2Val);
            ec.setFrozen(true);
            assertThrows(IllegalStateException.class, () -> {
                ec.observe(obs1, obs1Val2);
            });
            assertThrows(IllegalStateException.class, () -> {
                ec.intervene(obs1, obs1Val2);
            });
            assertThrows(IllegalStateException.class, () -> {
                ec.unobserve(obs1);
            });
            assertThrows(IllegalStateException.class, () -> {
                ec.unobserve(obs2);
            });
        } catch (StateNotFoundException e) {
            System.err.println("In evidence controller " + e);
        }
    }
}
