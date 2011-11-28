package edu.jhu.hltcoe.train;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Time;
import edu.jhu.hltcoe.util.Utilities;

/**
 * 
 * @author mgormley
 *
 * @param <C> The type of expected counts
 */
public class EMTrainer<C> implements Trainer {

    private Logger log = Logger.getLogger(EMTrainer.class);

    private EStep<C> eStep;
    private MStep<C> mStep;
    private ModelFactory modelFactory;
    private int iterations;
    private double convergenceRatio;
    private Model model;
    private int iterCount;
    private Double logLikelihood;
    
    public EMTrainer(EStep<C> eStep, MStep<C> mStep, ModelFactory modelFactory, int iterations, double convergenceRatio) {
        this.eStep = eStep;
        this.mStep = mStep;
        this.modelFactory = modelFactory;
        this.iterations = iterations;
        this.convergenceRatio = convergenceRatio;
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        // Initialize the parameters of the model
        model = modelFactory.getInstance(sentences);
        
        // Run iterations of EM
        iterCount = 0;
        Stopwatch iterTimer = new Stopwatch();
        double prevLogLikelihood = Double.NEGATIVE_INFINITY;

        while (true) {
            iterTimer.start();
            log.info("iteration = " + getIterationsCompleted());
            
            // E-step 
            Pair<C,Double> pair = eStep.getCountsAndLogLikelihood(sentences, model);
            C counts = pair.get1();
            
            // Check for convergence or iteration limit
            logLikelihood = pair.get2();
            log.info("logLikelihood = " + logLikelihood);
            log.info("likelihood ratio = " + Utilities.exp(prevLogLikelihood - logLikelihood));
            if (prevLogLikelihood - logLikelihood > Utilities.log(convergenceRatio)) {
                log.info("Stopping training due to convergence");
                break;
            } else if ( iterCount >= iterations) {
                log.info("Stopping training at max iterations");
                break;
            }
            prevLogLikelihood = logLikelihood;
            
            // M-step
            model = mStep.getModel(counts);
            
            log.debug("Time remaining: " + Time.durAsStr(Time.avgMs(iterTimer)*(iterations - iterCount)));
            iterTimer.stop();
            iterCount++;
        }
    }

    public int getIterationsCompleted() {
        return iterCount;
    }
    
    public double getLogLikelihood() {
        return logLikelihood;
    }

    @Override
    public Model getModel() {
        return model;
    }
    
    public void setLogger(Logger logger) {
        this.log = logger;
    }
        
}
