package com.enro.htool.main;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enro.bwutils.BWUtils;
import com.enro.htool.common.ConfReader;
import com.enro.htool.common.HToolConstants;
import com.enro.htool.common.HToolUtil;

import COM.TIBCO.hawk.config.rbengine.rulebase.Rulebase;
import COM.TIBCO.hawk.config.rbengine.rulebase.RulebaseXML;
import COM.TIBCO.hawk.talon.DataElement;
import COM.TIBCO.hawk.talon.MethodInvocation;
import COM.TIBCO.hawk.talon.MicroAgentID;

/*
 * HRBDispatcher
 * Send rulebases to appropriate Hawk agents in a Hawk environment
 * Implements the Dispatcher interface so that the console can invoke it when MA's are discovered
 * 
 * 1. Find configuration file, its only argument.
 * 2. Read configuration file and use it to call the ConsoleFactory.
 * 3. ConsoleFactory returns a valid console for the configuration.
 * 4. Initialize the console (subscribe to agents).
 * 5. The console invokes Dispatcher methods to send Rulebases to appropriate MicroAgents.
 * 6. Finalize console and finish the program.
 */
public class HRBDispatcher implements Dispatcher,Runnable{
	
	private NuHToolConsole console;
	private String domain;
	private static Logger logger = LoggerFactory.getLogger(HRBDispatcher.class);
	private List<RBTemplate> templates = new ArrayList<RBTemplate>();
	private List<AgentDetail> agents = new ArrayList<AgentDetail>();
	private int interval = 30000;

	public HRBDispatcher(NuHToolConsole console, String domain) {
		this.console = console;
		this.domain = domain;
	}

	private static void showUsage(){
		String nm;
		
		try{
			nm = new File(HRBDispatcher.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
		} catch(Exception e){nm = "HRBDispatcher";}
		System.err.println("Usage: " + nm + " [conf_file]");

		System.exit(1);
	}
	
	private void readTemplates(ConfReader reader) throws IOException {
		
		String templatePath = reader.getString("Template", "path", "");
		if(templatePath.length() == 0) throw new IOException("Invalid template path directory.");
		
		for(String configuredMicroAgent : reader.getMASections()){
			String microAgent = configuredMicroAgent.split(":")[1];
			Properties microAgentProps = reader.getSection(configuredMicroAgent);
			templates.addAll(HToolUtil.readTemplates(templatePath,microAgent,microAgentProps));
		}
		
	}

	private String replaceInTemplate(String deployment, String component, String sourceAgent, String targetAgent, RBTemplate r) {
		
		// Replace known parameters
    	String processed = 
				BWUtils.strReplace(
					BWUtils.strReplace(
						BWUtils.strReplace(
							BWUtils.strReplace(r.getRbXML(),
								HToolConstants.DOMEXP,
								domain),
							HToolConstants.DEPEXP,
							deployment),
						HToolConstants.CMPEXP,
						component),
					HToolConstants.TAGEXP,
					targetAgent);
    	
    	// Retrieve available template properties
    	Properties props = r.getTemplateProps();
    	
    	if(null != props && !props.isEmpty()){
    		
    		// Check to see if we got any service directives
    		String serviceList = props.getProperty("service_list", "");	
    		
    		
    		if(serviceList.length()>0){
    			
    			// For Service MicroAgents, replace the service name
    			String [] services = serviceList.split(",");
    			boolean processService = false;
    			
    			for(String service : services){
    				
    				if(service.contentEquals(r.getTmpltID())){
    					processService = true;
    					break;
    				}    				
    			}
    			
    			// Ignore if no valid service name was found
				if(!processService) processed = "";
    		}
    		
    		// Check to see if we got any script directives
    		String script = props.getProperty("script", "");			
    		if(script.length()>0){
    			processed = BWUtils.strReplace(processed, HToolConstants.SCREXP, script);
    		}
    		
    	}
    	
    	// Finally, for HawkEvent, replace the sourceAgent into the rulebase where appropriate
    	processed = (null != sourceAgent) ? BWUtils.strReplace(processed, HToolConstants.EAGEXP, sourceAgent) : processed;
    	
    	return processed;
	}
	
	private RulebaseXML getRulebaseXML(String rulebaseData, String rulebaseName) {
    	try {
			Rulebase rb = new Rulebase(new StringReader(rulebaseData));
			
			// Rulebase name can't contain spaces
			String nm = BWUtils.strReplace(rulebaseName,"\\s", "_");
			
			logger.debug("Configuring XML for rulebase " + nm);
			rb.setName(nm);
			rb.setLastModification(this.getClass().getSimpleName());
			
			logger.debug("XML data is\n " + rulebaseData);
			
			RulebaseXML rbXml = new RulebaseXML(rb);
			logger.debug("Pocesses XML is\n"+rbXml.getXMLString());
			
			return rbXml;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return null;
	}
	
	private RulebaseXML processTemplate(RBTemplate r, MicroAgentID maid,String targetAgent, String sourceAgent) {
		
		String maNm = maid.getName();
		String sep = maNm.contains(HToolConstants.BWMANM) ? domain + "." : "hawk.";
		String [] parts = maNm.substring(maNm.lastIndexOf(sep)+sep.length()).split("\\.");
		String deployment = parts[0];
    	String component = parts[1];
    	
    	String rulebaseName = BWUtils.strReplace(deployment+"_"+component+"_"+r.getTmpltID(),"\\.[hH][rR][bB]$","");
    	logger.info("Generating rulebase <" + rulebaseName + ">");
    	
    	String rulebaseData = replaceInTemplate(deployment,component,sourceAgent,targetAgent,r);
    	
    	// Rulebase was skipped
    	if(rulebaseData.length() == 0) return null;
    	
    	return getRulebaseXML(rulebaseData, rulebaseName);
	}
	
	private void processRulebase(RBTemplate r, MicroAgentID maid, AgentDetail ad,List<AgentDetail> ads) {
		
		String agdNm = ad.getAgentInstance().getAgentID().getName();
		List<AgentDetail> agents = (null != ads) ? ads : new ArrayList<AgentDetail>();
		
		if(null == ads) agents.add(ad);
		
		for(AgentDetail sourceAgent : agents){
			
			String srcNm = (null == ads) ? null : sourceAgent.getAgentInstance().getAgentID().getName();
			RulebaseXML rbXml = processTemplate(r,maid,agdNm,srcNm);
			
			if(null != rbXml){
				try{
					DataElement[] dataElements = new DataElement[1];
					dataElements[0] = new DataElement("RulebaseXML", rbXml);
					MethodInvocation mi = new MethodInvocation("addRuleBase", dataElements);
					console.invoke(ad.getRbe(), mi);
				} catch (Exception e){
					logger.info("Unable to send " + r.getTmpltID() + " to " + agdNm);
					logger.error(e.getMessage());
				}
			}
		}	 
	}
	
	@Override
	public void dispatch(AgentDetail agentDetail) {	
		agents.add(agentDetail);
	}
	
	private void dispatch(){
		
		// Iterate through each agent that was discovered in the environment
		for(AgentDetail agentDetail : agents){
			logger.info("Processing " + agentDetail.getAgentInstance().getAgentID().getName() + "'s MicroAgents");
			
			// Iterate through each template that was loaded
			for(RBTemplate r : templates){
				
				// This is the MicroAgent that the rulebase applies to
				String rulebaseTarget = r.getMaID();
				
				// Check each MicroAgent to see if it matches the rulebase
				for(MicroAgentID maid : agentDetail.getMAs()){
					// This is the current MicroAgent's name
					String magName = maid.getName();
					
					if(magName.contains(rulebaseTarget)){
						logger.debug(r + " matches. Rulebase will be sent to " + magName);
						processRulebase(r,maid,agentDetail,(rulebaseTarget.contentEquals(HToolConstants.HEMANM)) ? agents : null);
					}
				}
			}
		}
	}

	@Override
	public void run() {

		try{
			this.console.start();
			Thread.sleep(interval);
			dispatch();
		} catch(InterruptedException ie){
			logger.info("Ending now.");
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally{	
			this.console.end();
		}
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}
	
	public void setInterval(int interval){
		this.interval = interval;
	}
	public static void main(String [] args){
		
		if(args.length != 1 ) showUsage();
		
		try{
			ConfReader reader = new ConfReader(args[0]);
			NuHToolConsole console = HToolConsoleFactory.getInstance(reader);
			HRBDispatcher dispatcher = new HRBDispatcher(console,reader.getString("General","hawk_domain", ""));
			dispatcher.setInterval(reader.getInt("General", "interval", 30000));
			console.addDispatcher(dispatcher);
			
			dispatcher.readTemplates(reader);
			
			Thread t = new Thread(dispatcher);
			t.start();
		} catch(IOException ioe){
			ioe.printStackTrace();
			logger.error(ioe.getMessage());
		} catch(NullPointerException n){
			n.printStackTrace();
			logger.error(n.getMessage());
		} catch(Exception e){
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}
}
