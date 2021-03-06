package edu.jhu.nlp.data.simple;

import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.AnnoPipeline;
import edu.jhu.nlp.ner.NerRunner;
import edu.jhu.nlp.relations.RelationMunger;
import edu.jhu.nlp.relations.RelationMunger.RelationDataPostproc;
import edu.jhu.nlp.relations.RelationMunger.RelationDataPreproc;
import edu.jhu.nlp.relations.RelationMunger.RelationMungerPrm;
import edu.jhu.pacaya.util.cli.ArgParser;
import edu.jhu.pacaya.util.cli.Opt;

public class CorpusConverter {

    private static final Logger log = LoggerFactory.getLogger(NerRunner.class);

    @Opt(description="Perform relation munging.")
    public static boolean mungeRelations = true;
    
    private static void run(ArgParser parser) throws IOException {
        CorpusHandler handler = new CorpusHandler();
        AnnoSentenceCollection sents = handler.getTrainGold();
        if (mungeRelations) {
            AnnoPipeline prep = new AnnoPipeline();
            RelationMunger relMunger = new RelationMunger(parser.getInstanceFromParsedArgs(RelationMungerPrm.class));
            // Pre-processing.
            RelationDataPreproc dataPreproc = relMunger.getDataPreproc();
            prep.add(dataPreproc);
            if (!relMunger.getPrm().makeRelSingletons) {
                // Post-processing.
                RelationDataPostproc dataPostproc = relMunger.getDataPostproc();
                prep.add(dataPostproc);
            }
            prep.annotate(sents);
            AnnoSentenceReader.logSentStats(sents, log, "train");
        }
        handler.writeTrainGold();
    }
    
    public static void main(String[] args) {
        int exitCode = 0;
        ArgParser parser = null;
        try {
            parser = new ArgParser(CorpusConverter.class);
            parser.registerClass(CorpusConverter.class);
            parser.registerClass(CorpusHandler.class);
            parser.registerClass(RelationMungerPrm.class);
            parser.parseArgs(args);

            CorpusConverter.run(parser);
        } catch (ParseException e1) {
            log.error(e1.getMessage());
            if (parser != null) {
                parser.printUsage();
            }
            exitCode = 1;
        } catch (Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        }
        
        System.exit(exitCode);
    }
    
}
