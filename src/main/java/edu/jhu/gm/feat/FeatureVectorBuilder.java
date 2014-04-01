package edu.jhu.gm.feat;

import edu.jhu.prim.map.IntDoubleHashMap;
import edu.jhu.prim.sort.IntDoubleSort;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.Lambda.FnIntDoubleToDouble;

/**
 * For building large (e.g. 20000+) sparse feature vectors quickly.
 * @author mgormley
 */
public class FeatureVectorBuilder extends IntDoubleHashMap {

    private static final long serialVersionUID = 1L;

    public FeatureVectorBuilder() {
        super(0.0);
    }
    
    public void add(int index, double value) {
        super.put(index, this.get(index) + value);
    }
    
    public void add(FeatureVector fv) {
        final FeatureVectorBuilder thisFvb = this;
        fv.apply(new FnIntDoubleToDouble() {
            public double call(int idx, double val) {
                thisFvb.add(idx, val);
                return val;
            }
        });
    }
    
    public FeatureVector toFeatureVector() {
        Pair<int[], double[]> ivs = this.getIndicesAndValues();
        int[] index = ivs.get1();
        double[] values = ivs.get2();
        IntDoubleSort.sortIndexAsc(index, values);
        return new FeatureVector(index, values);
    }
    
}
