package edu.jhu.gm.inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.globalfac.GlobalFactor;

public class NoGlobalFactorsMpSchedule implements MpSchedule {

    private ArrayList<Object> order;
    
    public NoGlobalFactorsMpSchedule(FactorGraph fg) {
        order = new ArrayList<Object>(fg.getEdges());
    }
    
    @Override
    public List<Object> getOrder() {
        return order;
    }

}
