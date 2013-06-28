/**
 * 
 */
package edu.jhu.model.dmv;

import edu.jhu.util.Alphabet;
import edu.jhu.data.Label;

public class UniformDmvModelFactory extends AbstractDmvModelFactory implements DmvModelFactory {

    public UniformDmvModelFactory() { }

    @Override
    public DmvModel getInstance(Alphabet<Label> alphabet) {
        DmvModel dmv = new DmvModel(alphabet);
        dmv.fill(0.0);
        dmv.logNormalize();
        return dmv;
    }
    
}