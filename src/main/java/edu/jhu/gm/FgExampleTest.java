package edu.jhu.gm;

import static edu.jhu.util.Utilities.getList;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.gm.Var.VarType;
import edu.jhu.srl.SrlFgExamplesBuilder;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFgExampleBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.Alphabet;

public class FgExampleTest {

    @Test
    public void testClampedFactorGraphs() {
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        //tokens.add(new CoNLL09Token(1, "the", "_", "_", "Det", "_", getList("feat"), getList("feat") , 2, 2, "det", "_", false, "_", new ArrayList<String>()));
        //tokens.add(new CoNLL09Token(id, form, lemma, plemma, pos, ppos, feat, pfeat, head, phead, deprel, pdeprel, fillpred, pred, apreds));
        tokens.add(new CoNLL09Token(1, "the", "_", "_", "Det", "_", getList("feat"), getList("feat") , 2, 2, "det", "_", false, "_", getList("_")));
        tokens.add(new CoNLL09Token(2, "dog", "_", "_", "N", "_", getList("feat"), getList("feat") , 2, 2, "subj", "_", false, "_", getList("arg0")));
        tokens.add(new CoNLL09Token(3, "ate", "_", "_", "V", "_", getList("feat"), getList("feat") , 2, 2, "v", "_", true, "ate.1", getList("_")));
        //tokens.add(new CoNLL09Token(4, "food", "_", "_", "N", "_", getList("feat"), getList("feat") , 2, 2, "obj", "_", false, "_", getList("arg1")));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        
        List<CoNLL09Sentence> sents = getList(sent);
        
        System.out.println("Done reading.");
        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.useProjDepTreeFactor = true;
        prm.fgPrm.linkVarType = VarType.LATENT;

        prm.fePrm.biasOnly = true;
        
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, alphabet);
        FgExamples data = builder.getData(sents);
        
        FgExample ex = data.get(0);
        
        // Global factor should still be there.
        assertEquals(1 + 3 + 3*2 + 2 + 2, ex.getFgLatPred().getFactors().size());
        assertEquals(1 + 3 + 3*2 + 2 + 2, ex.getFgLat().getFactors().size());

        assertEquals(0, getEmpty(ex.getFgLatPred().getFactors()).size());
        // Just the two Role unary factors. The link/role binary factors shouldn't be empty.
        assertEquals(2, getEmpty(ex.getFgLat().getFactors()).size());
    }

    private List<Factor> getEmpty(List<Factor> factors) {
        ArrayList<Factor> filt = new ArrayList<Factor>();
        for (Factor f : factors) {
            if (f.getVars().size() == 0) {
                filt.add(f);
            }
        }
        return filt;
    }
    
    
        
}