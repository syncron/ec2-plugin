package hudson.plugins.ec2;

import hudson.Extension;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.slaves.NodeProperty;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

/**
 * Slave running on EC2.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class EC2SwarmOndemandSlave extends EC2AbstractSlave {
	
	private Slave swarmNode;
	
    public EC2SwarmOndemandSlave(String instanceId, String description, String remoteFS, int numExecutors, String labelString, Mode mode, String initScript, String remoteAdmin, String jvmopts, boolean stopOnTerminate, String idleTerminationMinutes, String publicDNS, String privateDNS, List<EC2Tag> tags, String cloudName, boolean usePrivateDnsName, boolean useDedicatedTenancy, int launchTimeout, AMITypeData amiType) throws FormException, IOException {
    	this(description + " (" + instanceId + ")", instanceId, description, remoteFS, numExecutors, labelString, mode, initScript, Collections.<NodeProperty<?>>emptyList(), remoteAdmin, jvmopts, stopOnTerminate, idleTerminationMinutes, publicDNS, privateDNS, tags, cloudName, usePrivateDnsName, useDedicatedTenancy, launchTimeout, amiType);
    	this.swarmNode = null;
    } 	 

    @DataBoundConstructor
    public EC2SwarmOndemandSlave(String name, String instanceId, String description, String remoteFS, int numExecutors, String labelString, Mode mode, String initScript, List<? extends NodeProperty<?>> nodeProperties, String remoteAdmin, String jvmopts, boolean stopOnTerminate, String idleTerminationMinutes, String publicDNS, String privateDNS, List<EC2Tag> tags, String cloudName, boolean usePrivateDnsName, boolean useDedicatedTenancy, int launchTimeout, AMITypeData amiType) throws FormException, IOException {
    	
        super(name, instanceId, description, remoteFS, numExecutors, mode, "swarm-controller", new EC2SwarmComputerLauncher(), new EC2SwarmRetentionStrategy(idleTerminationMinutes), initScript, nodeProperties, remoteAdmin, jvmopts, stopOnTerminate, idleTerminationMinutes, tags, cloudName, usePrivateDnsName, useDedicatedTenancy, launchTimeout, amiType);

        this.publicDNS = publicDNS;
        this.privateDNS = privateDNS;
    }

    /**
     * Terminates the instance in EC2.
     */
    public void terminate() {
        try {
            if (!isAlive(true)) {
                /* The node has been killed externally, so we've nothing to do here */
                LOGGER.info("EC2 instance already terminated: "+getInstanceId());
            } else {
                AmazonEC2 ec2 = getCloud().connect();
                TerminateInstancesRequest request = new TerminateInstancesRequest(Collections.singletonList(getInstanceId()));
                ec2.terminateInstances(request);
                LOGGER.info("Terminated EC2 instance (terminated): "+getInstanceId());
            }
        } catch (AmazonClientException e) {
            LOGGER.log(Level.WARNING,"Failed to terminate EC2 instance: "+getInstanceId(),e);
        }
        try {
            Hudson.getInstance().removeNode(this);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,"Failed to terminate EC2 instance: "+getInstanceId(),e);
        }
    }

    @Override
	public Node reconfigure(final StaplerRequest req, JSONObject form) throws FormException {
        if (form == null) {
            return null;
        }

        if (!isAlive(true)) {
            LOGGER.info("EC2 instance terminated externally: " + getInstanceId());
            try {
                Hudson.getInstance().removeNode(this);
            } catch (IOException ioe) {
                LOGGER.log(Level.WARNING, "Attempt to reconfigure EC2 instance which has been externally terminated: " + getInstanceId(), ioe);
            }
    
            return null;
        }

        return super.reconfigure(req, form);
    }
    
    public Slave getSwarmNode() {
    	return swarmNode;
    }
    
    public void setSwarmNode(Slave node) {
    	this.swarmNode = node;
    }

    @Extension
    public static final class DescriptorImpl extends EC2AbstractSlave.DescriptorImpl {
        @Override
		public String getDisplayName() {
			return "Amazon EC2 Swarm On Demand";
        }
    }

    private static final Logger LOGGER = Logger.getLogger(EC2SwarmOndemandSlave.class.getName());

	@Override
	public String getEc2Type() {
		return "Swarm On Demand";
	}
}
