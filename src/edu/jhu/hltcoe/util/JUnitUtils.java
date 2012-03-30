package edu.jhu.hltcoe.util;

import org.junit.Assert;

public class JUnitUtils {

    private JUnitUtils() {
        // private constructor
    }

    public static void assertArrayEquals(double[][] a1, double[][] a2, double delta) {
        assertSameSize(a1, a2);
        for (int i=0; i<a1.length; i++) {
            assertArrayEquals(a1[i], a2[i], delta);
        }
    }
    
    public static void assertArrayEquals(double[] a1, double[] a2, double delta) {
        Assert.assertEquals(a1.length, a2.length);
        for (int i=0; i<a1.length; i++) {
            Assert.assertEquals(a1[i], a2[i], delta);
        }
    }
    
    public static void assertSameSize(double[][] newLogPhi, double[][] logPhi) {
        Assert.assertEquals(newLogPhi.length, logPhi.length);
        for (int k=0; k<logPhi.length; k++) {
            Assert.assertEquals(newLogPhi[k].length, logPhi[k].length); 
        }
    }

    public static void assertArrayEquals(int[] a1, int[] a2) {
        Assert.assertEquals(a1.length, a2.length);
        for (int i=0; i<a1.length; i++) {
            Assert.assertEquals(a1[i], a2[i]);
        }
    }
    
    
}