package edu.ucla.belief.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.*;

import edu.ucla.belief.BeliefNetwork;
import edu.ucla.belief.BeliefNetworkImpl;
import edu.ucla.belief.EvidenceController;
import edu.ucla.belief.FiniteVariable;
import edu.ucla.belief.FiniteVariableImpl;
import edu.ucla.belief.TableShell;
import il2.model.Table;

public class BeliefNetworkTest {
    private static BeliefNetwork bn = new BeliefNetworkImpl( true );
    private static EvidenceController ec = bn.getEvidenceController();
    private static Map<String,FiniteVariable> vars = new HashMap<String,FiniteVariable>();

    @BeforeAll
    static void init() {
        String[] vals = {"T", "F"};

        // initialize variables
        vars.put("Z", new FiniteVariableImpl("Z", vals));
        vars.put("V", new FiniteVariableImpl("V", vals));
        vars.put("W", new FiniteVariableImpl("W", vals));
        vars.put("X", new FiniteVariableImpl("X", vals));
        vars.put("Y", new FiniteVariableImpl("Y", vals));
        vars.put("S", new FiniteVariableImpl("S", vals));

        // add variables 
        bn.addVariable(vars.get("Z"), true);
        bn.addVariable(vars.get("V"), true);
        bn.addVariable(vars.get("W"), true);
        bn.addVariable(vars.get("X"), true);
        bn.addVariable(vars.get("Y"), true);
        bn.addVariable(vars.get("S"), true);

        // add edges
        bn.addEdge(vars.get("Z"), vars.get("V"), true);
        bn.addEdge(vars.get("Z"), vars.get("X"), true);
        bn.addEdge(vars.get("Z"), vars.get("W"), true);
        bn.addEdge(vars.get("V"), vars.get("W"), true);
        bn.addEdge(vars.get("V"), vars.get("Y"), true);
        bn.addEdge(vars.get("X"), vars.get("Y"), true);
        bn.addEdge(vars.get("X"), vars.get("S"), true);

        // initialize CPTs
        // double[] data1 = new double[]{0.4, 0.6}; 
        // Table table1 = new Table(new FiniteVariable[]{vars.get("Z"), data1);
        // vars.get("Z").setCPTShell(new TableShell(table1));

        // double[] data2 = new double[]{0.2, 0.8, 0.7, 0.3};
        // Table table2 = new Table(new FiniteVariable[]{vars.get("V"), vars.get("Z")}, data2);
        // vars.get("V").setCPTShell(new TableShell(table2));

        // double[] data3 = new double[]{0.55, 0.45, 0.5, 0.5, 0.65, 0.35, 0.2, 0.8};
        // Table table3 = new Table(new FiniteVariable[]{vars.get("W"), vars.get("Z"), vars.get("V")}, data3);
        // vars.get("W").setCPTShell(new TableShell(table3));

        // double[] data4 = new double[]{0.3, 0.7, 0.6, 0.4}; 
        // Table table4 = new Table(new FiniteVariable[]{vars.get("X"), vars.get("Z")}, data4);
        // vars.get("X").setCPTShell(new TableShell(table4));

        // double[] data5 = new double[]{0.1, 0.9, 0.6, 0.4, 0.85, 0.15, 0.8, 0.2};
        // Table table5 = new Table(new FiniteVariable[]{vars.get("Y"), vars.get("V"), vars.get("X")}, data5);
        // vars.get("Y").setCPTShell(new TableShell(table5));

        // double[] data6 = new double[]{0.9, 0.1, 0.25, 0.75};
        // Table table6 = new Table(new FiniteVariable[]{vars.get("S"), vars.get("X")}, data6);
        // vars.get("S").setCPTShell(new TableShell(table6));
    }

    /*
     * Tests that intervening an edge correctly removes edge from the underlying belief
     * network and unintervening adds the correct edge back. 
     */
    @Test
    void testInterveneEdge() {
        // test intervene and unintervene existing edge 
        assertEquals(true, bn.interveneEdge(vars.get("X"), vars.get("Y")));
        assertEquals(6, bn.numEdges());
        assertEquals(false, bn.containsEdge(vars.get("X"), vars.get("Y")));
        assertEquals(1, bn.inComing(vars.get("Y")).size());
        assertEquals(1, bn.getIntervenedEdges().size());
        assertEquals(true, bn.uninterveneEdge(vars.get("X"), vars.get("Y")));
        assertEquals(7, bn.numEdges());
        assertEquals(true, bn.containsEdge(vars.get("X"), vars.get("Y")));
        assertEquals(2, bn.inComing(vars.get("Y")).size());
        assertEquals(0, bn.getIntervenedEdges().size());

        // test intervene and unintervene unexisting edge
        assertThrows(IllegalArgumentException.class, () -> {
            bn.interveneEdge(vars.get("W"), vars.get("Y"));
        });
        assertEquals(7, bn.numEdges());
        assertEquals(0, bn.getIntervenedEdges().size());
        assertEquals(false, bn.uninterveneEdge(vars.get("W"), vars.get("Y")));
        assertEquals(7, bn.numEdges());
        assertEquals(0, bn.getIntervenedEdges().size());
    }

}
