package com.enro.htool.main;

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import COM.TIBCO.hawk.talon.MicroAgentData;
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
    
    public HTool(String hawkTransportParams[]){
    	init(HToolUtil.getProps(hawkTransportParams));
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
    			
    			logger.log(Level.INFO,"Inspecting agent "+console.getAgentName());
    			
    			Thread.sleep(HToolConstants.INTVL);
    			domMaids = ad.get(console.getAgentName());
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
    
    private String processRulebaseTemplate(String domain, String deployment, String component, String template) {
    	
    	return BWUtils.strReplace(
    			BWUtils.strReplace(
    					BWUtils.strReplace(template,HToolConstants.DOMEXP,domain),
    			HToolConstants.DEPEXP,deployment),
    		HToolConstants.CMPEXP,component);

	}
    
    private RulebaseXML getRulebaseXML(String rulebaseData, String rulebaseName) {
    	try {
			Rulebase rb = new Rulebase(new StringReader(rulebaseData));
			
			logger.log(Level.INFO, "Configuring XML for rulebase " + rulebaseName);
			System.out.println("Configuring XML for rulebase " + rulebaseName);
			rb.setName(rulebaseName);
			
			System.out.println("XML data is\n " + rulebaseData);
			logger.log(Level.INFO,"XML data is\n " + rulebaseData);
			
			RulebaseXML rbXml = new RulebaseXML(rb);
			System.out.println("Pocesses XML is\n"+rbXml.getXMLString());
			logger.log(Level.INFO,"Pocesses XML is\n"+rbXml.getXMLString());
			
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
			MicroAgentData mad = console.invoke(maid, mi);
			rslt = 1;
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return rslt;
	}
    
    public int processRulebaseTemplates(String domain,String  [] microAgents,String [] templates,String [] templateNames){
    	
    	int rslt = 0;
    	int len = templates.length;
    	
    	if(len != templateNames.length){
    		logger.log(Level.SEVERE,"Invalid parameters: Rulebase and Rulebase name mismatch");
    		return rslt;
    	}
    	
    	if(domainMicroAgentIDMap == null) getMAgents(microAgents);
    	MicroAgentID maidRBE = domainMicroAgentIDMap.get(HToolConstants.REMANM);
    	
    	for(int i=0;i<len;i++){
    		String template = templates[i];
    		String templateName = templateNames[i];
    		
    		for(String microAgent : microAgents){
    			
    			String sep = domain + ".";
    	    	String [] depComp = microAgent.substring(microAgent.lastIndexOf(sep)+sep.length()).split("\\.");
    	    	String deployment = depComp[0];
    	    	String component = depComp[1];
    	    	String rulebaseName = BWUtils.strReplace(deployment+"_"+component+"_"+templateName,"\\.[hH][rR][bB]$","");
    	    	String rulebaseData = processRulebaseTemplate(domain,deployment,component,template);
    	    	RulebaseXML rbXml = getRulebaseXML(rulebaseData, rulebaseName);
    	    	
    	    	if(rbXml != null){
    	    		rslt += pushRulebase(maidRBE,rbXml);
    	    	}
    	    	
    		}
    	}
    	
    	return rslt;
    	
    } 
    
    public void cleanup(){
    	console.cleanUp();
    }
}
