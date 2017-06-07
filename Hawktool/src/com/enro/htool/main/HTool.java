package com.enro.htool.main;

import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.enro.bwutils.BWUtils;
import com.enro.htool.common.HToolConstants;
import com.enro.htool.common.HToolUtil;

import COM.TIBCO.hawk.config.rbengine.rulebase.Rulebase;
import COM.TIBCO.hawk.config.rbengine.rulebase.RulebaseXML;
import COM.TIBCO.hawk.talon.DataElement;
import COM.TIBCO.hawk.talon.MethodInvocation;
import COM.TIBCO.hawk.talon.MicroAgentID;

public class HTool
    implements Serializable
{
	
	private static final long serialVersionUID = 0xede48a6b44b76df8L;
    private HToolConsole console;
    private Map<String,MicroAgentID> domainMicroAgentIDMap = null;

    public HTool(){
    }
    
    public HTool(String hawkTransportParams[]){
    	try {
			console = new HToolConsole(HToolUtil.getProps(hawkTransportParams));
		} catch (Exception e) {
			e.printStackTrace();
			console = null;
		}
    }  
    
    public String [] getMAgents(String mAgents[]){
    	
    	Map<String,Map<String,MicroAgentID>> ad = console.getAgentDetails();
    	Map<String,MicroAgentID> domMaids = null;
    	
    	while(null == domMaids){
    		try {
				Thread.sleep(HToolConstants.INTVL);
				domMaids = ad.get(console.getAgentName());
				domainMicroAgentIDMap = domMaids;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
    	}
    	
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
    }
    
    private String[] getRequestedMicroAgents(String mAgents[]){
        List<String> microAgents = new ArrayList<String>();

        for(String mAgent : mAgents){
            microAgents.add(mAgent.contains(HToolConstants.BWMANM) ? HToolConstants.BWMANM : mAgent);
        }

        return (String[])microAgents.toArray(new String[microAgents.size()]);
    }
    
    private String ProcessRulebaseTemplate(String domain, String deployment, String component, String template) {
    	
    	return BWUtils.strReplace(
    			BWUtils.strReplace(
    					BWUtils.strReplace(template,HToolConstants.DOMEXP,domain),
    			HToolConstants.DEPEXP,deployment),
    		HToolConstants.CMPEXP,component);

	}
    
    private RulebaseXML getRulebaseXML(String rulebaseData, String rulebaseName) {
    	try {
			Rulebase rb = new Rulebase(new StringReader(rulebaseData));
			rb.setName(rulebaseName);
			
			RulebaseXML rbXml = new RulebaseXML(rb);
			return rbXml;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
    
    private boolean pushRulebase(MicroAgentID maid, RulebaseXML rbXml) {
		
		boolean rslt = false;
		
		try{
			DataElement[] dataElements = new DataElement[1];
			dataElements[0] = new DataElement("RulebaseXML", rbXml);
			MethodInvocation mi = new MethodInvocation("addRuleBase", dataElements);
			console.invoke(maid, mi);
		} catch (Exception e){
			e.printStackTrace();
		}
		
		return rslt;
	}
    
    public boolean ProcessRulebaseTemplates(String domain,String  [] microAgents,String [] templates,String [] templateNames){
    	
    	boolean rslt = false;
    	int len = templates.length;
    	
    	if(len != templateNames.length){
    		System.out.println("Invalid parameters: Rulebase and Rulebase name mismatch");
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
    	    	String rulebaseData = ProcessRulebaseTemplate(domain,deployment,component,template);
    	    	RulebaseXML rbXml = getRulebaseXML(rulebaseData, rulebaseName);
    	    	
    	    	if(rbXml != null){
    	    		pushRulebase(maidRBE,rbXml);
    	    	}
    	    	
    		}
    	}
    	
    	return true;
    	
    } 
    
    public void cleanup(){
    	console.cleanUp();
    }

	public static void main(String a[]) {
        System.out.println("HTool");
    }
}
