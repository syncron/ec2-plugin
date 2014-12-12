package hudson.plugins.ec2;

import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.model.labels.LabelAtom;
import hudson.remoting.Channel;
import hudson.remoting.Channel.Listener;
import hudson.slaves.ComputerLauncher;

import java.io.IOException;
import java.io.PrintStream;

import jenkins.slaves.iterators.api.NodeIterator;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.Instance;

public class EC2SwarmComputerLauncher extends EC2ComputerLauncher {

	@Override
	protected void launch(EC2Computer computer, PrintStream logger, Instance inst) throws AmazonClientException,
			IOException, InterruptedException {
		EC2SwarmOndemandSlave slave = (EC2SwarmOndemandSlave) computer.getNode();
        for (int i=0; i<20; i++) {
        	logger.println("Waiting for swarm slave to get up");
        	for (Slave node: NodeIterator.nodes(Slave.class)){
            	if (nodeHasLabel(node, "swarm") && nodeHasLabel(node, slave.getInstanceId())) {
            		logger.println("Got slave " + node.getDisplayName());
            		slave.setSwarmNode(node);
            		computer.setChannel((Channel) slave.getChannel(), logger, new Listener(){});
            		return;
            	}
            }
        	try {
        		Thread.sleep(10000);
            }  catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
	}

    private boolean nodeHasLabel(Slave node, String label) {
    	for (LabelAtom labelAtom : node.getAssignedLabels())
    		if (labelAtom.getName().equalsIgnoreCase(label))
    			return true;
    	return false;
    }

    @Override
	public Descriptor<ComputerLauncher> getDescriptor() {
        throw new UnsupportedOperationException();
    }

}
