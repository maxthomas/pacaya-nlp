package edu.jhu.nlp.eval;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.CorpusHandler;
import edu.jhu.nlp.features.TemplateLanguage.AT;

public class ProportionAnnotated implements Evaluator {

    private static final Logger log = Logger.getLogger(ProportionAnnotated.class);

    @Override
    public double evaluate(AnnoSentenceCollection predSents, AnnoSentenceCollection goldSents, String name) {
        List<AT> ats = CorpusHandler.getPredAts();
        int[] counts = new int[AT.values().length];
        for (AnnoSentence sent : predSents) {
            for (AT at : ats) {
                if (sent.hasAt(at)) {
                    counts[at.ordinal()]++;
                }
            }
        }
        for (AT at : ats) {
            log.info(String.format("Proportion %s: %d / %d = %g", at.name(), counts[at.ordinal()], predSents.size(), 
                    (double) counts[at.ordinal()] / predSents.size()));
        }
        return 0.0;
    }

}
