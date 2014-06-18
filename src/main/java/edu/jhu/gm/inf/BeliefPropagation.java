package edu.jhu.gm.inf;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.erma.AbstractFgInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.util.Timer;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

/**
 * Loopy belief propagation inference algorithm.
 * 
 * @author mgormley
 *
 */
public class BeliefPropagation extends AbstractFgInferencer implements FgInferencer {
    
    private static final Logger log = Logger.getLogger(BeliefPropagation.class);

    public static class BeliefPropagationPrm implements FgInferencerFactory {
        public BpScheduleType schedule = BpScheduleType.TREE_LIKE;
        public int maxIterations = 100;
        public double timeoutSeconds = Double.POSITIVE_INFINITY;
        public BpUpdateOrder updateOrder = BpUpdateOrder.PARALLEL;
        public boolean logDomain = true;
        public Algebra s = null;
        /** Whether to normalize the messages after sending. */
        public boolean normalizeMessages = true;        
        public boolean cacheFactorBeliefs = false;
        /** The maximum message residual for convergence testing. */
        public double convergenceThreshold = 0;
        public BeliefPropagationPrm() {
        }
        public FgInferencer getInferencer(FactorGraph fg) {
            return new BeliefPropagation(fg, this);
        }
        
        @Override
        public Algebra getAlgebra() {
            if (s == null) {
                return logDomain ? Algebras.LOG_SEMIRING : Algebras.REAL_ALGEBRA;
            } else {
                return s;
            }
        }
        
    }
    
    public enum BpScheduleType {
        /** Send messages from a root to the leaves and back. */
        TREE_LIKE,
        /** Send messages in a random order. */
        RANDOM
    }
    
    public enum BpUpdateOrder {
        /** Send each message in sequence according to the schedule. */ 
        SEQUENTIAL,
        /** Create all messages first. Then send them all at the same time. */
        PARALLEL
    };
    
    private final BeliefPropagationPrm prm;
    private final Algebra s;
    private final FactorGraph fg;
    /** A container of messages each edge in the factor graph. Indexed by edge id. */
    private final Messages[] msgs;
    private BpSchedule sched;
    
    private VarTensor[] factorBeliefCache;
    // The number of messages that have converged.
    private int numConverged = 0;

    public BeliefPropagation(final FactorGraph fg, BeliefPropagationPrm prm) {
        this.prm = prm;
        this.s = prm.getAlgebra();
        this.fg = fg;
        this.msgs = new Messages[fg.getNumEdges()];
        this.factorBeliefCache = new VarTensor[fg.getNumFactors()];
        
        if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
            if (prm.schedule == BpScheduleType.TREE_LIKE) {
                sched = new BfsBpSchedule(fg);
            } else if (prm.schedule == BpScheduleType.RANDOM) {
                sched = new RandomBpSchedule(fg);
            } else {
                throw new RuntimeException("Unknown schedule type: " + prm.schedule);
            }
        } else {
            sched = new BpSchedule() {
                @Override
                public List<FgEdge> getOrder() {
                    return fg.getEdges();
                }
            };
        }
    }
    
    /** For testing only. */
    public Messages[] getMessages() {
    	return msgs;
    }
    
    /** @inheritDoc */
    @Override
    public void run() {
        Timer timer = new Timer();
        timer.start();
                
        // Initialization.
        for (int i=0; i<msgs.length; i++) {
            // TODO: consider alternate initializations.
            msgs[i] = new Messages(s, fg.getEdge(i), s.one());
        }
        // Reset the global factors.
        for (Factor factor : fg.getFactors()) {
            if (factor instanceof GlobalFactor) {
                ((GlobalFactor)factor).reset();
            }
        }
        if (prm.cacheFactorBeliefs) {
            // Initialize factor beliefs
            for(FgNode node : fg.getNodes()) {
            	if(node.isFactor() && !(node.getFactor() instanceof GlobalFactor)) {
                	Factor f = node.getFactor();        		
            		VarTensor fBel = new VarTensor(s, f.getVars());
                	int c = f.getVars().calcNumConfigs();
                	for(int i=0; i<c; i++)
                		fBel.setValue(i, s.fromLogProb(f.getLogUnormalizedScore(i)));
                	
                	for(FgEdge v2f : node.getInEdges()) {
                		VarTensor vBel = msgs[v2f.getId()].message;
                		fBel.prod(vBel);
                	}
                	
                	factorBeliefCache[f.getId()] = fBel;
            	}
            }
        }
        
        
        // Message passing.
        //
        // At iteration -1, we send all the constant messages. Then we never send them again.
        List<FgEdge> order = null;
        for (int iter=-1; iter < prm.maxIterations; iter++) {
            if (timer.totSec() > prm.timeoutSeconds) {
                break;
            }
            order = updateOrder(order, iter);
            if (prm.updateOrder == BpUpdateOrder.SEQUENTIAL) {
                for (FgEdge edge : order) {
                    createMessage(edge, iter);
                    sendMessage(edge);
                    if (isConverged()) {
                        // Stop on convergence: Break out of inner loop.
                        break;
                    }
                }
            } else if (prm.updateOrder == BpUpdateOrder.PARALLEL) {
                for (FgEdge edge : order) {
                    createMessage(edge, iter);
                }
                for (FgEdge edge : order) {
                    sendMessage(edge);
                }
            } else {
                throw new RuntimeException("Unsupported update order: " + prm.updateOrder);
            }
            if (isConverged()) {
                // Stop on convergence.
                log.trace("Stopping on convergence. Iterations = " + (iter+1));
                break;
            }
        }
        
        // Clear memory.
        for (Messages msg : msgs) {
            // These are not needed to compute the marginals.
            msg.newMessage = null;
        }
        
        timer.stop();
    }

    private List<FgEdge> updateOrder(List<FgEdge> order, int iter) {
        if (iter >= 1 && !(prm.updateOrder == BpUpdateOrder.SEQUENTIAL || prm.schedule == BpScheduleType.RANDOM)) {
            // Just re-use the same order.
            return order;
        }
        // Get the initial order for the edges.
        order = sched.getOrder();
        if (iter == -1) {   
            // Keep only the messages from the leaves for iteration -1. Then never send these again.
            order = filterNonConstantMsgs(order);
        } else {
            // Filter out the messages from the leaves.
            order = filterConstantMsgs(order);
        }
        return order;
    }
    
    /** Filters edges from a leaf node. */
    private List<FgEdge> filterConstantMsgs(List<FgEdge> order) {
        ArrayList<FgEdge> filt = new ArrayList<FgEdge>();
        for (FgEdge edge : order) {
            // If the parent node is not a leaf.
            if (!isConstantMsg(edge)) {
                filt.add(edge);
            }
        }
        return filt;
    }
    
    /** Filters edges not from a leaf node. */
    private List<FgEdge> filterNonConstantMsgs(List<FgEdge> order) {
        ArrayList<FgEdge> filt = new ArrayList<FgEdge>();
        for (FgEdge edge : order) {
            // If the parent node is not a leaf.
            if (isConstantMsg(edge)) {
                filt.add(edge);
            }
        }
        return filt;
    }

    /** Returns true iff the edge corresponds to a message which is constant (i.e. sent from a leaf node). */
    private boolean isConstantMsg(FgEdge edge) {
        return edge.getParent().getOutEdges().size() == 1;
    }
    
    public boolean isConverged() {
        return numConverged == msgs.length;
    }
        
    /**
     * Creates a message and stores it in the "pending message" slot for this edge.
     * @param edge The directed edge for which the message should be created.
     * @param iter The iteration number.
     */
    protected void createMessage(FgEdge edge, int iter) {

    	int edgeId = edge.getId();
        Var var = edge.getVar();
        Factor factor = edge.getFactor();
        
        if (!edge.isVarToFactor() && factor instanceof GlobalFactor) {
            log.trace("Creating messages for global factor.");
            // Since this is a global factor, we pass the incoming messages to it, 
            // and efficiently marginalize over the variables. The current setup is
            // create all the messages from this factor to its variables, but only 
            // once per iteration.
            GlobalFactor globalFac = (GlobalFactor) factor;
            boolean created = globalFac.createMessages(edge.getParent(), msgs, iter);
            if (created && prm.normalizeMessages) {
               for (FgEdge e2 : edge.getParent().getOutEdges()) {
                   normalize(msgs[e2.getId()].newMessage);
               }
            }
            // The messages have been set, so just return.
            return;
        } else {
        	// Since this is not a global factor, we send messages in the normal way, which
            // in the case of a factor to variable message requires enumerating all possible
            // variable configurations.
            VarTensor msg = msgs[edgeId].newMessage;
            
            // Initialize the message to all ones (zeros in log-domain) since we are "multiplying".
            msg.fill(s.one());
            
            if (edge.isVarToFactor()) {
                // Message from variable v* to factor f*.
                //
                // Compute the product of all messages received by v* except for the
                // one from f*.
                getProductOfMessages(edge.getParent(), msg, edge.getChild());
            } else {
                // Message from factor f* to variable v*.
                //
                // Compute the product of all messages received by f* (each
                // of which will have a different domain) with the factor f* itself.
                // Exclude the message going out to the variable, v*.
                VarTensor prod;
            	if(prm.cacheFactorBeliefs && !(factor instanceof GlobalFactor)) {
            		// we are computing f->v, which is the product of a bunch of factor values and v->f messages
            		// we can cache this product and remove the v->f message that would have been excluded from the product
            		VarTensor remove = msgs[edge.getOpposing().getId()].message;
            		VarTensor from = factorBeliefCache[factor.getId()];
            		prod = new VarTensor(from);
            		prod.divBP(remove);
            		
            		assert !prod.containsBadValues() : "prod from cached beliefs = " + prod;
            	}
            	else {	// fall back on normal way of computing messages without caching
                    // Set the initial values of the product to those of the sending factor.
            		prod = BruteForceInferencer.safeNewVarTensor(s, factor);
                	getProductOfMessages(edge.getParent(), prod, edge.getChild());
            	}

            	
                // Marginalize over all the assignments to variables for f*, except
                // for v*.
                msg = prod.getMarginal(new VarSet(var), false);
            }
            
            assert (msg.getVars().equals(new VarSet(var)));
            
            normalize(msg);
            
            // Set the final message in case we created a new object.
            msgs[edgeId].newMessage = msg;
        }
    }

    private void normalize(VarTensor msg) {
        if (prm.normalizeMessages) {
            msg.normalize();
        } else { 
            // normalize and logNormalize already check for NaN
            assert !msg.containsBadValues() : "msg = " + msg;
        }
    }

    /**
     * Computes the product of all messages being sent to a node, optionally
     * excluding messages sent from another node.
     * 
     * Upon completion, prod will contain the product of all incoming messages
     * to node, except for the message from exclNode if specified, times the
     * factor given in prod.
     * 
     * @param node
     *            The node to which all the messages are being sent.
     * @param prod
     *            An input factor with which the product will (destructively) be
     *            taken.
     * @param exclNode
     *            If null, the product of all messages will be taken. If
     *            non-null, any message sent from exclNode to node will be
     *            excluded from the product.
     */
    protected void getProductOfMessages(FgNode node, VarTensor prod, FgNode exclNode) {
        for (FgEdge nbEdge : node.getInEdges()) {
            if (nbEdge.getParent() == exclNode) {
                // Don't include the receiving variable.
                continue;
            }
            // Get message from neighbor to factor.
            VarTensor nbMsg = msgs[nbEdge.getId()].message;
            
            // If the node is a variable, this is an element-wise product. 
            // If the node is a factor, this an an outer product.
            prod.prod(nbMsg);
            assert !prod.containsBadValues() : "prod = " + prod;
        }
    }

    /** Gets the product of messages (as in getProductOfMessages()) and then normalizes. */
    protected void getProductOfMessagesNormalized(FgNode node, VarTensor prod, FgNode exclNode) {
        getProductOfMessages(node, prod, exclNode);
        prod.normalize();
    }
    
    /**
     * Sends the message that is currently "pending" for this edge. This just
     * copies the message in the "pending slot" to the "message slot" for this
     * edge.
     * 
     * @param edge The edge over which the message should be sent.
     */
    protected void sendMessage(FgEdge edge) {
        int edgeId = edge.getId();
       
        Messages ec = msgs[edgeId];
        // Update the residual
        double oldResidual = ec.residual;
        ec.residual = smartResidual(ec.message, ec.newMessage, edge);
        if (oldResidual > prm.convergenceThreshold && ec.residual <= prm.convergenceThreshold) {
            // This message has (newly) converged.
            numConverged ++;
        }
        if (oldResidual <= prm.convergenceThreshold && ec.residual > prm.convergenceThreshold) {
            // This message was marked as converged, but is no longer converged.
            numConverged--;
        }
        
        // Send message: Just swap the pointers to the current message and the new message, so
        // that we don't have to create a new factor object.
        VarTensor oldMessage = ec.message;
        ec.message = ec.newMessage;
        ec.newMessage = oldMessage;
        assert !ec.message.containsBadValues() : "ec.message = " + ec.message;

        // Update factor beliefs
        if(prm.cacheFactorBeliefs && edge.isVarToFactor() && !(edge.getFactor() instanceof GlobalFactor)) {
        	Factor f = edge.getFactor();
        	VarTensor update = factorBeliefCache[f.getId()];
        	update.divBP(oldMessage);
        	update.prod(ec.message);
        }
        
        if (log.isTraceEnabled()) {
            log.trace("Message sent: " + ec.message);
        }
    }

    /** Returns the "converged" residual for constant messages, and the actual residual otherwise. */
    private double smartResidual(VarTensor message, VarTensor newMessage, FgEdge edge) {
        // This is intentionally NOT the semiring zero.
        return isConstantMsg(edge) ? 0.0 : getResidual(message, newMessage);
    }

    /**
     * Gets the residual for a new message, as the maximum error over all assignments.
     * 
     * Following the definition of Sutton & McCallum (2007), we compute the residual as the infinity
     * norm of the difference of the log of the message vectors.
     * 
     * Note: the returned value is NOT in the semiring / abstract algebra. It is the actual value
     * described above.
     */
    private double getResidual(VarTensor t1, VarTensor t2) {
        assert s == t1.getAlgebra() && s == t2.getAlgebra();
        Tensor.checkEqualSize(t1, t2);
        Tensor.checkSameAlgebra(t1, t2);
        double residual = Double.NEGATIVE_INFINITY;
        for (int c=0; c<t1.size(); c++) {
            double abs = Math.abs(s.toLogProb(t1.get(c)) - s.toLogProb(t2.get(c)));
            if (abs > residual) {
                residual = abs;
            }
        }
        return residual;
    }
    
    protected VarTensor getVarBeliefs(int varId) {
        return getVarBeliefs(fg.getVar(varId));
    }
    
    protected VarTensor getFactorBeliefs(int facId) {
        return getFactorBeliefs(fg.getFactor(facId));
    }
    
    protected VarTensor getVarBeliefs(Var var) {
        VarTensor prod = new VarTensor(s, new VarSet(var), s.one());
        // Compute the product of all messages sent to this variable.
        FgNode node = fg.getVarNode(var.getId());
        getProductOfMessagesNormalized(node, prod, null);
        return prod;
    }

    protected VarTensor getFactorBeliefs(Factor factor) {
        if (factor instanceof GlobalFactor) {
            log.warn("Getting marginals of a global factor is not supported."
                    + " This will require exponential space to store the resulting factor."
                    + " This should only be used for testing.");
        }
        
        VarTensor prod = new VarTensor(BruteForceInferencer.safeNewVarTensor(s, factor));
        // Compute the product of all messages sent to this factor.
        FgNode node = fg.getFactorNode(factor.getId());
        getProductOfMessagesNormalized(node, prod, null);
        return prod;
    }
    
    public double getPartitionBelief() {
        if (prm.schedule == BpScheduleType.TREE_LIKE && prm.normalizeMessages == false) {
            // Special case which only works on non-loopy graphs with the two pass schedule and 
            // no renormalization of messages.
            // 
            // The factor graph's overall partition function is the product of the
            // partition functions for each connected component. 
            double partition = s.one();
            for (FgNode node : fg.getConnectedComponents()) {
                if (!node.isVar()) {
                    if (node.getOutEdges().size() == 0) {
                        // This is an empty factor that makes no contribution to the partition function.
                        continue;
                    } else {
                        // Get a variable node in this connected component.
                        node = node.getOutEdges().get(0).getChild();
                        assert(node.isVar());
                    }
                }
                
                double nodePartition = getPartitionBeliefAtVarNode(node);
                partition = s.times(partition, nodePartition);
            }
            assert !s.isNaN(partition);
            return partition;
        }
        
        return s.fromLogProb(- getBetheFreeEnergy());
    }
    
    /**
     * Computes the Bethe free energy of the factor graph. For acyclic graphs,
     * this is equal to -log(Z) where Z is the exact partition function. For 
     * loopy graphs it can be used as an approximation.
     */
    protected double getBetheFreeEnergy() {
        // 
        // G_{Bethe} = \sum_a \sum_{x_a} - b(x_a) ln \chi(x_a)
        //              + \sum_a \sum_{x_a} b(x_a) ln b(x_a)
        //              + \sum_i (n_i - 1) \sum_{x_i} b(x_i) ln b(x_i)
        //           = \sum_a \sum_{x_a} b(x_a) ln (b(x_a) / \chi(x_a))
        //              + \sum_i (n_i - 1) \sum_{x_i} b(x_i) ln b(x_i)
        //
        //     where n_i is the number of neighbors of the variable x_i,
        //     b(x_a) and b(x_i) are normalized distributions and x_a is 
        //     the set of variables participating in factor a. 
        //
                
        double bethe = 0.0;
        for (int a=0; a<fg.getFactors().size(); a++) {
            Factor f = fg.getFactors().get(a);
            if (!(f instanceof GlobalFactor)) {
                int numConfigs = f.getVars().calcNumConfigs();
                VarTensor beliefs = getFactorBeliefs(a);
                for (int c=0; c<numConfigs; c++) {                
                    // Since we want multiplication by 0 to always give 0 (not the case for Double.POSITIVE_INFINITY or Double.NaN.
                    double b_c = beliefs.getValue(c);
                    if (b_c != s.zero()) {
                        double r_b_c = s.toReal(b_c);
                        double log_b_c = s.toLogProb(b_c);
                        double log_chi_c = f.getLogUnormalizedScore(c);
                        bethe += r_b_c * (log_b_c - log_chi_c);
                    }
                }
            } else {
                bethe += ((GlobalFactor) f).getExpectedLogBelief(fg.getFactorNode(a), msgs);
            }
        }
        for (int i=0; i<fg.getVars().size(); i++) {
            Var v = fg.getVars().get(i);
            int numNeighbors = fg.getVarNode(i).getOutEdges().size();
            if (numNeighbors > 1) {
                VarTensor beliefs = getVarBeliefs(i);
                double sum = 0.0;
                for (int c=0; c<v.getNumStates(); c++) {
                    double b_c = beliefs.getValue(c);
                    if (b_c != s.zero()) {
                        double r_b_c = s.toReal(b_c);
                        double log_b_c = s.toLogProb(b_c);
                        sum += r_b_c * log_b_c;
                    }
                }
                bethe -= (numNeighbors - 1) * sum;
            }
        }
        
        assert !Double.isNaN(bethe);        
        return bethe;
    }

    /**
     * FOR TESTING ONLY.
     * Gets the partition function for the connected component containing the given node.
     */
    // TODO: This should be package private or protected. It is exposed for testing only.
    public double getPartitionBeliefAtVarNode(FgNode node) {
        // We just return the normalizing constant for the marginals of any variable.
        if (!node.isVar()) {
            throw new IllegalArgumentException("Node must be a variable node.");
        }
        Var var = node.getVar();
        VarTensor prod = new VarTensor(s, new VarSet(var), s.one());
        // Compute the product of all messages sent to this node.
        getProductOfMessages(node, prod, null);
        return prod.getSum();
    }

    public FactorGraph getFactorGraph() {
        return fg;
    }
    
    public Algebra getAlgebra() {
        return s;
    }
    
}
