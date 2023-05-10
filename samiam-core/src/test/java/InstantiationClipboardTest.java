package edu.ucla.belief.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.*;

import edu.ucla.belief.*;
import edu.ucla.belief.ui.*;
import edu.ucla.belief.ui.clipboard.InstantiationClipBoard;
import edu.ucla.belief.ui.clipboard.InstantiationClipBoardImpl;
import edu.ucla.belief.VariableComparator;

import java.util.*;
import java.io.*;

/**
 * Tests Instantiation Clipboard functionality with EvidenceController ONLY. Does not test
 * listener responses to evidence changes such as network display, evidence tree, and monitor
 * changes. 
 */
public class InstantiationClipboardTest {

    private Integer numNodes = 5;
    private BeliefNetwork bn = RandomNetworks.randomNetwork(numNodes, 0.5);
    private List vertices = bn.topologicalOrder();
    private EvidenceController ec = bn.getEvidenceController();
    private InstantiationClipBoard clipboard = new InstantiationClipBoardImpl(null);
    
    @BeforeEach
    void reset() {
        ec.resetEvidence();
        clipboard.clear();
    }

    @DisplayName("Test copy + paste empty evidence")
    @Test
    void copyEmptyEvidenceTest() {
        clipboard.copy(ec.evidence(), ec.intervenedVariables());
        clipboard.paste(bn);
        assertEquals(0, ec.size());
    }

    @DisplayName("Test copy + paste observed evidence on unobserved evidence")
    @Test
    void copyObsEvidenceUnobsTest() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        Object obs1Val = obs1.instance(0);

        try {
            ec.observe(obs1, obs1Val);
            assertEquals(true, ec.isObservation(obs1));
            clipboard.copy(ec.evidence(), ec.intervenedVariables());
            ec.resetEvidence();
            assertEquals(false, ec.isObservation(obs1));
            clipboard.paste(bn);
            assertEquals(true, ec.isObservation(obs1));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.observe() " + e);
        }
    }

    @DisplayName("Test copy + paste observed evidence on intervened evidence")
    @Test
    void copyObsEvidenceIntTest() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        Object obs1Val = obs1.instance(0);

        try {
            ec.intervene(obs1, obs1Val);
            assertEquals(true, ec.isIntervention(obs1));
            clipboard.copy(ec.evidence(), ec.intervenedVariables());
            ec.resetEvidence();
            assertEquals(false, ec.isIntervention(obs1));
            clipboard.paste(bn);
            assertEquals(true, ec.isIntervention(obs1));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.intervene() " + e);
        }
    }

    @DisplayName("Test copy + paste on multiple types of evidence")
    @Test
    void copyMultEvidenceTest() {
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
            ec.observe(obs1, obs1Val1);
            ec.intervene(obs2, obs2Val);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(true, ec.isIntervention(obs2));
            clipboard.copy(ec.evidence(), ec.intervenedVariables());
            ec.resetEvidence();
            ec.intervene(obs1, obs1Val2);
            ec.observe(obs2, obs2Val);
            clipboard.paste(bn);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.observe() or ec.intervene()" + e);
        }
    }

    @DisplayName("Test cut + paste empty evidence")
    @Test
    void cutEmptyEvidence() {
        clipboard.cut(ec.evidence(), ec.intervenedVariables(), ec);
        assertEquals(0, ec.size());
        clipboard.paste(bn);
        assertEquals(0, ec.size());
    }

    @DisplayName("Test cut + paste observed evidence")
    @Test
    void cutObsEvidence() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        Object obs1Val = obs1.instance(0);

        try {
            ec.observe(obs1, obs1Val);
            assertEquals(true, ec.isObservation(obs1));
            clipboard.cut(ec.evidence(), ec.intervenedVariables(), ec);
            assertEquals(0, ec.size());
            clipboard.paste(bn);
            assertEquals(true, ec.isObservation(obs1));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.observe() " + e);
        }
    }

    @DisplayName("Test cut + paste intervened evidence")
    @Test
    void cutIntEvidence() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        Object obs1Val = obs1.instance(0);

        try {
            ec.intervene(obs1, obs1Val);
            assertEquals(true, ec.isIntervention(obs1));
            clipboard.cut(ec.evidence(), ec.intervenedVariables(), ec);
            assertEquals(0, ec.size());
            clipboard.paste(bn);
            assertEquals(true, ec.isIntervention(obs1));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.intervene() " + e);
        }
    }

    @DisplayName("Test cut + paste multiple types of evidence")
    @Test
    void cutMultEvidence() {
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
            ec.observe(obs1, obs1Val1);
            ec.intervene(obs2, obs2Val);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(true, ec.isIntervention(obs2));
            clipboard.cut(ec.evidence(), ec.intervenedVariables(), ec);
            assertEquals(0, ec.size());
            ec.intervene(obs1, obs1Val2);
            ec.observe(obs2, obs2Val);
            clipboard.paste(bn);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
        } catch (StateNotFoundException e) {
            System.err.println("In ec.observe() or ec.intervene()" + e);
        }
    }

    @DisplayName("Test save and load functionality")
    @Test
    void saveLoadEvidence() {
        Random random = new Random();
        Integer index1 = random.nextInt(numNodes);
        Integer index2 = random.nextInt(numNodes);
        while (index1 == index2) index2 = random.nextInt(numNodes);

        FiniteVariable obs1 = (FiniteVariable) vertices.get(index1); 
        FiniteVariable obs2 = (FiniteVariable) vertices.get(index2); 
        Object obs1Val1 = obs1.instance(0);
        Object obs1Val2 = obs1.instance(1);
        Object obs2Val = obs2.instance(1);

        File file = new File("test.txt");

        try {
            // save network, cut clears evidence
            ec.observe(obs1, obs1Val1);
            ec.intervene(obs2, obs2Val);
            clipboard.cut(ec.evidence(), ec.intervenedVariables(), ec);
            assertEquals(0, ec.size());
            clipboard.save(file);
            ec.intervene(obs1, obs1Val2);
            ec.observe(obs2, obs2Val);
            clipboard.load(file);
            clipboard.paste(bn);
            assertEquals(true, ec.isObservation(obs1));
            assertEquals(obs1Val1, ec.getValue(obs1));
            assertEquals(true, ec.isIntervention(obs2));
            assertEquals(obs2Val, ec.getValue(obs2));
            file.delete();
        } catch (StateNotFoundException e) {
            System.err.println("In ec.observe() or ec.intervene()" + e);
        } catch (Exception e) {
            System.err.println("In InstantiationClipBoard.load() " + e);
        }
    }
}
