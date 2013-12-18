package edu.jhu.globalopt.randwalk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import edu.jhu.globalopt.AbstractNodeOrderer;
import edu.jhu.globalopt.NodeOrderer;
import edu.jhu.globalopt.ProblemNode;
import edu.jhu.globalopt.dmv.DmvProblemNode;
import edu.jhu.util.Prng;

public class DfsRandWalkNodeOrderer extends AbstractNodeOrderer implements NodeOrderer {

    private static final Logger log = Logger.getLogger(DfsRandWalkNodeOrderer.class);

    private int maxDepth;
    private ProblemNode rootNode;
    private ArrayList<ProblemNode> nodes;

    public DfsRandWalkNodeOrderer(int maxDepth) {
        this.rootNode = null;
        this.maxDepth = maxDepth; 
        this.nodes = new ArrayList<ProblemNode>();
    }

    @Override
    public boolean add(ProblemNode node) {
        if (rootNode == null) {
            rootNode = node;
        }
        return nodes.add(node);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ProblemNode remove() {
        if (nodes.size() == 0 || nodes.get(0).getDepth() >= maxDepth) {
            if (rootNode == null) {
                throw new NoSuchElementException();
            }
            nodes.clear();
            ((DmvProblemNode)rootNode).clear();
            return rootNode;
        } else {
            // Return a random child, then clear away all the rest.
            ProblemNode next = nodes.remove(Prng.nextInt(nodes.size()));
            nodes.clear();
            return next;
        }
    }

    @Override
    public int size() {
        return nodes.size();
    }
    
    public void clear() {
        nodes.clear();
    }

    @Override
    public Iterator<ProblemNode> iterator() {
        // TODO: Fix this!
        log.error("Not creating a proper iterator over the entire collection");
        return nodes.iterator();
    }

}