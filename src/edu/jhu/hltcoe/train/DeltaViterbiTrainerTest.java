package edu.jhu.hltcoe.train;

import junit.framework.Assert;

import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.DmvMStep;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.parse.DeltaGenerator;
import edu.jhu.hltcoe.parse.FixedIntervalDeltaGenerator;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpViterbiParserTest;
import edu.jhu.hltcoe.parse.IlpViterbiParserWithDeltas;
import edu.jhu.hltcoe.parse.IlpViterbiSentenceParser;
import edu.jhu.hltcoe.parse.ViterbiParser;

public class DeltaViterbiTrainerTest {

    @Test
    public void testConvergence() {
        double lambda = 0.1;
        int iterations = 25;
        double convergenceRatio = 0.99999;

        IlpSolverFactory ilpSolverFactory = new IlpSolverFactory(IlpSolverId.CPLEX, 1, 128);
        ViterbiParser fastParser = new IlpViterbiSentenceParser(IlpFormulation.FLOW_NONPROJ, ilpSolverFactory);
        DeltaGenerator deltaGen = new FixedIntervalDeltaGenerator(0.1, 3);
        ViterbiParser deltaParser = new IlpViterbiParserWithDeltas(IlpFormulation.FLOW_NONPROJ, ilpSolverFactory,
                deltaGen);
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        DeltaViterbiTrainer trainer = new DeltaViterbiTrainer(deltaParser, fastParser, mStep, modelFactory, iterations,
                convergenceRatio);

        SentenceCollection sentences = new SentenceCollection();
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the man ate the pizza with a fork"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the man ate the pizza"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the man ate with a fork the pizza"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the man ate with a fork"));
        trainer.train(sentences);

        Assert.assertEquals(3, trainer.getIterationsCompleted());
    }

}
