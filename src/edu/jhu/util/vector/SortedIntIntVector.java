package edu.jhu.util.vector;

import edu.jhu.util.Utilities;


/**
 * Infinite length sparse vector.
 * 
 * @author mgormley
 *
 */
public class SortedIntIntVector extends SortedIntIntMap {

    boolean norm2Cached = false;
    double norm2Value;
    
    public SortedIntIntVector() {
        super();
    }
    
    public SortedIntIntVector(int[] index, int[] data) {
    	super(index, data);
	}
    
    public SortedIntIntVector(SortedIntIntVector vector) {
    	super(vector);
    }

	public SortedIntIntVector(int[] denseRow) {
		this(Utilities.getIndexArray(denseRow.length), denseRow);
	}

	// TODO: This could be done with a single binary search instead of two.
    public void add(int idx, int val) {
    	int curVal = getWithDefault(idx, 0);
    	put(idx, curVal + val);
    }
    
    public void set(int idx, int val) {
    	put(idx, val);
    }
    
    @Override
	public int get(int idx) {
		return getWithDefault(idx, 0);
	}
    
    public void scale(double multiplier) {
    	for (int i=0; i<used; i++) {
    		values[i] *= multiplier;
    	}
    }

    public void add(SortedIntIntVector other) {
        // TODO: this could be done much faster with a merge of the two arrays.
        for (IntIntEntry ve : other) {
            add(ve.index(), ve.get());
        }
    }
    
	public void set(SortedIntIntVector other) {
		// TODO: this could be done much faster with a merge of the two arrays.
		for (IntIntEntry ve : other) {
			set(ve.index(), ve.get());
		}
	}

    public int dot(SortedIntIntVector y) {
        if (y instanceof SortedIntIntVector) {
            SortedIntIntVector other = ((SortedIntIntVector) y);
            int ret = 0;
            int oc = 0;
            for (int c = 0; c < used; c++) {
                while (oc < other.used) {
                    if (other.indices[oc] < indices[c]) {
                        oc++;
                    } else if (indices[c] == other.indices[oc]) {
                        ret += values[c] * other.values[oc];
                        break;
                    } else {
                        break;
                    }
                }
            }
            return ret;
        } else {
        	throw new IllegalArgumentException("Unhandled type: " + y.getClass());
        }
    }
    

    /**
     * @return A new vector without zeros OR the same vector if it has none.
     */
    public static SortedIntIntVector getWithNoZeroValues(SortedIntIntVector row) {
        int[] origIndex = row.getIndices();
        int[] origData = row.getValues();
        
        // Count and keep track of nonzeros.
        int numNonZeros = 0;
        boolean[] isNonZero = new boolean[row.getUsed()];
        for (int i = 0; i < row.getUsed(); i++) {
            if (origData[i] != 0) {
                isNonZero[i] = true;
                numNonZeros++;
            } else {
                isNonZero[i] = false;
            }
        }
        int numZeros = row.getUsed() - numNonZeros;
        
        if (numZeros > 0) {
            // Create the new vector without the zeros.
            int[] newIndex = new int[numNonZeros];
            int[] newData = new int[numNonZeros];

            int newIdx = 0;
            for (int i = 0; i < row.getUsed(); i++) {
                if (isNonZero[i]) {
                    newIndex[newIdx] = origIndex[i];
                    newData[newIdx] = origData[i];
                    newIdx++;
                }
            }
            return new SortedIntIntVector(newIndex, newData);
        } else {
            return row;
        }
    }
    

    /**
     * TODO: Make a SortedIntLongVectorWithExplicitZeros class and move this method there.
     * 
     * Here we override the zero method so that it doesn't set the number of
     * used values to 0. This ensures that we keep explicit zeros in.
     */
    public SortedIntIntVector zero() {
        java.util.Arrays.fill(values, 0);
        //used = 0;
        return this;
    }

    /**
     * Computes the Hadamard product (or entry-wise product) of this vector with
     * another.
     */
    public SortedIntIntVector hadamardProd(SortedIntIntVector other) {
    	SortedIntIntVector ip = new SortedIntIntVector();
        int oc = 0;
        for (int c = 0; c < used; c++) {
            while (oc < other.used) {
                if (other.indices[oc] < indices[c]) {
                    oc++;
                } else if (indices[c] == other.indices[oc]) {
                    ip.set(indices[c], values[c] * other.values[oc]);
                    break;
                } else {
                    break;
                }
            }
        }
        return ip;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < used; i++) {
            sb.append(indices[i]);
            sb.append(":");
            sb.append(values[i]);
            if (i + 1 < used) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Returns true if the input vector is equal to this one.
     */
    @Override
    public boolean equals(Object obj) {
    	if (obj instanceof SortedIntIntVector) {
    		SortedIntIntVector other = (SortedIntIntVector) obj;

            SortedIntIntVector v1 = SortedIntIntVector.getWithNoZeroValues(this);
            SortedIntIntVector v2 = SortedIntIntVector.getWithNoZeroValues(other);
            
	        if (v2.size() != v1.size()) {
	            return false;
	        }
	        // This is slow, but correct.
	        for (IntIntEntry ve : v1) {
	            if (ve.get() != v2.get(ve.index())) {
	                return false;
	            }
	        }
	        for (IntIntEntry ve : v2) {
	            if (ve.get() != v1.get(ve.index())) {
	                return false;
	            }
	        }
	        return true;
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	throw new RuntimeException("not implemented");
    }

}