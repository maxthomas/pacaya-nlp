package edu.jhu.hltcoe.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import edu.stanford.nlp.ling.HasTag;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.Dependency;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;

public class DepTree {

    private static final int EMPTY = -2;
    private static final int WALL = -1;
    
    private List<DepTreeNode> nodes = new ArrayList<DepTreeNode>();
    private int[] parents;
    
    public DepTree(Tree tree) {  
        List<Tree> leaves = tree.getLeaves();

        // Create parents array
        parents = new int[leaves.size()];
        Arrays.fill(parents, EMPTY);
        
        HeadFinder hf = new CollinsHeadFinder();
        Collection<Dependency<Tree, Tree, Object>> dependencies = mapDependencies(tree, hf);
        assert(dependencies.size() == leaves.size() - 1);
        for(Dependency<Tree, Tree, Object> dependency : dependencies) {
            Tree parent = dependency.governor();
            Tree child = dependency.dependent();
            
            int parentIdx = indexOfInstance(leaves, parent);
            int childIdx = indexOfInstance(leaves, child);
            
            if (childIdx < 0) {
                throw new IllegalStateException("Child not found in leaves: " + child);
            } else if (parents[childIdx] != EMPTY) {
                throw new IllegalStateException("Multiple parents for the same node: " + child);
            }
            
            parents[childIdx] = parentIdx;
        }
        for (int i=0; i<parents.length; i++) {
            if (parents[i] == EMPTY) {
                parents[i] = WALL;
            }
        }
        
        // Create nodes
        int position = 0;
        for (Tree leaf : leaves) {
            edu.stanford.nlp.ling.Label label = leaf.label();
            String word = null;
            if (label instanceof HasWord) {
                word = ((HasWord)label).word();
            }
            String tag = null;
            if (label instanceof HasTag) {
                tag = ((HasTag)label).tag();
            }
            assert(word != null || tag != null);
            nodes.add(new DepTreeNode(word, tag, position));            
            position++;
        }
        
        // Add parent/child links to DepTreeNodes
        addParentChildLinksToNodes();
    }

    public DepTree(Sentence sentence, int[] parents) {
        this.parents = parents;
        for (Label tw : sentence) {
            nodes.add(new DepTreeNode(tw));
        }
        // Add parent/child links to DepTreeNodes
        addParentChildLinksToNodes();
    }
    
    private void addParentChildLinksToNodes() {
        checkTree();
        for (int i=0; i<parents.length; i++) {
            DepTreeNode node = nodes.get(i);
            if (parents[i] != WALL) {
                node.setParent(nodes.get(parents[i]));
            }
            for (int j=0; j<parents.length; j++) {
                if (parents[j] == i) {
                    node.addChild(nodes.get(j));
                }
            }
        }
    }

    private void checkTree() {
        // Check that there is exactly one node with the WALL as its parent
        int emptyCount = countChildrenOf(EMPTY);
        if (emptyCount != 0) {
            throw new IllegalStateException("Found an empty parent cell. emptyCount=" + emptyCount);
        }
        int wallCount = countChildrenOf(WALL);
        if (wallCount != 1) {
            throw new IllegalStateException("There must be exactly one node with the wall as a parent. wallCount=" + wallCount);
        }
        
        // Check that there are no cyles
        for (int i=0; i<parents.length; i++) {
            int numAncestors = 0;
            int parent = parents[i];
            while(parent != WALL) {
                numAncestors += 1;
                if (numAncestors > parents.length - 1) {
                    throw new IllegalStateException("Found cycle in parents array");
                }
                parent = parents[parent];
            }
        }

        // Check for proper list lengths
        if (nodes.size() != parents.length) {
            throw new IllegalStateException("Number of nodes does not equal number of parents");
        }
    }

    private int countChildrenOf(int parent) {
        int count = 0;
        for (int i=0; i<parents.length; i++) {
            if (parents[i] == parent) {
                count++;
            }
        }
        return count;
    }

    /**
     * Standard List.indexOf() uses the equals method for comparison.
     * This method uses == for comparison.
     */
    private <X> int indexOfInstance(List<X> list, X obj) {
        int i=0; 
        for (X x : list) {
            if ( x == obj) {
               return i; 
            }
            i++;
        }
        return -1;
    }
    
    @Override
    public String toString() {
        for (int i=0; i<parents.length; i++) {
            //TODO:
        }
        return super.toString();
    }

    public static Collection<Dependency<Tree, Tree, Object>> mapDependencies(Tree tree, HeadFinder hf) {
        if (hf == null) {
            throw new IllegalArgumentException("mapDependencies: need headfinder");
        }
        List<Dependency<Tree, Tree, Object>> deps = new ArrayList<Dependency<Tree, Tree, Object>>();
        for (Tree node : tree) {
            if (node.isLeaf() || node.children().length < 2) {
                continue;
            }
            // Label l = node.label();
            // System.err.println("doing kids of label: " + l);
            // Tree hwt = node.headPreTerminal(hf);
            Tree hwt = node.headTerminal(hf);
            // System.err.println("have hf, found head preterm: " + hwt);
            if (hwt == null) {
                throw new IllegalStateException("mapDependencies: headFinder failed!");
            }

            for (Tree child : node.children()) {
                // Label dl = child.label();
                // Tree dwt = child.headPreTerminal(hf);
                Tree dwt = child.headTerminal(hf);
                if (dwt == null) {
                    throw new IllegalStateException("mapDependencies: headFinder failed!");
                }
                // System.err.println("kid is " + dl);
                // System.err.println("transformed to " +
                // dml.toString("value{map}"));
                if (dwt != hwt) {
                    Dependency<Tree, Tree, Object> p = new UnnamedTreeDependency(hwt, dwt);
                    deps.add(p);
                }
            }
        }
        return deps;
    }

    public List<DepTreeNode> getNodes() {
        return nodes;
    }

}