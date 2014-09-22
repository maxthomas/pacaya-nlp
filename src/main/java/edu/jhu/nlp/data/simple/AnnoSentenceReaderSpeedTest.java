package edu.jhu.nlp.data.simple;

import java.io.File;
import java.io.IOException;

import edu.jhu.nlp.data.simple.AnnoSentenceReader.AnnoSentenceReaderPrm;
import edu.jhu.nlp.data.simple.AnnoSentenceReader.DatasetType;
import edu.jhu.util.Timer;

/**
 * Speed test for AnnoSentenceReader.
 * 
 * @author mgormley
 */
public class AnnoSentenceReaderSpeedTest {

    private static String conllxDir = "/Users/mgormley/research/pacaya/data/conllx/CoNLL-X";
    //private static String conllxDir = "c:/cygwin64/home/mgormley/corpora/CoNLL-X";
    
    public static String ptbYmTest = conllxDir + "/test/data/english/ptb_ym/test/english_ptb_ym_test.conll";
    public static String ptbYmTrain = conllxDir + "/train/data/english/ptb_ym/train/english_ptb_ym_train.conll";
    public static String czTrain = AnnoSentenceReaderSpeedTest.conllxDir + "/train/data/czech/pdt/train/czech_pdt_train.conll";

    public static AnnoSentenceCollection readPtbYmConllx() {
        return read(ptbYmTest, DatasetType.CONLL_X);
    }

    public static AnnoSentenceCollection read(String path, DatasetType type) {
        AnnoSentenceReaderPrm prm = new AnnoSentenceReaderPrm();
        AnnoSentenceReader reader = new AnnoSentenceReader(prm);
        try {
            reader.loadSents(new File(path), type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return reader.getData();
    }

    /**
     * Speed test results:
     * # sents: 39832
     * Tokens / sec: 106961.04480972754
     */
    //@Test
    public void testReaderSpeed() {
        Timer t = new Timer();
        t.start();
        AnnoSentenceCollection sents = read(ptbYmTrain, DatasetType.CONLL_X);
        t.stop();
        System.out.println("# sents: "+ sents.size());
        System.out.println("Tokens / sec: " + (sents.getNumTokens() / t.totSec()));
    }
    
    public static void main(String[] args) {
        (new AnnoSentenceReaderSpeedTest()).testReaderSpeed();
    }
    
}
