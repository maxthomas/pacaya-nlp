/**
 * 
 */
package edu.jhu.model.dmv;

import edu.jhu.util.Alphabet;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.Label;

public class SupervisedDmvModelFactory extends AbstractDmvModelFactory implements DmvModelFactory {
    
    private DmvModel dmv;
    
    public SupervisedDmvModelFactory(DepTreebank trainTreebank) {
        DmvMStep mStep = new DmvMStep(0.0);
        dmv = (DmvModel)mStep.getModel(trainTreebank);
    }
    
    @Override
    public DmvModel getInstance(Alphabet<Label> alphabet) {
        assert(alphabet == dmv.getTagAlphabet());
        return dmv;
    }
    
}