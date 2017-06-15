package com.enro.htool.main;

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.enro.bwutils.BWUtils;
import com.enro.htool.common.HToolConstants;
import com.enro.htool.common.HToolUtil;

import COM.TIBCO.hawk.config.rbengine.rulebase.Rulebase;
import COM.TIBCO.hawk.config.rbengine.rulebase.RulebaseXML;
import COM.TIBCO.hawk.talon.DataElement;
import COM.TIBCO.hawk.talon.MethodInvocation;
import COM.TIBCO.hawk.talon.MicroAgentID;
import COM.TIBCO.hawk.utilities.misc.HawkConstants;

public class HTool
    implements Serializable
{
	
	private static final long serialVersionUID = 0xede48a6b44b76df8L;
    private HToolConsole console;
    private Map<String,MicroAgentID> domainMicroAgentIDMap = null;
    
    private final static Logger logger = Logger.getLogger(HTool.class.getName());
    
    private void init(Properties props){
    	try {
    		Hashtable <String,String> sslProps = HToolUtil.processSSLProperties(props);
    		
    		if(sslProps.size() > 0){
    			props.put(HawkConstants.HAWK_SSL_PARAMS, sslProps);
    		}
			console = new HToolConsole(props);
		} catch (Exception e) {
			e.printStackTrace();
			console = null;
		}
    }

    public HTool(Properties props){
    	init(props);
    }
    
    public String [] getMAgents(String mAgents[]){
    	
    	Map<String,Map<String,MicroAgentID>> ad = console.getAgentDetails();
    	Map<String,MicroAgentID> domMaids = null;
    	
    	while(null == ad ){
    		try {
				Thread.sleep(HToolConstants.INTVL);
				ad = console.getAgentDetails();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
    	}
    	
    	int count = 0;
    	while(null == domMaids && count < HToolConstants.NUMAT){
    		try{
    			
    			Thread.sleep(HToolConstants.INTVL);
    			
    			// Query all agents in the domain for their MicroAgents
    			Set<Entry<String,Map<String,MicroAgentID>>> allMASet = ad.entrySet();
    			Iterator <Entry<String,Map<String,MicroAgentID>>> allMASetIt = allMASet.iterator();
    			while(allMASetIt.hasNext()){
    				Entry<String,Map<String,MicroAgentID>> agMA = allMASetIt.next();
    				Map<String,MicroAgentID> ma = agMA.getValue();
    				
    				if(null == domMaids) domMaids = ma;
    				else domMaids.putAll(ma);			// Each MicroAgent should be unique in the domain, so this should work
    			}
    			
    			domainMicroAgentIDMap = domMaids;
    	    	
    	    	Set<String> allMas = domMaids.keySet();
    	    	Iterator <String>it = allMas.iterator();
    	    	List<String> mAgts = new ArrayList<String>();
    	    	String requestedMicroAgents[] = getRequestedMicroAgents(mAgents);
    	    	
    	    	while(it.hasNext()){
    	    		String ma = it.next();
    	    		
    	    		for(String requestedMicroAgent : requestedMicroAgents){
    	                if(!ma.contains(requestedMicroAgent)) continue;
    	                
    	                mAgts.add(ma);
    	                break;
    	            }
    	    	}
    	    	
    	    	return mAgts.toArray(new String[mAgts.size()]);
    	    	
    		} catch(Exception e){
    			count+=1;
    			String msg = console.getAgentName() + " has not responded after " + count + " attempts";
    			logger.log(Level.WARNING,msg);
    		}
    	}
    	
    	return null;
    }
    
    private String[] getRequestedMicroAgents(String mAgents[]){
        List<String> microAgents = new ArrayList<String>();

        for(String mAgent : mAgents){
            microAgents.add(mAgent.contains(HToolConstants.BWMANM) ? HToolConstants.BWMANM : mAgent);
        }

        return (String[])microAgents.toArray(new String[microAgents.size()]);
    }
    
    private String replaceInTemplate(String domain, String deployment, String component, String template) {
    	
    	String processed = BWUtils.strReplace(
    			BWUtils.strReplace(
    					BWUtils.strReplace(template,HToolConstants.DOMEXP,domain),
    			HToolConstants.DEPEXP,deployment),
    		HToolConstants.CMPEXP,component);
    	
    	return processed;

	}
    
    private RulebaseXML getRulebaseXML(String rulebaseData, String rulebaseName) {
    	try {
			Rulebase rb = new Rulebase(new StringReader(rulebaseData));
			
			// Rulebase name can't contain spaces
			String nm = BWUtils.strReplace(rulebaseName,"\\s", "_");
			
			logger.log(Level.FINE, "Configuring XML for rulebase " + nm);
			rb.setName(nm);
			rb.setLastModification(this.getClass().getSimpleName());
			
			logger.log(Level.FINE,"XML data is\n " + rulebaseData);
			
			RulebaseXML rbXml = new RulebaseXML(rb);
			logger.log(Level.FINE,"Pocesses XML is\n"+rbXml.getXMLString());
			
			return rbXml;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
    
    private int pushRulebase(MicroAgentID maid, RulebaseXML rbXml) {
		
		int rslt = 0;
		
		try{
			DataElement[] dataElements = new DataElement[1];
			dataElements[0] = new DataElement("RulebaseXML", rbXml);
			MethodInvocation mi = new MethodInvocation("addRuleBase", dataElements);
			console.invoke(maid, mi);
			rslt = 1;
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return rslt;
	}
    
    private int processRulebaseTemplate(String domain,String microAgent,String template,String templateName,String service){
    	
    	int rslt = 0;
    	
    	// Find the RuleBaseEngine Micro Agent for the agent which contains the requested MicroAgent
    	MicroAgentID [] maidRBEs = console.getRBEMicroAgentsFor(microAgent,service,templateName);
    	
    	if(maidRBEs == null){
    		logger.log(Level.WARNING, "Unable to find RuleBase Engine MicroAgent for " + microAgent + ". Skipping...");
    		return rslt;
    	}
    	
    	String [] depComp = null;
    	
    	if(microAgent.contains(HToolConstants.BWMANM)){
    		String sep = domain + ".";
	    	depComp = microAgent.substring(microAgent.lastIndexOf(sep)+sep.length()).split("\\.");
    	}
    	else{
    		String sep = "hawk.";
	    	depComp = microAgent.substring(microAgent.lastIndexOf(sep)+sep.length()).split("\\.");
    	}
		
		String deployment = depComp[0];
    	String component = depComp[1];
    	String rulebaseName = BWUtils.strReplace(deployment+"_"+component+"_"+templateName,"\\.[hH][rR][bB]$","");
    	String rulebaseData = replaceInTemplate(domain,deployment,component,template);
    	RulebaseXML rbXml = getRulebaseXML(rulebaseData, rulebaseName);
    		
    	if(rbXml != null){
    		for(MicroAgentID maidRBE : maidRBEs){
    			logger.log(Level.FINE,"Pushing "+rulebaseName+" to "+maidRBE.getAgent().getName());
    			rslt += pushRulebase(maidRBE,rbXml);
    		}
    	}
    	
    	return rslt;
    }
    
    public int processRulebaseTemplates(String domain,String  [] microAgents,String [] templates,String [] templateNames, String [] services){
    	int rslt = 0;
    	int len = templates.length;
    	
    	if(len != templateNames.length){
    		logger.log(Level.SEVERE,"Invalid parameters: Rulebase and Rulebase name mismatch");
    		return rslt;
    	}
    	
    	if(domainMicroAgentIDMap == null) getMAgents(microAgents);
    	
    	for(int i=0;i<len;i++){
    		String template = templates[i];
    		String templateName = templateNames[i];
    		
    		for(String microAgent : microAgents){
    			
    			if(services == null){
    				rslt = processRulebaseTemplate(domain, microAgent, template, templateName, null);
    			} else{
    				for(String service : services){
    					rslt += processRulebaseTemplate(domain, microAgent, template, templateName, service);
    				}
    			}
    	    	
    		}
    	}
    	
    	return rslt;
    }
    
    public int processRulebaseTemplates(String domain,String  [] microAgents,String [] templates,String [] templateNames){
    	return processRulebaseTemplates(domain,microAgents,templates,templateNames,null);
    } 
    
    public void cleanup(){
    	console.cleanUp();
    }

	public void showDomainDetails() {
		console.showDomainDetails();
	}
}
