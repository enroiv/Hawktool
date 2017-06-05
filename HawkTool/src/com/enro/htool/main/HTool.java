package com.enro.htool.main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import COM.TIBCO.hawk.console.hawkeye.AgentManager;
import COM.TIBCO.hawk.console.hawkeye.TIBHawkConsole;
import COM.TIBCO.hawk.console.hawkeye.TIBHawkConsoleFactory;
import COM.TIBCO.hawk.talon.DataElement;
import COM.TIBCO.hawk.talon.MethodInvocation;
import COM.TIBCO.hawk.talon.MicroAgentData;
import COM.TIBCO.hawk.talon.MicroAgentID;
import COM.TIBCO.hawk.talon.TabularData;

public class HTool implements Serializable {
	
	private static final long serialVersionUID = -1304765798711595528L;
	private final String SELFMA="COM.TIBCO.hawk.microagent.Self";
	private final String MAINFO="getMicroAgentInfo";
	private final String BWMANM="COM.TIBCO.ADAPTER.bwengine";
	
	public HTool(){}
	
	private Properties getProps(String [] params){
		Properties p = new Properties();
		
		for(String entry : params){
			String [] keyval = entry.split("=");
			p.setProperty(keyval[0],keyval[1]);
		}
		
		return p;
	}
	
	private String [] getDomainMicroAgents(AgentManager mgr, String [] requestedMicroAgents) throws Exception{
		
		MicroAgentID [] ids = mgr.getMicroAgentIDs(SELFMA);
		if(ids.length == 0) throw new Exception("FATAL: Self Micro-Agent could not be found");
		
		MethodInvocation gmaiMeth = new MethodInvocation(MAINFO,null);
		MicroAgentData gmaiData = mgr.invoke(ids[0], gmaiMeth);
		TabularData gmaiTabData = (TabularData) gmaiData.getData();
		
		DataElement [][] microAgentsData = gmaiTabData.getAllDataElements();
		if(microAgentsData == null) throw new Exception("FATAL: Self Micro-Agent could not find Rulebase info");
		
		Map<String,String> dmAgents = new HashMap<String,String>();
		
		for(int i=0;i<microAgentsData.length;i++){
			for(int j=0;j<microAgentsData[i].length;j++){
				
				if(microAgentsData[i][j].getName().equalsIgnoreCase("Name")){
					
					String val = microAgentsData[i][j].getValue().toString();
					
					for(String nm : requestedMicroAgents){
						// If we found a microAgent in the domain which corresponds to a requested microAgent, add it.
						if(val.contains(nm)){
							dmAgents.put(val, val);
							break;
						}
					}
				}
				
			}
		}
		
		List<String> bwAgents = dmAgents.values().stream().collect(Collectors.toList());
		return bwAgents.toArray(new String[bwAgents.size()]);
	}
	
	private String[] getRequestedMicroAgents(String[] mAgents) {
		List<String> microAgents = new ArrayList<String>();
		
		for(String mAgent : mAgents){
			microAgents.add(((mAgent.contains(BWMANM)) ? BWMANM : mAgent));
		}
		
		return microAgents.toArray(new String[microAgents.size()]);
	}
	
	public String process(String [] mAgents, String [] hawkTransportParams){
		
		String ret = "";
		Properties hawkProps = getProps(hawkTransportParams);
		String [] requestedMicroAgents = getRequestedMicroAgents(mAgents);
		
		try {
			TIBHawkConsole console = TIBHawkConsoleFactory.getInstance().createHawkConsole(hawkProps);
			AgentManager manager = console.getAgentManager();
			manager.initialize();
			
			String domainMicroAgentsArr [] = getDomainMicroAgents(manager,requestedMicroAgents);
			
			StringBuilder sb = new StringBuilder();
			for(String domainMicroAgent : domainMicroAgentsArr){
				sb.append(domainMicroAgent);
				sb.append(",");
			}
			
			manager.shutdown();
			ret = sb.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
			ret = "FATAL: " + e.toString();
		}
		
		return ret;
	}

	public static void main(String [] a){
		System.out.println("HTool");
	}
}
