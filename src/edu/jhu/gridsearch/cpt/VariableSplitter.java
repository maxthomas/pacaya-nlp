package edu.jhu.gridsearch.cpt;

import java.util.List;


public interface VariableSplitter {

    List<CptBoundsDeltaList> split(CptBounds bounds, VariableId varId);

}